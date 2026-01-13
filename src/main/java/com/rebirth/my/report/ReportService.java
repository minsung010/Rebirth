package com.rebirth.my.report;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class ReportService {

    @Autowired
    private ReportDao reportDao;

    // 신고 접수
    @Transactional
    public void createReport(ReportVo reportVo) {
        reportDao.insertReport(reportVo);
    }

    // 신고 목록 조회 (관리자)
    public List<ReportVo> getReportList(Map<String, Object> params) {
        return reportDao.selectReportList(params);
    }

    // 신고 상세 (관리자)
    public ReportVo getReportDetail(Long id) {
        return reportDao.selectReportById(id);
    }

    // 신고 처리 및 제재 (관리자)
    @Transactional
    public void processReport(Long reportId, ReportActionVo actionVo) {
        // 1. 제재 내역 저장
        actionVo.setReportId(reportId);
        reportDao.insertReportAction(actionVo);

        // 2. 신고 상태 'Y'로 업데이트 (이미 처리됨)
        Map<String, Object> params = new HashMap<>();
        params.put("id", reportId);
        params.put("isProcessed", "Y");
        reportDao.updateReportStatus(params);

        // TODO: actionType에 따라 유저 상태(STATUS)를 BANNED 등으로 변경하는 로직 추가 가능
        // userDao.updateStatus(targetUserId, "BANNED");
    }
}
