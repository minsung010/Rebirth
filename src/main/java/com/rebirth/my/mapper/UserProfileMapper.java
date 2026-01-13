package com.rebirth.my.mapper;

import com.rebirth.my.domain.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserProfileMapper {
    void save(UserProfile userProfile);

    Optional<UserProfile> findById(@Param("userId") Long userId);

    void update(UserProfile userProfile);

    int updateLastLoginAt(@Param("userId") Long userId, @Param("lastLoginAt") java.time.LocalDateTime lastLoginAt);
}
