package com.rebirth.my.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import com.rebirth.my.domain.User;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class PendingUserFilter extends OncePerRequestFilter {

    private static final List<String> WHITELIST = Arrays.asList(
            "/auth/social-join",
            "/auth/logout",
            "/css/", "/js/", "/images/", "/img/", "/video/", "/error",
            "/api/email/", "/auth/check-id", "/auth/check-email"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String requestURI = request.getRequestURI();

        // 화이트리스트에 없는 경로로 접근 시 체크
        if (auth != null && auth.isAuthenticated() && !isWhitelisted(requestURI)) {
            Object principal = auth.getPrincipal();
            
            // 소셜 로그인 사용자 체크
            if (principal instanceof CustomOAuth2User) {
                User user = ((CustomOAuth2User) principal).getUser();
                if ("PENDING".equals(user.getStatus())) {
                    response.sendRedirect("/auth/social-join");
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isWhitelisted(String uri) {
        for (String path : WHITELIST) {
            if (uri.startsWith(path)) {
                return true;
            }
        }
        return false;
    }
}
