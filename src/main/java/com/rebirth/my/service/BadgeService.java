package com.rebirth.my.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rebirth.my.domain.Badge;
import com.rebirth.my.domain.UserBadge;
import com.rebirth.my.mapper.BadgeMapper;
import com.rebirth.my.mapper.EcoTodoMapper;

import com.rebirth.my.mapper.UserProfileMapper;
import com.rebirth.my.domain.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeMapper badgeMapper;
    private final EcoTodoMapper ecoTodoMapper;
    private final UserProfileMapper userProfileMapper;

    /**
     * 사용자의 실시간 상태를 확인하여 획득 가능한 모든 뱃지를 부여합니다.
     */
    @Transactional
    public void checkAndAwardBadges(Long userId) {
        log.info("사용자(ID: {}) 뱃지 획득 여부 체크 시작", userId);

        // 1. 전체 뱃지 목록 조회
        List<Badge> allBadges = badgeMapper.findAll();

        // 2. 미션 완료 횟수 조회 (도전과제 조건 등으로 활용)
        int totalMissions = ecoTodoMapper.countTotalUserChecks(userId);

        // 3. 사용자 프로필 조회 (포인트, 의류 개수 등)
        UserProfile profile = userProfileMapper.findById(userId).orElse(null);

        for (Badge badge : allBadges) {
            log.debug(">>> Checking badge: {} (Condition: {})", badge.getName(), badge.getConditionType());
            // 4. 이미 획득한 뱃지인지 확인
            if (badgeMapper.countUserBadge(userId, badge.getId()) > 0) {
                continue;
            }

            // 5. 조건 달성 여부 확인
            boolean achieved = isConditionMet(badge, totalMissions, profile);
            log.debug(">>> Condition check for {}: result={}", badge.getName(), achieved);

            if (achieved) {
                UserBadge userBadge = new UserBadge();
                userBadge.setUserId(userId);
                userBadge.setBadgeId(badge.getId());
                badgeMapper.insertUserBadge(userBadge);
                log.info("사용자(ID: {}) 가 새로운 뱃지를 획득했습니다! [{} - {}]", userId, badge.getCode(), badge.getName());
            }
        }
    }

    private boolean isConditionMet(Badge badge, int totalMissions, UserProfile profile) {
        String type = badge.getConditionType();
        Double threshold = badge.getThresholdValue();

        if (type == null || threshold == null)
            return false;

        switch (type) {
            case "MISSION_COUNT":
                boolean missionMet = totalMissions >= threshold.intValue();
                log.debug(">>> MISSION_COUNT check: total={}, threshold={}, result={}", totalMissions, threshold,
                        missionMet);
                return missionMet;

            case "TOTAL_POINT": // 실제 DB 조건명: 총 포인트
                boolean pointMet = profile != null && profile.getEcoPoints() >= threshold.intValue();
                if (profile != null) {
                    log.debug(">>> TOTAL_POINT check: userPoints={}, threshold={}, result={}", profile.getEcoPoints(),
                            threshold, pointMet);
                }
                return pointMet;

            case "CLOSET_REG": // 실제 DB 조건명: 옷장 등록 수
                boolean clothingMet = profile != null && profile.getTotalClothingCount() >= threshold.intValue();
                if (profile != null) {
                    log.debug(">>> CLOSET_REG check: userClothes={}, threshold={}, result={}",
                            profile.getTotalClothingCount(), threshold, clothingMet);
                }
                return clothingMet;

            case "TOTAL_WEIGHT": // 실제 DB 조건명: 수거된 총 무게
                boolean weightMet = profile != null && profile.getTotalCollectedKg() != null
                        && profile.getTotalCollectedKg() >= threshold;
                if (profile != null) {
                    log.debug(">>> TOTAL_WEIGHT check: userWeight={}, threshold={}, result={}",
                            profile.getTotalCollectedKg(), threshold, weightMet);
                }
                return weightMet;

            case "COLLECT_COUNT": // 실제 DB 조건명: 수거 횟수 (현재 미구현)
                log.warn(">>> COLLECT_COUNT condition encountered. Not implemented yet.");
                return false;

            case "DAY_STREAK":
            case "REVIEW_COUNT":
                log.debug(">>> Condition {} not yet implemented.", type);
                return false;

            default:
                log.warn(">>> Unknown condition type: {}", type);
                return false;
        }
    }
}
