package com.rebirth.my.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String loginId;

    private String password;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String addressDetail; // 상세주소 (건물명, 층, 호수 등)
    private LocalDateTime birthDate;
    private String role = "USER";
    private String status = "ACTIVE";
    private String emailVerifStatus = "NONE";
    private String memImg;
    private LocalDateTime withdrawalAt; // 탈퇴 예정일
    private String activeDecoration; // 프로필 꾸미기 효과 (global support)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
