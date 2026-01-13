package com.rebirth.my.mapper;

import java.time.LocalDate;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.rebirth.my.domain.UserTodoCheck;

@Mapper
public interface UserTodoCheckMapper {

    // 오늘 날짜 기준으로 이미 체크한 기록 조회
    UserTodoCheck findByUserIdAndTaskIdAndCheckDate(@Param("userId") Long userId,
                                                    @Param("taskId") Long taskId,
                                                    @Param("checkDate") LocalDate checkDate);

    // INSERT
    void insert(UserTodoCheck todoCheck);

    // UPDATE
    void update(UserTodoCheck todoCheck);
}