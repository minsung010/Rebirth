package com.rebirth.my.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rebirth.my.domain.User;
import com.rebirth.my.mapper.ProfileMapper;

@Service
public class ProfileService {

    @Autowired
    private ProfileMapper profileMapper;

    // 🌟 수정: 경로는 Controller에서 관리하거나 DB에 저장된 값을 그대로 사용합니다.

    /**
     * 1. 새 프로필 이미지 업로드 및 히스토리 관리
     * 이미지 업로드 성공 후 호출되는 핵심 로직입니다.
     * 
     * @param userId       현재 사용자 ID
     * @param newImagePath 새로 업로드된 이미지의 저장 경로
     */
    @Transactional
    public void uploadAndManageProfileImage(Long userId, String newImageUrl) {
        try {
            // 🌟 수정: Controller에서 이미 웹 경로(/uploads/...)를 전달받으므로 그대로 저장합니다.
            // 필요시 여기서 경로 검증이나 변환을 추가할 수 있습니다.
            Map<String, Object> updateParam = new HashMap<>();
            updateParam.put("userId", userId);
            updateParam.put("imagePath", newImageUrl);

            // 1. USERS 테이블 업데이트 (헤더용)
            profileMapper.updateUserProfileImage(updateParam);

            // 2. USER_PROFILE 테이블 업데이트 (마이페이지/본문용)
            profileMapper.updateUserProfileAvatar(updateParam);

            // 3. 히스토리 테이블에 새 기록 저장
            profileMapper.insertProfileHistory(updateParam);

            // 4. 5개가 초과된 가장 오래된 기록 삭제
            profileMapper.deleteOldestHistory(userId);

        } catch (Exception e) {
            System.err.println("!!! [Upload DB Error] 프로필 업로드 관련 DB 처리 중 오류 발생");
            throw new RuntimeException("프로필 이미지 DB 처리 실패", e);
        }
    }

    /**
     * 2. 최근 5개 이미지 기록 조회
     * 프론트엔드에 히스토리 썸네일을 보여주기 위해 호출됩니다.
     * 
     * @param userId 현재 사용자 ID
     * @return 최근 5개의 이미지 경로 목록
     */
    public List<String> getRecentImageHistory(Long userId) {
        try {
            return profileMapper.selectRecentImageHistory(userId);
        } catch (Exception e) {
            System.err.println("!!! [DB Error] Failed to get profile history: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("프로필 히스토리 조회 실패", e);
        }
    }

    /**
     * 3. 히스토리 이미지로 프로필 복원 (원클릭 변경)
     * 
     * @param userId           현재 사용자 ID
     * @param historyImagePath 히스토리에서 선택된 이미지 경로
     */
    @Transactional
    public void restoreProfileImage(Long userId, String historyImagePath) {
        try {
            Map<String, Object> param = new HashMap<>();
            param.put("userId", userId);
            param.put("imagePath", historyImagePath);
            profileMapper.updateUserProfileImage(param);
            profileMapper.updateUserProfileAvatar(param);
        } catch (Exception e) {
            System.err.println("!!! [Restore DB Error] 프로필 이미지 복원 중 오류 발생:");
            System.err.println("!!! Error Type: " + e.getClass().getName());
            System.err.println("!!! Error Message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("프로필 이미지 복원 실패", e);
        }
    }

    public User findUserById(Long userId) {
        // 맵퍼를 통해 USERS 테이블에서 해당 ID의 User 객체 전체를 가져오는 쿼리를 실행해야 합니다.
        return profileMapper.selectUserById(userId);
        // Mapper에 selectUserById 쿼리 추가 필요
    }
}