package com.rebirth.my.ootd;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class OotdCommentVo {
    private Long commentId;
    private Long ootdId;
    private Long userId;
    private String userName;
    private String userProfileImage;
    private String content;
    private Long parentId; // 대댓글용
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private boolean isDeleted;
}
