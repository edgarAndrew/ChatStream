package com.chatstream.core.messageCRUDService.services;

import com.chatstream.core.messageCRUDService.models.GroupChat;
import com.chatstream.core.messageCRUDService.models.SingleChat;
import com.chatstream.core.messageCRUDService.repository.GroupChatRepository;
import com.chatstream.core.messageCRUDService.repository.SingleChatRepository;
import com.chatstream.core.messageCRUDService.repository.UserRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {
    private final SingleChatRepository singleChatRepository;
    private final GroupChatRepository groupChatRepository;
    private final UserRepository userRepository;

    public ChatService(SingleChatRepository singleChatRepository,
                       GroupChatRepository groupChatRepository,
                       UserRepository userRepository) {
        this.singleChatRepository = singleChatRepository;
        this.groupChatRepository = groupChatRepository;
        this.userRepository = userRepository;
    }

    // Single Chat operations
    public Mono<SingleChat> createSingleChat(String user1Id, String user2Id) {
        return userRepository.findById(user1Id)
                .zipWith(userRepository.findById(user2Id))
                .flatMap(tuple -> {
                    SingleChat chat = SingleChat.builder()
                            .member1(user1Id)
                            .member2(user2Id)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return singleChatRepository.save(chat);
                });
    }

    // Group Chat operations with member/admin validation
    public Mono<GroupChat> createGroupChat(String name, String creatorId, List<String> memberIds) {
        return userRepository.findById(creatorId)
                .flatMap(creator -> {
                    GroupChat chat = GroupChat.builder()
                            .name(name)
                            .admins(List.of(creatorId))
                            .members(memberIds)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    return groupChatRepository.save(chat);
                });
    }

    public Mono<GroupChat> addGroupMember(String groupId, String adminId, String newMemberId) {
        return groupChatRepository.findById(groupId)
                .filter(group -> group.getAdmins().contains(adminId))
                .flatMap(group -> {
                    group.getMembers().add(newMemberId);
                    return groupChatRepository.save(group);
                });
    }

    public Mono<GroupChat> addGroupAdmin(String groupId, String currentAdminId, String newAdminId) {
        return groupChatRepository.findById(groupId)
                .filter(group -> group.getAdmins().contains(currentAdminId))
                .flatMap(group -> {
                    group.getAdmins().add(newAdminId);
                    return groupChatRepository.save(group);
                });
    }

    // Room access validation methods
    public Mono<Boolean> canAccessRoom(String userId, String roomId) {
        return Mono.zip(
                singleChatRepository.findById(roomId).map(chat ->
                        chat.getMember1().equals(userId) || chat.getMember2().equals(userId)
                ).defaultIfEmpty(false),
                groupChatRepository.findById(roomId).map(chat ->
                        chat.getMembers().contains(userId)
                ).defaultIfEmpty(false)
        ).map(tuple -> tuple.getT1() || tuple.getT2());
    }

    public Mono<Boolean> isGroupAdmin(String userId, String groupId) {
        return groupChatRepository.findById(groupId)
                .map(group -> group.getAdmins().contains(userId))
                .defaultIfEmpty(false);
    }
}
