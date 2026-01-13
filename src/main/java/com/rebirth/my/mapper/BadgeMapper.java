package com.rebirth.my.mapper;

import com.rebirth.my.domain.Badge;
import com.rebirth.my.domain.UserBadge;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BadgeMapper {
    List<Badge> findAll();

    List<UserBadge> findUserBadges(Long userId);

    // 뱃지 가졌는지 확인
    int countUserBadge(@org.apache.ibatis.annotations.Param("userId") Long userId,
            @org.apache.ibatis.annotations.Param("badgeId") Long badgeId);

    // 뱃지 부여
    void insertUserBadge(UserBadge userBadge);
}
