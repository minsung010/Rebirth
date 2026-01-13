package com.rebirth.my.ootd;

import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OotdDao {

    // OOTD 게시물 등록
    @Insert("""
                INSERT INTO OOTD_POSTS (
                    OOTD_ID, USER_ID, TITLE, DESCRIPTION, IMAGE_URL,
                    TOP_CLOTHES_ID, BOTTOM_CLOTHES_ID, TOP_TYPE, BOTTOM_TYPE,
                    TOP_COLOR, BOTTOM_COLOR, SHOE_COLOR, BACKGROUND_THEME, TAGS
                ) VALUES (
                    OOTD_SEQ.NEXTVAL, #{userId}, #{title}, #{description}, #{imageUrl},
                    #{topClothesId}, #{bottomClothesId}, #{topType}, #{bottomType},
                    #{topColor}, #{bottomColor}, #{shoeColor}, #{backgroundTheme}, #{tags}
                )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "ootdId", keyColumn = "OOTD_ID")
    int insertOotd(OotdVo vo);

    // OOTD 목록 조회 (최신순)
    @Select("""
                SELECT o.*, m.NAME as USER_NAME, m.MEM_IMG as USER_PROFILE_IMAGE,
                       (SELECT COUNT(*) FROM OOTD_LIKES l WHERE l.OOTD_ID = o.OOTD_ID) as LIKE_COUNT
                FROM OOTD_POSTS o
                LEFT JOIN USERS m ON o.USER_ID = m.ID
                ORDER BY o.CREATED_AT DESC
                FETCH FIRST #{limit} ROWS ONLY
            """)
    List<OotdVo> selectOotdList(@Param("limit") int limit);

    // 특정 사용자의 OOTD 목록
    @Select("""
                SELECT o.*, m.NAME as USER_NAME, m.MEM_IMG as USER_PROFILE_IMAGE,
                       (SELECT COUNT(*) FROM OOTD_LIKES l WHERE l.OOTD_ID = o.OOTD_ID) as LIKE_COUNT
                FROM OOTD_POSTS o
                LEFT JOIN USERS m ON o.USER_ID = m.ID
                WHERE o.USER_ID = #{userId}
                ORDER BY o.CREATED_AT DESC
            """)
    List<OotdVo> selectOotdListByUser(@Param("userId") Long userId);

    // OOTD 상세 조회
    @Select("""
                SELECT o.*, m.NAME as USER_NAME, m.MEM_IMG as USER_PROFILE_IMAGE,
                       (SELECT COUNT(*) FROM OOTD_LIKES l WHERE l.OOTD_ID = o.OOTD_ID) as LIKE_COUNT
                FROM OOTD_POSTS o
                LEFT JOIN USERS m ON o.USER_ID = m.ID
                WHERE o.OOTD_ID = #{ootdId}
            """)
    OotdVo selectOotdById(@Param("ootdId") Long ootdId);

    // 조회수 증가
    @Update("UPDATE OOTD_POSTS SET VIEW_COUNT = NVL(VIEW_COUNT, 0) + 1 WHERE OOTD_ID = #{ootdId}")
    int incrementViewCount(@Param("ootdId") Long ootdId);

    // OOTD 삭제
    @Delete("DELETE FROM OOTD_POSTS WHERE OOTD_ID = #{ootdId} AND USER_ID = #{userId}")
    int deleteOotd(@Param("ootdId") Long ootdId, @Param("userId") Long userId);

    // 좋아요 추가
    @Insert("INSERT INTO OOTD_LIKES (OOTD_ID, USER_ID) VALUES (#{ootdId}, #{userId})")
    int insertLike(@Param("ootdId") Long ootdId, @Param("userId") Long userId);

    // 좋아요 취소
    @Delete("DELETE FROM OOTD_LIKES WHERE OOTD_ID = #{ootdId} AND USER_ID = #{userId}")
    int deleteLike(@Param("ootdId") Long ootdId, @Param("userId") Long userId);

    // 좋아요 여부 확인
    @Select("SELECT COUNT(*) FROM OOTD_LIKES WHERE OOTD_ID = #{ootdId} AND USER_ID = #{userId}")
    int countLike(@Param("ootdId") Long ootdId, @Param("userId") Long userId);

    /* --- OOTD 캘린더 (DB 저장) --- */

    // 캘린더 이벤트 등록
    // 시퀀스가 없는 경우를 대비하여 MAX(ID) + 1 사용
    @SelectKey(statement = "SELECT NVL(MAX(ID), 0) + 1 FROM OOTD_CALENDAR", keyProperty = "id", before = true, resultType = Long.class)
    @Insert("""
                INSERT INTO OOTD_CALENDAR (ID, USER_ID, EVENT_DATE, TITLE, IMAGE_BASE64)
                VALUES (#{id}, #{userId}, #{eventDate}, #{title}, #{imageBase64})
            """)
    int insertCalendarEvent(OotdCalendarVo vo);

    // 캘린더 이벤트 목록 조회
    @Select("""
                SELECT ID, USER_ID, EVENT_DATE, TITLE, IMAGE_BASE64 as imageBase64, CREATED_AT
                FROM OOTD_CALENDAR
                WHERE USER_ID = #{userId}
                ORDER BY EVENT_DATE
            """)
    List<OotdCalendarVo> selectCalendarEventsByUserId(@Param("userId") Long userId);

    // 캘린더 이벤트 삭제
    @Delete("DELETE FROM OOTD_CALENDAR WHERE ID = #{id} AND USER_ID = #{userId}")
    int deleteCalendarEvent(@Param("id") Long id, @Param("userId") Long userId);

    // 특정 날짜의 캘린더 이벤트 조회 (챗봇용)
    @Select("""
                SELECT ID, USER_ID, EVENT_DATE, TITLE, IMAGE_BASE64 as imageBase64, CREATED_AT
                FROM OOTD_CALENDAR
                WHERE USER_ID = #{userId} AND EVENT_DATE = #{eventDate}
            """)
    OotdCalendarVo selectCalendarEventByDate(@Param("userId") Long userId, @Param("eventDate") java.sql.Date eventDate);
}
