package com.chatstream.core.messageCRUDService.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "group_chats")
public class GroupChat {
    @Id
    private String id;
    private String name;
    private List<String> admins;
    private List<String> members;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
