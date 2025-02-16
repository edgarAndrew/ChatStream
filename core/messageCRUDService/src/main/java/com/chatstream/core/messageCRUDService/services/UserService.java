package com.chatstream.core.messageCRUDService.services;



import com.chatstream.core.messageCRUDService.models.User;
import com.chatstream.core.messageCRUDService.repository.UserRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<User> createUser(User user) {
        user.setCreatedAt(LocalDateTime.now());
        user.setLastSeen(LocalDateTime.now());
        return userRepository.save(user);
    }

    public Mono<User> getUserById(String userId) {
        return userRepository.findById(userId);
    }

    public Mono<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Mono<User> updateUserStatus(String userId, String status) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    user.setStatus(status);
                    user.setLastSeen(LocalDateTime.now());
                    return userRepository.save(user);
                });
    }

    public Mono<User> addContact(String userId, String contactId) {
        return userRepository.findById(userId)
                .flatMap(user -> {
                    user.getContacts().add(contactId);
                    return userRepository.save(user);
                });
    }

    public Flux<User> getUserContacts(String userId) {
        return userRepository.findById(userId)
                .flatMapMany(user -> userRepository.findAllById(user.getContacts()));
    }
}