package com.rebirth.my.wardrobe;

import lombok.Data;

@Data
public class WardrobeVo {
    private String clothesId;
    private String userId;
    private String name;
    private String category;
    private String imageUrl;

    // Additional fields mapped to CLOTHING_ITEMS
    private String brand;
    private String color;
    private String season;
    private String style; // 스타일 (캐주얼/포멀/스포츠 등)
    private String detailDesc; // 상세 설명
    private String conditionGrade; // 상태 등급
    private String itemSize; // 사이즈 (Oracle 예약어 이슈로 변경)
    private String status;
    private String isForSale; // 판매중 여부 ('Y'/'N')
    private java.sql.Date purchaseDate;

    // 이미지 Base64 (등록 시 사용)
    private String imageBase64;

    // 개인 선호도 (별점 1-5, DB PREFERENCE NUMBER)
    private Integer personalNote;

    // 소재 정보 (CLOTHING_MATERIALS 테이블)
    private String material; // 주요 소재명 (예: "면", "폴리에스터")
    private String materialCode; // 소재 코드 (예: "MAT_001")
    private Integer materialPercentage; // 소재 비율 (예: 100)
}
