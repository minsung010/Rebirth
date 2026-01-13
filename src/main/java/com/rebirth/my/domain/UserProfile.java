package com.rebirth.my.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class UserProfile {

    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Double totalWaterSavedL = 0.0;
    private Double totalCarbonSavedKg = 0.0;
    private Integer ecoPoints = 0;
    private Double totalCollectedKg = 0.0;
    private Integer totalClothingCount = 0;
    private LocalDateTime lastLoginAt;
    private String gender;
    private String stylePreference;
    private String activeDecoration; // 프로필 꾸미기 효과 (CSS Class)
}
