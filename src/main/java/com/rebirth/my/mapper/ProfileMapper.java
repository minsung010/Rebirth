package com.rebirth.my.mapper;

import java.util.List;
import java.util.Map;

// Spring Legacy에서 Mybatis-Spring 연동 시 사용
// Spring Boot에서는 보통 @Mapper를 사용합니다.
import org.springframework.stereotype.Repository;

import com.rebirth.my.domain.User;

@Repository
public interface ProfileMapper {

    /**
     * 1. 새 프로필 이미지 기록 저장 (ProfileMapper.xml의 insertProfileHistory)
     * @param param Map: userId, imagePath 포함
     */
    void insertProfileHistory(Map<String, Object> param);

    /**
     * 2. 5개를 초과하는 가장 오래된 기록 삭제 (ProfileMapper.xml의 deleteOldestHistory)
     * @param userId 사용자 ID
     * @return 삭제된 레코드 수
     */
    int deleteOldestHistory(Long userId);

    /**
     * 3. 최근 5개 이미지 기록 조회 (ProfileMapper.xml의 selectRecentImageHistory)
     * @param userId 사용자 ID
     * @return 이미지 경로(String) 리스트
     */
    List<String> selectRecentImageHistory(Long userId);

    /** 
     * 4. 프로필 이미지 경로 업데이트 (ProfileMapper.xml의 updateUserProfileImage)
     * @param param Map: userId, imagePath 포함
     */
    void updateUserProfileImage(Map<String, Object> param);
    
    public User selectUserById(Long userId);
    
    void updateUserProfileAvatar(Map<String, Object> param);
    
}