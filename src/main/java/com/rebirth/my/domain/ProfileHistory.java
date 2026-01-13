// 경로: com.rebirth.my.domain.ProfileHistory
package com.rebirth.my.domain;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ProfileHistory {
    private Long histId;
    private Long userId; // 사용자 ID 타입에 맞게 조정하세요.
    private String imagePath;
    private LocalDateTime uploadedAt;

    // Getters and Setters, Constructors, toString() 등을 여기에 추가합니다.
    // Lombok을 사용한다면 @Data 어노테이션으로 대체 가능합니다.

}