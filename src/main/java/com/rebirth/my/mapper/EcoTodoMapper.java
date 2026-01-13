package com.rebirth.my.mapper;

import com.rebirth.my.domain.EcoTodoTask;
import com.rebirth.my.domain.UserTodoCheck;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface EcoTodoMapper {
    List<EcoTodoTask> findAllActiveTasks();

    List<UserTodoCheck> findUserChecks(@Param("userId") Long userId, @Param("checkDate") LocalDate checkDate);

    void insertCheck(UserTodoCheck check);

    void deleteCheck(@Param("userId") Long userId, @Param("taskId") Long taskId,
            @Param("checkDate") LocalDate checkDate);

    int countTotalUserChecks(@Param("userId") Long userId);

    int countTasks();

    void insertTask(EcoTodoTask task);
}
