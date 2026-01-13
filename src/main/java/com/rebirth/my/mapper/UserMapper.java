package com.rebirth.my.mapper;

import com.rebirth.my.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserMapper {
    void save(User user);

    Optional<User> findByEmail(@Param("email") String email);

    Optional<User> findByLoginId(@Param("loginId") String loginId);

    Optional<User> findByEmailOrLoginId(@Param("keyword") String keyword);

    java.util.List<User> searchUsers(@Param("keyword") String keyword, @Param("excludeUserId") Long excludeUserId);

    // ID로 사용자 조회 (단건)
    User getUserById(@Param("id") Long id);

    void update(User user);

    void deletePermanentlyByStatusAndDate(@Param("now") java.time.LocalDateTime now);

    void deleteById(@Param("id") Long id);
}
