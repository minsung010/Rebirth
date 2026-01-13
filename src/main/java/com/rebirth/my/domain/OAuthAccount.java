package com.rebirth.my.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class OAuthAccount {

    private Long id;
    private Long userId; // Changed from User object to userId for MyBatis simplicity
    private String provider;
    private String providerUserId;
    private LocalDateTime linkedAt;
}
