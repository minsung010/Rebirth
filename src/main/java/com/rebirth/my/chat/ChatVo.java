package com.rebirth.my.chat;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatVo {
    // Primary Key
    private Long id;

    // Room & Sender
    private Long roomId;
    private Long senderId;

    // Content
    private String messageType; // TEXT, IMAGE, OOTD
    private String content;
    private String imageUrl;
    private Long outfitId;

    // Meta
    private LocalDateTime createdAt;

    // Legacy support (optional, can be removed if not used elsewhere)
    private String chatId;
    private String userId;
    private String message;
    private LocalDateTime sentAt;
}
