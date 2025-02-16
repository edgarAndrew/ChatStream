package com.chatstream.core.messageCRUDService.repository;

import com.chatstream.core.messageCRUDService.models.User;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveMongoRepository<User, String> {
    Mono<User> findByUsername(String username);
    Mono<User> findByEmail(String email);
}
