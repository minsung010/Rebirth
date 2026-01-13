package com.rebirth.my.auth;

import com.rebirth.my.domain.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final com.rebirth.my.service.BadgeService badgeService;

    public OAuth2SuccessHandler(com.rebirth.my.service.BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        CustomOAuth2User oauthUser = (CustomOAuth2User) authentication.getPrincipal();
        User user = oauthUser.getUser();

        System.out.println("=== OAuth2 Login Success Handler Triggered ===");
        System.out.println("User ID: " + user.getId());

        // 소셜 로그인 시점에 뱃지 획득 조건 재점검
        try {
            badgeService.checkAndAwardBadges(user.getId());
        } catch (Exception e) {
            System.err.println("=== OAuth2SuccessHandler: Failed to check badges: " + e.getMessage());
        }

        if ("PENDING".equals(user.getStatus())) {
            response.sendRedirect("/auth/social-join");
        } else {
            response.sendRedirect("/main");
        }
    }
}
