package com.rebirth.my.donation;

import com.rebirth.my.domain.MarketVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface DonationDao {
    // 내 옷장 아이템 조회 (판매중 아니고, 기부 안된 것들)
    List<MarketVo> selectClosetItems(Long userId);

    // 기부 상태로 업데이트
    int updateItemToDonated(@Param("id") Long id,
            @Param("userId") Long userId,
            @Param("disposalMethod") String disposalMethod);
}
