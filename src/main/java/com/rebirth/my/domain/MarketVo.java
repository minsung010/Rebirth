package com.rebirth.my.domain;

import lombok.Data;
import java.sql.Timestamp;
import java.util.List;

@Data
public class MarketVo {
    private Long id; // CLOTHING_ITEMS.ID
    private Long userId; // CLOTHING_ITEMS.USER_ID
    private String sellerName; // USERS.NAME or NICKNAME
    private String name; // CLOTHING_ITEMS.NAME (상품명)
    private String category; // CLOTHING_ITEMS.CATEGORY
    private String detailDesc; // CLOTHING_ITEMS.DETAIL_DESC
    private Integer targetPrice; // CLOTHING_ITEMS.TARGET_PRICE (판매가격)
    private String imageUrl; // CLOTHING_IMAGES.IMAGE_URL (대표 이미지)
    private String status; // CLOTHING_ITEMS.STATUS
    private String isForSale; // CLOTHING_ITEMS.IS_FOR_SALE ('Y')
    private Timestamp createdAt; // CLOTHING_ITEMS.CREATED_AT
    private String clothesId; // 옷장에서 판매하기 시 기존 옷 ID

    // For Wishlist
    private int wishCount; // 찜 개수
    private boolean isWished; // 현재 사용자의 찜 여부

    // For Map-based Search
    private String sellerAddress; // 판매자 주소
    private Double latitude; // 위도
    private Double longitude; // 경도
    private Double distance; // 검색 기준점과의 거리 (km)

    // Trade Location
    private String tradeLocation; // 희망 거래 장소

    // 추가 이미지 (뒷면, 디테일, 라벨 등)
    private List<String> additionalImages; // 추가 이미지 URL 목록
    private String additionalImagesJson; // 프론트에서 전송할 때 사용 (JSON 문자열)
}
