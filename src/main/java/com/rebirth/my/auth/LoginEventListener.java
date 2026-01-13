package com.rebirth.my.auth;

import com.rebirth.my.service.LoginRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpSession;
import com.rebirth.my.mapper.UserProfileMapper;

/**
 * Spring Security의 로그인 성공 이벤트를 관찰하여
 * 로그인 방식을 가리지 않고(폼 로그인, 소셜 로그인 등)
 * 로그인 시간을 기록하는 비침투적 리스너입니다.
 */
@Component
@RequiredArgsConstructor
public class LoginEventListener implements ApplicationListener<AuthenticationSuccessEvent> {

    private final LoginRecordService loginRecordService;
    private final UserProfileMapper userProfileMapper;

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        Authentication authentication = event.getAuthentication();
        Object principal = authentication.getPrincipal();

        Long userId = null;

        // 1. 일반 로그인 (CustomUserDetails) 확인
        if (principal instanceof CustomUserDetails) {
            userId = ((CustomUserDetails) principal).getId();
        }
        // 2. OAuth2 로그인 (CustomOAuth2User) 확인
        else if (principal instanceof CustomOAuth2User) {
            userId = ((CustomOAuth2User) principal).getUser().getId();
        }

        if (userId != null) {
            System.out.println(">>> [LoginEventListener] 로그인 성공 이벤트 감지 (User ID: " + userId + ")");

            try {
                // 1. 기존 LAST_LOGIN_AT 시간을 먼저 조회하여 세션에 백업 (보안 강화 목적)
                userProfileMapper.findById(userId).ifPresent(profile -> {
                    if (profile.getLastLoginAt() != null) {
                        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder
                                .getRequestAttributes();
                        if (attr != null) {
                            HttpSession session = attr.getRequest().getSession(true);
                            session.setAttribute("previousLoginAt", profile.getLastLoginAt());
                            System.out.println(
                                    ">>> [LoginEventListener] 이전 로그인 시간 세션 백업 완료: " + profile.getLastLoginAt());
                        }
                    }
                });

                // 2. 현재 시간으로 업데이트
                loginRecordService.recordLoginTime(userId);
            } catch (Exception e) {
                System.err.println(">>> [LoginEventListener] 로그인 기록 도중 예외 발생: " + e.getMessage());
            }
        }
    }
}
