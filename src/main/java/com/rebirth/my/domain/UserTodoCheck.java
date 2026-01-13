package com.rebirth.my.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserTodoCheck {
    private Long id;
    private Long userId;
    private Long taskId;
    private LocalDate checkDate;
    private String checked;
    private Integer pointsEarned;
    private LocalDateTime createdAt;
}
