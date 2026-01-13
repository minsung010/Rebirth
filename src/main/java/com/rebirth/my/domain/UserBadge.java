package com.rebirth.my.domain;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserBadge {
    private Long id;
    private Long userId;
    private Long badgeId;
    private LocalDateTime awardedAt;
    
    // For joining with Badge
    private Badge badge;
}
