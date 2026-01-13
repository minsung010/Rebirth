package com.rebirth.my.mypage;

import lombok.Data;

@Data
public class WishlistItemVo {
    private Long id; // 상품 ID
    private String name; // 상품명
    private Integer price; // 가격
    private String category; // 카테고리
    private String imageUrl; // 대표 이미지 URL
}
