package com.rebirth.my.mypage;

import lombok.Data;

@Data
public class MypageVo {
    // 프로필
    private String userId; // ID
    private String userName; // 닉네임
    private String avatarUrl; // 프사URL
    private int ecoPoint; // 에코포인트
    private String gender; // 성별
    private String activeDecoration; // 프로필 꾸미기 효과
    // 활동지표
    private double totalWater; // 물절약
    private double totalCarbon; // CO2절약
    private double totalEnergy; // 에너지절약
    // 지표 포매팅거친값
    private String formattedTotalWater;
    private String formattedTotalCarbon;
    private String formattedTotalEnergy;
    // 뱃지
    private int badgeCount; // 뱃지 획득 개수
    private int totalBadges; // 전체 뱃지 개수
    private java.util.List<com.rebirth.my.domain.Badge> badges; // 뱃지 목록

    // 최근 로그인 정보
    private java.time.LocalDateTime lastLoginAt;
    private java.time.LocalDateTime previousLoginAt; // 세션 보관용 이전 로그인 시간

    // 탈퇴 상태 관련
    private String status;
    private java.time.LocalDateTime withdrawalAt;

    // 연동된 소셜 계정 목록
    private java.util.List<String> linkedProviders;

    // 통합 활동 내역
    private java.util.List<ActivityVo> activityHistory;

    // 나의 랭킹 정보
    private int ecoPointRank;
    private int donationRank;
    private int salesRank;
    private int totalUsers;

    // 랭킹 산정 기준이 되는 실제 실적 수치
    private int donationCount;
    private int salesCount;

    // 관심상품 목록
    private java.util.List<WishlistItemVo> wishlist;
}
