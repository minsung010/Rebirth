package com.rebirth.my.ootd;

import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OotdCommentDao {

    // 댓글 등록
    @Insert("""
                INSERT INTO OOTD_COMMENTS (COMMENT_ID, OOTD_ID, USER_ID, CONTENT, PARENT_ID)
                VALUES (OOTD_COMMENT_SEQ.NEXTVAL, #{ootdId}, #{userId}, #{content}, #{parentId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "commentId", keyColumn = "COMMENT_ID")
    int insertComment(OotdCommentVo vo);

    // 댓글 목록 조회
    @Select("""
                SELECT c.*, m.NAME as USER_NAME, m.PROFILE_IMAGE as USER_PROFILE_IMAGE
                FROM OOTD_COMMENTS c
                LEFT JOIN MEMBERS m ON c.USER_ID = m.ID
                WHERE c.OOTD_ID = #{ootdId} AND c.IS_DELETED = 'N'
                ORDER BY c.CREATED_AT ASC
            """)
    List<OotdCommentVo> selectCommentsByOotdId(@Param("ootdId") Long ootdId);

    // 댓글 수 조회
    @Select("SELECT COUNT(*) FROM OOTD_COMMENTS WHERE OOTD_ID = #{ootdId} AND IS_DELETED = 'N'")
    int countCommentsByOotdId(@Param("ootdId") Long ootdId);

    // 댓글 삭제 (소프트 삭제)
    @Update("UPDATE OOTD_COMMENTS SET IS_DELETED = 'Y', UPDATED_AT = CURRENT_TIMESTAMP WHERE COMMENT_ID = #{commentId} AND USER_ID = #{userId}")
    int deleteComment(@Param("commentId") Long commentId, @Param("userId") Long userId);

    // 댓글 수정
    @Update("UPDATE OOTD_COMMENTS SET CONTENT = #{content}, UPDATED_AT = CURRENT_TIMESTAMP WHERE COMMENT_ID = #{commentId} AND USER_ID = #{userId}")
    int updateComment(@Param("commentId") Long commentId, @Param("userId") Long userId,
            @Param("content") String content);
}
