package com.rebirth.my.domain;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class EcoTodoTask {
    private Long id;
    private String code;
    private String title;
    private String description;
    private Integer defaultPoints;
    private String isActive;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    
    // For UI display state
    private boolean checked;
}
