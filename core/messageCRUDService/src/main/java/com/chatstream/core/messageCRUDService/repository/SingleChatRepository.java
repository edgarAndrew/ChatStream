package com.chatstream.core.messageCRUDService.repository;

import com.chatstream.core.messageCRUDService.models.SingleChat;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface SingleChatRepository extends ReactiveMongoRepository<SingleChat, String> {
    Flux<SingleChat> findByMember1OrMember2(String member1, String member2);
}
