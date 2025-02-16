package com.chatstream.core.messageCRUDService.repository;

import com.chatstream.core.messageCRUDService.models.GroupChat;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface GroupChatRepository extends ReactiveMongoRepository<GroupChat, String> {
    Flux<GroupChat> findByMembersContaining(String memberId);
}
