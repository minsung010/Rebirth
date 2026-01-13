package com.rebirth.my.market;

import com.rebirth.my.domain.MarketVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface MarketDao {
        // 상품 등록 (CLOTHING_ITEMS에 Insert)
        int insertClothingItem(MarketVo vo);

        // 상품 이미지 등록 (CLOTHING_IMAGES에 Insert)
        int insertClothingImage(MarketVo vo);

        // 전체 판매 상품 목록 조회
        List<MarketVo> selectMarketList(@Param("userId") Long userId);

        // 상품 상세 조회
        MarketVo selectMarketItemDetail(Long id);

        // 판매 취소 (본인 확인을 위해 userId도 함께 전달)
        int cancelSale(@Param("id") Long id, @Param("userId") Long userId);

        // 옷장에서 판매하기 (기존 옷 상태 업데이트)
        int updateClothesForSale(MarketVo vo);

        // 찜하기 (Insert)
        int insertWish(@Param("marketId") Long marketId, @Param("userId") Long userId);

        // 찜 취소 (Delete)
        int deleteWish(@Param("marketId") Long marketId, @Param("userId") Long userId);

        // 찜 여부 확인
        int checkWishExists(@Param("marketId") Long marketId, @Param("userId") Long userId);

        // 찜 개수 조회
        int countWishes(Long marketId);

        // 상품 정보 수정
        int updateMarketItem(MarketVo vo);

        // 상품 상태 변경 (판매완료 등)
        int updateStatus(@Param("id") Long id, @Param("userId") Long userId, @Param("status") String status);

        // ========== 추가 이미지 관련 ==========

        // 추가 이미지 등록
        int insertAdditionalImage(@Param("clothingItemId") Long clothingItemId, @Param("imageUrl") String imageUrl);

        // 추가 이미지 목록 조회
        List<String> selectAdditionalImages(@Param("clothingItemId") Long clothingItemId);

        // 추가 이미지 삭제
        int deleteAdditionalImages(@Param("clothingItemId") Long clothingItemId);
}
