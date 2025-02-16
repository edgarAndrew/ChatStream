package com.chatstream.core.messageCRUDService.services;

import com.chatstream.core.messageCRUDService.models.Message;
import com.chatstream.core.messageCRUDService.repository.MessageRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class MessageService {
    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final ChatService chatService;

    public MessageService(MessageRepository messageRepository,
                          ConversationService conversationService,
                          ChatService chatService) {
        this.messageRepository = messageRepository;
        this.conversationService = conversationService;
        this.chatService = chatService;
    }

    public Mono<Message> sendMessage(String conversationId, String senderId, String content) {
        return conversationService.getConversation(conversationId, senderId)
                .flatMap(conversation -> {
                    Message message = Message.builder()
                            .conversationId(conversationId)
                            .senderId(senderId)
                            .content(content)
                            .timestamp(LocalDateTime.now())
                            .readStatus(false)
                            .build();
                    return messageRepository.save(message);
                });
    }

    public Flux<Message> getConversationMessages(String conversationId, String userId) {
        return conversationService.getConversation(conversationId, userId)
                .flatMapMany(conversation ->
                        messageRepository.findByConversationIdOrderByTimestampDesc(conversationId)
                );
    }

    public Mono<Void> markMessagesAsRead(String conversationId, String userId) {
        return conversationService.getConversation(conversationId, userId)
                .flatMapMany(conversation ->
                        messageRepository.findByConversationIdAndReadStatusFalse(conversationId)
                )
                .flatMap(message -> {
                    message.setReadStatus(true);
                    return messageRepository.save(message);
                })
                .then();
    }
}