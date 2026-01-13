package com.rebirth.my.report;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReportVo {
    private Long id;
    private Long reporterId;
    private Long reportedUserId;
    private String reasonCategory; // SPAM, ABUSIVE, INAPPROPRIATE_CONTENT, ETC
    private String isProcessed; // Y/N
    private String description;
    private LocalDateTime createdAt;

    // Join Fields
    private String reporterName;
    private String reportedUserName;
}
