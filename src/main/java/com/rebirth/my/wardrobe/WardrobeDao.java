package com.rebirth.my.wardrobe;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface WardrobeDao {
    int insertClothes(WardrobeVo vo);

    List<WardrobeVo> selectClothesList(String userId);

    List<WardrobeVo> selectClothesListByCategory(@Param("userId") String userId, @Param("category") String category);

    List<WardrobeVo> selectClothesListBySeason(@Param("userId") String userId, @Param("season") String season);

    List<WardrobeVo> selectClothesListByIds(java.util.List<Long> ids);

    List<WardrobeVo> selectAllClothes();

    List<WardrobeVo> searchClothesByKeyword(String userId, String keyword);

    // 판매 가능 목록 조회 (IN_CLOSET & IS_FOR_SALE='N')
    List<WardrobeVo> selectAvailableClothesList(String userId);

    List<WardrobeVo> selectAvailableClothesListByCategory(@Param("userId") String userId,
            @Param("category") String category);

    List<WardrobeVo> selectAvailableClothesListBySeason(@Param("userId") String userId, @Param("season") String season);

    List<WardrobeVo> searchAvailableClothesByKeyword(String userId, String keyword);

    int deleteClothes(@Param("clothesId") String clothesId, @Param("userId") String userId);

    // 개별 의류 조회
    WardrobeVo selectClothesById(@Param("clothesId") String clothesId, @Param("userId") String userId);

    // 의류 이미지 저장
    int insertClothingImage(@Param("clothesId") String clothesId, @Param("imageUrl") String imageUrl);

    // 의류 소재 저장
    int insertClothingMaterial(@Param("clothesId") String clothesId,
            @Param("materialCode") String materialCode,
            @Param("materialPercentage") Integer materialPercentage);

    // 소재명으로 코드 조회
    String getMaterialCode(@Param("material") String material);
}
