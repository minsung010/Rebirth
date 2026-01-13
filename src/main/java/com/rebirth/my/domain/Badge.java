package com.rebirth.my.domain;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Badge {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String iconUrl;
    private String conditionType;
    private Double thresholdValue;
    private LocalDateTime createdAt;
    
    // UI State
    private boolean acquired;
}
