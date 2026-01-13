package com.rebirth.my.mypage;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rebirth.my.auth.CustomOAuth2User;
import com.rebirth.my.auth.CustomUserDetails;
import com.rebirth.my.service.EcoMissionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/eco-mission")
@RequiredArgsConstructor
public class EcoMissionController {

    private final EcoMissionService ecoMissionService;

    @PostMapping("/check")
    public ResponseEntity<Void> checkMission(@RequestBody EcoMissionCheckRequest request) {

        // SecurityContextHolder를 통해 직접 인증 정보를 가져옴
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Long userId = getCurrentUserId(auth);

        if (userId == null) {
            throw new IllegalStateException("로그인된 사용자 정보가 없습니다.");
        }

        ecoMissionService.updateMissionCheck(userId, request.getTaskId(), request.isChecked());
        return ResponseEntity.ok().build();
    }

    private Long getCurrentUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        } else if (principal instanceof CustomOAuth2User) {
            return ((CustomOAuth2User) principal).getId();
        }

        return null;
    }
}
