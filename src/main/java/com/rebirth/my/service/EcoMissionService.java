package com.rebirth.my.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rebirth.my.domain.EcoTodoTask;
import com.rebirth.my.domain.UserTodoCheck;
import com.rebirth.my.mapper.EcoMissionMapper;
import com.rebirth.my.mypage.MypageService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EcoMissionService {

    private final EcoMissionMapper ecoMissionMapper;
    private final MypageService mypageService;
    private final BadgeService badgeService;

    @Transactional
    public void updateMissionCheck(Long userId, Long taskId, boolean checked) {

        LocalDate today = LocalDate.now();

        // 1. 미션 정보 조회 (포인트 계산을 위해)
        EcoTodoTask task = mypageService.getTaskById(taskId);
        int points = (task != null) ? task.getDefaultPoints() : 10;

        // 2. 기존 체크 여부 조회
        UserTodoCheck existing = ecoMissionMapper.findUserCheck(userId, taskId, today);

        if (checked) {
            // 체크하는 경우
            if (existing == null) {
                UserTodoCheck newCheck = new UserTodoCheck();
                newCheck.setUserId(userId);
                newCheck.setTaskId(taskId);
                newCheck.setCheckDate(today);
                newCheck.setChecked("Y");
                newCheck.setPointsEarned(points);
                newCheck.setCreatedAt(LocalDateTime.now());
                ecoMissionMapper.insertUserCheck(newCheck);

                // 뱃지 획득 실시간 체크
                badgeService.checkAndAwardBadges(userId);

                // 포인트 가산 (자정 정산으로 변경)
                // updateUserPoints(userId, points);
            }
        } else {
            // 체크 해제하는 경우 -> 레코드를 삭제하여 새로고침 시 반영되도록 함
            if (existing != null) {
                ecoMissionMapper.deleteUserCheck(userId, taskId, today);

                // 포인트 차감 (자정 정산으로 변경)
                // updateUserPoints(userId, -points);
            }
        }
    }
}