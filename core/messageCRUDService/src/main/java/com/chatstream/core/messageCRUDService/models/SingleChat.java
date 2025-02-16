package com.chatstream.core.messageCRUDService.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "single_chats")
public class SingleChat {
    @Id
    private String id;
    private String member1;
    private String member2;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
