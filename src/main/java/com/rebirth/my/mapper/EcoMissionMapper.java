package com.rebirth.my.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.rebirth.my.domain.EcoMission;
import com.rebirth.my.domain.UserTodoCheck;

@Mapper
public interface EcoMissionMapper {

    List<EcoMission> findTodayMissions(@Param("userId") Long userId,
            @Param("today") LocalDate today);

    UserTodoCheck findUserCheck(@Param("userId") Long userId,
            @Param("taskId") Long taskId,
            @Param("today") LocalDate today);

    void insertUserCheck(UserTodoCheck check);

    List<UserTodoCheck> findAllCompletedChecksByDate(@Param("today") LocalDate today);

    void updateUserCheck(UserTodoCheck check);

    // 체크 해제를 위해 레코드 삭제 기능을 추가합니다.
    void deleteUserCheck(@Param("userId") Long userId,
            @Param("taskId") Long taskId,
            @Param("today") LocalDate today);
}