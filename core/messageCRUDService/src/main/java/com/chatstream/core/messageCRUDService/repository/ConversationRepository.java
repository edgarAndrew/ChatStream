package com.chatstream.core.messageCRUDService.repository;

import com.chatstream.core.messageCRUDService.models.Conversation;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ConversationRepository extends ReactiveMongoRepository<Conversation, String> {
    Flux<Conversation> findByRoomId(String roomId);
    Mono<Conversation> findByRoomIdAndType(String roomId, String type);
}
