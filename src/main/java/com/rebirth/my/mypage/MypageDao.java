package com.rebirth.my.mypage;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MypageDao {
    MypageVo selectUserInfo(String userId);

    int updateUserInfo(MypageVo vo);

    // 통합 활동내역 조회
    List<ActivityVo> selectActivityHistory(Long userId);

    // 나의 랭킹 조회
    MypageVo selectMyRanking(Long userId);

    // 관심상품 목록 조회
    List<WishlistItemVo> selectWishlist(Long userId);

    // 장식 소유 정보 저장
    int insertOwnedDecoration(Long userId, String itemCode);

    // 소유한 장식 코드 목록 조회
    List<String> selectOwnedItemCodes(Long userId);
}
