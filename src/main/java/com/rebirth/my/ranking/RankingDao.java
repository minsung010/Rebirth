package com.rebirth.my.ranking;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RankingDao {

    // 전체 회원의 랭킹 리스트 조회 (페이징)
    List<RankingVo> selectRankingList(@Param("type") String type,
            @Param("offset") int offset,
            @Param("limit") int limit);

    // 전체 회원 수 조회 (페이징 계산용)
    int countTotalUsers();

    // 나의 랭킹 정보 상세 조회 (MypageDao.selectMyRanking과 유사하지만, 단일 VO로 합칠지 고민)
    // 여기서는 내 순위만 가져와도 되지만, 상단 카드 구성을 위해 구체적 정보 필요
    com.rebirth.my.mypage.MypageVo selectMyRankingDetail(@Param("userId") Long userId);
}
