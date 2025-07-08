package com.chatstream.core.messageBroker.service;

import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class MongoMessageService {

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoMessageService(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public Mono<Void> saveStreamMessages(String streamName, List<Map<String, String>> messages) {
        List<StreamMessage> messageDocs = new ArrayList<>();

        for (Map<String, String> message : messages) {
            // Transform the Redis message to MongoDB document
            StreamMessage doc = new StreamMessage();
            doc.setStreamName(streamName);
            doc.setMessage(message.values().iterator().next()); // Assuming single value in map
            doc.setTimestamp(Instant.now());
            messageDocs.add(doc);
        }

        return mongoTemplate.insertAll(messageDocs)
                .then()
                .doOnSuccess(v -> System.out.println("Saved " + messageDocs.size() + " messages from stream " + streamName + " to MongoDB"));
    }

    // Document class for MongoDB
    public static class StreamMessage {
        private String id;
        private String streamName;
        private String message;
        private Instant timestamp;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getStreamName() {
            return streamName;
        }

        public void setStreamName(String streamName) {
            this.streamName = streamName;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Instant timestamp) {
            this.timestamp = timestamp;
        }
    }
}