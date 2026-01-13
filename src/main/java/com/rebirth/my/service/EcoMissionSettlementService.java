package com.rebirth.my.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rebirth.my.domain.UserTodoCheck;
import com.rebirth.my.mapper.EcoMissionMapper;
import com.rebirth.my.mapper.UserMapper;
import com.rebirth.my.mapper.UserProfileMapper;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EcoMissionSettlementService {

    private final EcoMissionMapper ecoMissionMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserMapper userMapper;
    private final BadgeService badgeService;

    /**
     * 매일 자정(00:00:01)에 어제 완료한 에코 미션의 포인트를 일괄 정산하고,
     * 탈퇴 유예 기간이 지난 회원을 영구 삭제합니다.
     */
    @Scheduled(cron = "1 0 0 * * *")
    @Transactional
    public void performMidnightPostProcessing() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate yesterday = now.toLocalDate().minusDays(1);
        log.info("자정 통합 후처리 시작 - 대상 날짜: {}", yesterday);

        // 1. 에코 미션 포인트 정산
        settleMissions(yesterday);

        // 2. 탈퇴 대기 회원 영구 삭제
        cleanupWithdrawnUsers(now);

        log.info("자정 통합 후처리 종료");
    }

    private void settleMissions(LocalDate date) {
        log.info("에코 미션 포인트 정산 시작...");
        List<UserTodoCheck> checks = ecoMissionMapper.findAllCompletedChecksByDate(date);

        if (checks.isEmpty()) {
            log.info("{} 날짜에 정산할 미션 내역이 없습니다.", date);
            return;
        }

        Map<Long, Integer> userPointsMap = checks.stream()
                .collect(Collectors.groupingBy(
                        UserTodoCheck::getUserId,
                        Collectors.summingInt(UserTodoCheck::getPointsEarned)));

        userPointsMap.forEach((userId, totalPoints) -> {
            userProfileMapper.findById(userId).ifPresent(profile -> {
                int beforePoints = profile.getEcoPoints();
                profile.setEcoPoints(beforePoints + totalPoints);
                userProfileMapper.update(profile);
                log.info("사용자(ID: {}) 포인트 정산 완료: {} -> {} (+{})",
                        userId, beforePoints, profile.getEcoPoints(), totalPoints);

                // 뱃지 획득 여부 체크
                badgeService.checkAndAwardBadges(userId);
            });
        });
        log.info("미션 정산 완료 (총 {}명)", userPointsMap.size());
    }

    private void cleanupWithdrawnUsers(LocalDateTime now) {
        log.info("탈퇴 대기 회원 삭제 처리 시작...");
        userMapper.deletePermanentlyByStatusAndDate(now);
        log.info("탈퇴 대기 회원 삭제 처리 완료");
    }
}
