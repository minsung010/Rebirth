package com.rebirth.my.auth;

import lombok.Data;

@Data
public class JoinRequest {
    private String email;
    private String loginId;
    private String password;
    private String nickname;
    private String residentNumberFront;
    private String residentNumberBack;
    private String phone;
    private String zipcode;
    private String address;
    private String detailAddress;

    // For Social Join
    private String birthDateString; // YYYYMMDD
    private String gender; // MALE, FEMALE
}
