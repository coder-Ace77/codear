// src/main/java/com/codear/user/entity/ChatMessage.java
package com.codear.user.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String problemId; 

    @Column(columnDefinition = "TEXT")
    private String content;

    private String role; 

    private LocalDateTime timestamp;
}