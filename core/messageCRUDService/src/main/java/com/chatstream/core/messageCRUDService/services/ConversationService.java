package com.chatstream.core.messageCRUDService.services;

import com.chatstream.core.messageCRUDService.models.Conversation;
import com.chatstream.core.messageCRUDService.repository.ConversationRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final ChatService chatService;

    public ConversationService(ConversationRepository conversationRepository,
                               ChatService chatService) {
        this.conversationRepository = conversationRepository;
        this.chatService = chatService;
    }

    public Mono<Conversation> createConversation(String roomId, String type) {
        Conversation conversation = Conversation.builder()
                .roomId(roomId)
                .type(type)
                .updatedAt(LocalDateTime.now())
                .build();
        return conversationRepository.save(conversation);
    }

    public Mono<Conversation> getConversation(String conversationId, String userId) {
        return conversationRepository.findById(conversationId)
                .flatMap(conversation ->
                        chatService.canAccessRoom(userId, conversation.getRoomId())
                                .filter(Boolean::booleanValue)
                                .map(canAccess -> conversation)
                );
    }

    public Flux<Conversation> getUserConversations(String userId) {
        return conversationRepository.findAll()
                .filterWhen(conversation ->
                        chatService.canAccessRoom(userId, conversation.getRoomId())
                );
    }
}
