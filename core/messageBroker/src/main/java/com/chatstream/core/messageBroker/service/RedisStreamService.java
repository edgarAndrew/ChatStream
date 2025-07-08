package com.chatstream.core.messageBroker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStreamOperations;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RedisStreamService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ReactiveStreamOperations<String, String, String> streamOps;
    private final MongoMessageService mongoMessageService;
    private final Map<String, Long> activeStreams = new ConcurrentHashMap<>();

    // @Value("${stream_batch_size:100}")
    private final int streamBatchSize = 10;

    @Value("${stream_flush_interval_ms:60000}")
    private int flushIntervalMs; // Default 1 minute

    public RedisStreamService(ReactiveRedisTemplate<String, String> redisTemplate,
                              MongoMessageService mongoMessageService) {
        this.redisTemplate = redisTemplate;
        this.streamOps = redisTemplate.opsForStream();
        this.mongoMessageService = mongoMessageService;
    }

    public Mono<String> publishMessage(String stream, String message) {
        // Track active streams for scheduled flushing
        activeStreams.putIfAbsent(stream, System.currentTimeMillis());

        return streamOps.add(ObjectRecord.create(stream, message))
                .doOnNext(recordId -> System.out.println("Message published: " + message + " with ID: " + recordId))
                .map(recordId -> "Message published to " + stream + " with ID: " + recordId);
    }

    public Flux<ObjectRecord<String, String>> consumeMessages(String stream) {
        // Track active streams for scheduled flushing
        activeStreams.putIfAbsent(stream, System.currentTimeMillis());

        return streamOps.read(StreamOffset.fromStart(stream))
                .map(mapRecord -> {
                    String value = mapRecord.getValue().values().iterator().next();
                    System.out.println("Received from " + stream + ": " + value);
                    return ObjectRecord.create(stream, value);
                });
    }

    public Flux<Map<String, String>> fetchNMessages(String stream, long count) {
        return streamOps.range(stream, Range.unbounded())
                .take(count) // Limit the number of messages to N
                .map(MapRecord::getValue); // Extract the message body (key-value pairs)
    }

    public Flux<Map<String, String>> fetchNMessagesFromEnd(String stream, long count) {
        return streamOps.reverseRange(stream, Range.unbounded())
                .take(count) // Limit the number of messages to N
                .map(MapRecord::getValue); // Extract the message body (key-value pairs)
    }

    public Mono<Long> getStreamLength(String stream) {
        return streamOps.size(stream);
    }

    public Mono<Long> clearStream(String streamName) {
        return streamOps.size(streamName)
                .flatMap(size -> {
                    if (size == 0) {
                        return Mono.just(0L);
                    }
                    // Get all message IDs first
                    return streamOps.range(streamName, Range.unbounded())
                            .map(message -> message.getId().getValue())
                            .collectList()
                            .flatMap(ids -> {
                                if (ids.isEmpty()) {
                                    return Mono.just(0L);
                                }
                                // Delete all messages by their IDs
                                return streamOps.delete(streamName, ids.toArray(new String[0]))
                                        .map(deleted -> (long) ids.size())
                                        .doOnSuccess(deleted ->
                                                System.out.println("Cleared stream: " + streamName + ", deleted messages: " + deleted));
                            });
                });
    }

    // Method to process stream batch and save to MongoDB

    public Mono<Void> processBatch(String stream) {
        return getStreamLength(stream)
                .flatMap(length -> {
                    if (length == 0) {
                        return Mono.empty().then(); // Convert to Mono<Void>
                    }

                    System.out.println("Processing batch for stream: " + stream + " with length: " + length);
                    return fetchNMessages(stream, length)
                            .collectList()
                            .flatMap(messages -> {
                                if (messages.isEmpty()) {
                                    return Mono.empty().then(); // Convert to Mono<Void>
                                }

                                // Save messages to MongoDB
                                return mongoMessageService.saveStreamMessages(stream, messages)
                                        .then(clearStream(stream))
                                        .doOnSuccess(cleared -> {
                                            System.out.println("Batch processed and cleared for stream: " + stream);
                                            // Update last processed time
                                            activeStreams.put(stream, System.currentTimeMillis());
                                        })
                                        .then(); // Ensure we return Mono<Void>
                            });
                });
    }

    // Check streams on schedule and flush if they meet criteria
    @Scheduled(fixedDelayString = "${stream_check_interval_ms:10000}")
    public void checkAndFlushStreams() {
        long currentTime = System.currentTimeMillis();

        activeStreams.forEach((stream, lastProcessed) -> {
            // Check if elapsed time exceeds flush interval
            if (currentTime - lastProcessed > flushIntervalMs) {
                System.out.println("Time-based flush triggered for stream: " + stream);
                processBatch(stream).subscribe();
            } else {
                // Check if size exceeds batch size
                getStreamLength(stream)
                        .filter(size -> size >= streamBatchSize)
                        .flatMap(size -> {
                            System.out.println("Size-based flush triggered for stream: " + stream);
                            return processBatch(stream);
                        })
                        .subscribe();
            }
        });
    }
}