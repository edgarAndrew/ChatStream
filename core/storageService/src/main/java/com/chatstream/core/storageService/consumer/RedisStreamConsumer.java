package com.chatstream.core.storageService.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class RedisStreamConsumer implements CommandLineRunner {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Autowired
    public RedisStreamConsumer(ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        String streamKey = "your-stream-key"; // Replace with your actual Redis stream key

        // Continuously read from the Redis stream
        Flux<MapRecord<String, Object, Object>> streamMessages = reactiveRedisTemplate.opsForStream()
                .read(StreamReadOptions.empty().count(10), // Optional: Adjust the batch size
                        StreamOffset.latest(streamKey) // Correct way to specify stream offset
                );

        streamMessages.subscribe(message -> {
            // Process each message
            System.out.println("Received message: " + message.getValue());

            // Here you can add logic to batch insert into MongoDB
            // For now, we just print the message to the console
        });

        // Keep the application running to continue consuming messages
        Thread.currentThread().join();
    }
}