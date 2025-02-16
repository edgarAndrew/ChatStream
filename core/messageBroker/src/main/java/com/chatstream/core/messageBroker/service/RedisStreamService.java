package com.chatstream.core.messageBroker.service;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStreamOperations;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class RedisStreamService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ReactiveStreamOperations<String, String, String> streamOps;

    public RedisStreamService(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.streamOps = redisTemplate.opsForStream();
    }


    public Mono<String> publishMessage(String stream, String message) {
        return streamOps.add(ObjectRecord.create(stream, message))
                .doOnNext(recordId -> System.out.println("Message published: " + message + " with ID: " + recordId))
                .map(recordId -> "Message published to " + stream + " with ID: " + recordId);
    }

    public Flux<ObjectRecord<String, String>> consumeMessages(String stream) {
        return streamOps.read(StreamOffset.fromStart(stream))
                .map(mapRecord -> {
                    String value = mapRecord.getValue().values().iterator().next();
                    System.out.println("Received from " + stream + ": " + value);
                    return ObjectRecord.create(stream, value);
                });
    }

    public Flux<Map<String, String>> fetchNMessages(String stream, long count) {
        return streamOps.range(stream, org.springframework.data.domain.Range.unbounded())
                .take(count) // Limit the number of messages to N
                .map(MapRecord::getValue); // Extract the message body (key-value pairs)
    }

    public Flux<Map<String, String>> fetchNMessagesFromEnd(String stream, long count) {
        return streamOps.reverseRange(stream, org.springframework.data.domain.Range.unbounded())
                .take(count) // Limit the number of messages to N
                .map(MapRecord::getValue); // Extract the message body (key-value pairs)
    }

    public Mono<Long> getStreamLength(String stream) {
        return streamOps.size(stream);
    }

//    public Mono<Long> clearStream(String streamName) {
//        return redisTemplate.opsForStream()
//                .trim(streamName, Long.MAX_VALUE) // Use MINID to remove all messages
//                .doOnSuccess(deleted -> System.out.println("Cleared stream: " + streamName + ", deleted messages: " + deleted));
//    }

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

}