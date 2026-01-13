package com.rebirth.my.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatMessage {
    private MessageType type;
    private String sender;
    private String senderId; // For identifying the user
    private String content; // Message body
    private Long roomId; // Target Room ID
    private String timestamp; // ISO 8601 formatted time
    private String imageUrl; // Image URL for image messages

    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE,
        READ,
        IMAGE
    }
}
