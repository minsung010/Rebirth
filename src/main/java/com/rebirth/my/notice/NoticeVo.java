package com.rebirth.my.notice;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NoticeVo {
    private int id; // Renamed from noticeId
    private String title;
    private String content;
    private String writer;
    private LocalDateTime createdAt;
    private int viewCount; // Renamed from hit

    // New fields
    private String category; // NOTICE, QNA
    private String isSecret; // Y, N
    private String userId; // Writer's User ID

    // Admin Answer Support
    private String answer;
    private LocalDateTime answeredAt;
}
