package com.chatstream.core.messageCRUDService.repository;

import com.chatstream.core.messageCRUDService.models.Message;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface MessageRepository extends ReactiveMongoRepository<Message, String> {
    Flux<Message> findByConversationIdOrderByTimestampDesc(String conversationId);
    Flux<Message> findByConversationIdAndReadStatusFalse(String conversationId);
}