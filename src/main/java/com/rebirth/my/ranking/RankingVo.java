package com.rebirth.my.ranking;

import lombok.Data;

@Data
public class RankingVo {
    // 랭킹 공통
    private Long userId; // 사용자 ID (내부용)
    private String nickname; // 닉네임
    private String avatarUrl; // 프로필 이미지 URL
    private int rank; // 순위

    // 점수/수치 (타입에 따라 다른 값이 매핑됨)
    private int ecoPoints; // 에코 포인트
    private int donationCount; // 기부 횟수
    private int salesCount; // 판매 횟수

    // 표시용 포맷팅 값
    private String formattedScore; // UI에 표시할 포맷팅된 점수 (예: "1,200 P", "5 회")
    private String activeDecoration;
}
