package com.rebirth.my.ootd;

import lombok.Data;
import java.sql.Date;
import java.sql.Timestamp;

@Data
public class OotdCalendarVo {
    private Long id;
    private Long userId;
    private Date eventDate; // 캘린더 날짜 (YYYY-MM-DD)
    private String title; // 메모
    private String imageBase64; // 이미지 데이터 (Base64)
    private Timestamp createdAt;
}
