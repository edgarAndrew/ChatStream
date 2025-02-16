package com.chatstream.core.messageBroker.handler;

import com.chatstream.core.messageBroker.service.RedisStreamService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RedisStreamWebSocketHandler implements WebSocketHandler {
    private final RedisStreamService redisStreamService;
    private final Map<String, Sinks.Many<String>> streamSinks = new ConcurrentHashMap<>();

    @Value("${stream_batch_size}")
    private int streamBatchSize;

    public RedisStreamWebSocketHandler(RedisStreamService redisStreamService) {
        this.redisStreamService = redisStreamService;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Sinks.Many<String> sessionSink = Sinks.many().unicast().onBackpressureBuffer();
        Map<String, Disposable> subscriptions = new ConcurrentHashMap<>();

        Mono<Void> input = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .flatMap(payload -> handleCommand(session, payload, sessionSink, subscriptions))
                .doFinally(signal -> cleanupSubscriptions(subscriptions))
                .then();

        Mono<Void> output = session.send(sessionSink.asFlux().map(session::textMessage));

        return Mono.zip(input, output).then();
    }

    private Mono<Void> handleCommand(WebSocketSession session, String payload,
                                     Sinks.Many<String> sessionSink,
                                     Map<String, Disposable> subscriptions) {
        String[] parts = payload.split(":", 3);
        if (parts.length < 2) {
            return sendError(sessionSink, "Invalid message format");
        }

        String command = parts[0];
        String stream = parts[1];

        switch (command) {
            case "SUBSCRIBE":
                return handleSubscribe(stream, sessionSink, subscriptions);
            case "PUBLISH":
                if (parts.length < 3) return sendError(sessionSink, "Missing message content");
                return handlePublish(stream, parts[2], sessionSink);
            default:
                return sendError(sessionSink, "Unknown command: " + command);
        }
    }

    private Mono<Void> handleSubscribe(String stream, Sinks.Many<String> sessionSink,
                                       Map<String, Disposable> subscriptions) {
        if (subscriptions.containsKey(stream)) {
            sendSuccess(sessionSink, "Already subscribed to " + stream);
            return Mono.empty();
        }

        Sinks.Many<String> streamSink = streamSinks.computeIfAbsent(stream,
                key -> Sinks.many().multicast().onBackpressureBuffer());

        Disposable subscription = streamSink.asFlux()
                .doOnNext(msg -> sessionSink.tryEmitNext("MESSAGE:" + stream + ":" + msg))
                .subscribe();

        subscriptions.put(stream, subscription);

        // Start consuming from Redis if not already started
        redisStreamService.consumeMessages(stream)
                .doOnNext(record -> {
                    Sinks.Many<String> sink = streamSinks.get(stream);
                    if (sink != null) sink.tryEmitNext(record.getValue());
                })
                .subscribe();

        sendSuccess(sessionSink, "Subscribed to " + stream);
        return Mono.empty();
    }

//    private Mono<Void> handlePublish(String stream, String message, Sinks.Many<String> sessionSink) {
//        return redisStreamService.publishMessage(stream, message)
//                .doOnSuccess(id -> {
//                    Sinks.Many<String> streamSink = streamSinks.get(stream);
//                    if (streamSink != null) streamSink.tryEmitNext(message);
//                    sendSuccess(sessionSink, "PUBLISHED:" + stream + ":" + id);
//                })
//                .doOnError(err -> sendError(sessionSink, "Publish failed: " + err.getMessage()))
//                .then();
//    }

    private Mono<Void> handlePublish(String stream, String message, Sinks.Many<String> sessionSink) {
        return redisStreamService.publishMessage(stream, message)
                .flatMap(id -> redisStreamService.getStreamLength(stream)
                        .flatMap(length -> {
                            System.out.println("Stream length before check: " + length);
                            if (length > streamBatchSize) {
                                return redisStreamService.fetchNMessages(stream, length)
                                        .collectList()
                                        .flatMap(messages -> {
                                            // First save messages to database
                                            return saveMessagesToDatabase(stream, messages)
                                                    .then(redisStreamService.clearStream(stream))
                                                    .flatMap(deleted -> {
                                                        System.out.println("Cleared stream: " + stream + ", deleted messages: " + deleted);
                                                        // Verify the clear operation worked
                                                        return redisStreamService.getStreamLength(stream)
                                                                .flatMap(newLength -> {
                                                                    System.out.println("Stream length after clear: " + newLength);
                                                                    if (newLength >= length) {
                                                                        System.out.println("Failed to clear stream: length did not decrease");
                                                                    }
                                                                    return Mono.just(id);
                                                                });
                                                    });
                                        })
                                        .onErrorResume(error -> {
                                            System.out.println("Error processing stream batch: " + error.getMessage());
                                            error.printStackTrace();
                                            return Mono.error(error);
                                        });
                            }
                            return Mono.just(id);
                        }))
                .doOnSuccess(id -> {
                    Sinks.Many<String> streamSink = streamSinks.get(stream);
                    if (streamSink != null) {
                        streamSink.tryEmitNext(message);
                    }
                    sendSuccess(sessionSink, "PUBLISHED:" + stream + ":" + id);
                })
                .doOnError(err -> {
                    System.out.println("Publish failed: " + err.getMessage());
                    err.printStackTrace();
                    sendError(sessionSink, "Publish failed: " + err.getMessage());
                })
                .then();
    }

    // Placeholder method for database operations
    private Mono<Void> saveMessagesToDatabase(String stream, List<Map<String, String>> messages) {
        System.out.println("Saving " + messages.size() + " messages from stream " + stream + " to database");
        // TODO: Implement actual database save logic
        return Mono.empty();
    }


    private void sendSuccess(Sinks.Many<String> sink, String message) {
        sink.tryEmitNext("SUCCESS:" + message);
    }

    private Mono<Void> sendError(Sinks.Many<String> sink, String message) {
        sink.tryEmitNext("ERROR:" + message);
        return Mono.empty();
    }

    private void cleanupSubscriptions(Map<String, Disposable> subscriptions) {
        subscriptions.values().forEach(Disposable::dispose);
        subscriptions.clear();
    }
}