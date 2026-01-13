package com.rebirth.my.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import com.rebirth.my.auth.CustomUserDetails;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final com.rebirth.my.service.BadgeService badgeService;

    public CustomLoginSuccessHandler(com.rebirth.my.service.BadgeService badgeService) {
        this.badgeService = badgeService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        System.out.println(">>> [CustomLoginSuccessHandler] onAuthenticationSuccess start");

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails)) {
            System.err.println(">>> [CustomLoginSuccessHandler] Principal is not CustomUserDetails: "
                    + principal.getClass().getName());
            response.sendRedirect("/main");
            return;
        }

        CustomUserDetails userDetails = (CustomUserDetails) principal;
        System.out.println(">>> [CustomLoginSuccessHandler] User identified. ID: " + userDetails.getId() + ", Email: "
                + userDetails.getUsername());

        HttpSession session = request.getSession();
        session.setAttribute("userId", userDetails.getId());

        // 로그인 시점에 뱃지 획득 조건 재점검 (누락된 뱃지 자동 지급)
        try {
            badgeService.checkAndAwardBadges(userDetails.getId());
        } catch (Exception e) {
            System.err.println(">>> [CustomLoginSuccessHandler] Failed to check badges: " + e.getMessage());
        }

        System.out.println(">>> [CustomLoginSuccessHandler] Redirecting to /main");
        response.sendRedirect("/main");
    }
}
