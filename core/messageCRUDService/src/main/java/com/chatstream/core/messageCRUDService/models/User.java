package com.chatstream.core.messageCRUDService.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String username;
    private String email;
    private String password;
    private List<String> contacts;
    private String status;
    private LocalDateTime lastSeen;
    private LocalDateTime createdAt;
}