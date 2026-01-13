package com.rebirth.my.service;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.rebirth.my.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginRecordService {

    private final UserProfileMapper userProfileMapper;

    @Transactional
    public void recordLoginTime(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        // 업데이트 수행 및 결과 행 수 확인
        int updatedRows = userProfileMapper.updateLastLoginAt(userId, now);

        if (updatedRows > 0) {
            System.out.println(
                    "[LoginRecordService] User ID: " + userId + " | 시간: " + now + " | 로그인 일자 업데이트 성공 (행 수: "
                            + updatedRows
                            + ")");
        } else {
            System.err.println(
                    "[LoginRecordService] User ID: " + userId + " | 업데이트 실패 (USER_PROFILES 테이블에 해당 ID가 존재하지 않을 수 있음)");
        }
    }
}
