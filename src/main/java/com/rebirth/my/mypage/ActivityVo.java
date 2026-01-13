package com.rebirth.my.mypage;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 마이페이지 통합 활동내역 조회를 위한 VO
 */
@Data
public class ActivityVo {
    private LocalDateTime actDate; // 활동 일시
    private String actType; // 활동 유형 (SALE, PURCHASE, BADGE, DONATION, MISSION)
    private String actTitle; // 활동 제목 (상품명, 뱃지명 등)
    private String actDetail; // 부가 상세 정보
}
