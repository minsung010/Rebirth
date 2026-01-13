package com.rebirth.my.ootd;

import lombok.Data;
import java.sql.Timestamp;

@Data
public class OotdVo {
    private Long ootdId;
    private Long userId;
    private String userName;
    private String userProfileImage;
    private String title;
    private String description;
    private String imageUrl; // 스크린샷 이미지
    private String topClothesId; // 상의 옷장 아이템 ID
    private String bottomClothesId; // 하의 옷장 아이템 ID
    private String topType; // 상의 종류 (tshirt, longsleeve, dress)
    private String bottomType; // 하의 종류 (pants, skirt, shorts)
    private String topColor; // 상의 색상 (hex)
    private String bottomColor; // 하의 색상 (hex)
    private String shoeColor; // 신발 색상 (hex)
    private String backgroundTheme; // 배경 테마
    private String tags; // 태그 (콤마 구분)
    private Integer likeCount;
    private Integer viewCount;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // 좋아요 여부 (조회 시 사용)
    private boolean liked;
}
