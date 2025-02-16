package com.chatstream.core.messageBroker.controller;

import com.chatstream.core.messageBroker.service.RedisStreamService;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

// This is a rest endpoint for testing purpose

// Uncomment if want to test

@RestController
@RequestMapping("/api/stream")
public class RedisStreamController {

    private final RedisStreamService redisStreamService;

    public RedisStreamController(RedisStreamService redisStreamService) {
        this.redisStreamService = redisStreamService;
    }

    // Endpoint to publish a message to a specific Redis Stream
    @PostMapping("/publish")
    public Mono<String> publishMessage(@RequestParam String stream, @RequestParam String message) {
        return redisStreamService.publishMessage(stream, message);
    }

    // SSE Endpoint to consume messages from a given Redis Stream
    @GetMapping(value = "/consume", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ObjectRecord<String, String>> consumeMessages(@RequestParam String stream) {
        return redisStreamService.consumeMessages(stream);
    }

    // Endpoint to publish a message to a specific Redis Stream
    @PutMapping("/clear")
    public Mono<Long> clearStream(@RequestParam String stream) {
        return redisStreamService.clearStream(stream);
    }

    @GetMapping("/length")
    public Mono<Long> getStreamLength(@RequestParam String stream) {
        return redisStreamService.getStreamLength(stream);
    }

    @GetMapping("/messages")
    public Flux<Map<String, String>> fetchMessages(
            @RequestParam String stream,
            @RequestParam(defaultValue = "10") long count) {
        return redisStreamService.fetchNMessages(stream, count);
    }

    @GetMapping("/messages/recent")
    public Flux<Map<String, String>> fetchRecentMessages(
            @RequestParam String stream,
            @RequestParam(defaultValue = "10") long count) {
        return redisStreamService.fetchNMessagesFromEnd(stream, count);
    }
}