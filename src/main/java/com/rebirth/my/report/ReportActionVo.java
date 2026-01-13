package com.rebirth.my.report;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReportActionVo {
    private Long id;
    private Long reportId;
    private Long adminId;
    private Long targetUserId;
    private String actionType; // WARNING, SUSPENSION_3DAY, BAN_PERMANENT
    private String actionReason;
    private LocalDateTime createdAt;

    // Join Fields
    private String adminName;
    private String targetUserName;
}
