package com.rebirth.my.user;

import lombok.Data;
import java.time.LocalDateTime;
import java.sql.Date;

@Data
public class UserVo {
    private Long id;
    private String loginId;
    private String password;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String addressDetail; // 상세주소 (건물명, 층, 호수 등)
    private Date birthDate;
    private String role; // USER, ADMIN
    private String status; // ACTIVE, BANNED, WITHDRAWAL_PENDING
    private String memImg;

    // New fields for email verification and withdrawal
    private String emailVerifStatus; // NONE, PENDING, VERIFIED
    private LocalDateTime withdrawalAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
