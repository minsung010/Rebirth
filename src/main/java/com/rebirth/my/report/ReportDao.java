package com.rebirth.my.report;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface ReportDao {
    // 신고하기
    void insertReport(ReportVo reportVo);

    // 신고 목록 (관리자용) - 페이징 등은 추후 고려, 일단 전체 or 검색
    List<ReportVo> selectReportList(Map<String, Object> params);

    // 신고 상세 조회
    ReportVo selectReportById(Long id);

    // 신고 처리 상태 업데이트
    void updateReportStatus(Map<String, Object> params);

    // 제재 내역 저장
    void insertReportAction(ReportActionVo actionVo);

    // 유저별 누적 신고 수 (필요시)
    int countReportsByTargetUser(Long targetUserId);

    // 신고 삭제 (필요시)
    void deleteReport(Long id);
}
