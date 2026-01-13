package com.rebirth.my.report;

import com.rebirth.my.user.UserVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ReportController {

    @Autowired
    private ReportService reportService;

    // 신고 팝업 페이지
    @GetMapping("/report/popup")
    public String reportPopup(@RequestParam("targetId") Long targetId, Model model) {
        model.addAttribute("targetUserId", targetId);
        return "report/popup";
    }

    // 신고 등록 API
    @PostMapping("/report/api/create")
    @ResponseBody
    public Map<String, Object> createReport(@RequestBody ReportVo reportVo, HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        // 세션에서 로그인한 유저 ID 가져오기 (없으면 테스트용 1번)
        UserVo loginUser = (UserVo) session.getAttribute("loginUser");
        Long reporterId = (loginUser != null) ? loginUser.getId() : 1L;

        reportVo.setReporterId(reporterId);

        try {
            reportService.createReport(reportVo);
            result.put("success", true);
            result.put("message", "신고가 접수되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "오류가 발생했습니다.");
        }
        return result;
    }

    // [관리자] 신고 목록 페이지
    @GetMapping("/admin/report/list")
    public String adminReportList(Model model) {
        // TODO: 페이징, 필터링
        List<ReportVo> list = reportService.getReportList(new HashMap<>());
        model.addAttribute("reportList", list);
        return "admin/report_list";
    }

    // [관리자] 신고 처리 API
    @PostMapping("/admin/report/action")
    @ResponseBody
    public Map<String, Object> processReport(@RequestBody ReportActionVo actionVo, HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        // 관리자 ID (세션 없으면 0번 : 시스템)
        UserVo loginUser = (UserVo) session.getAttribute("loginUser");
        Long adminId = (loginUser != null) ? loginUser.getId() : 0L; // 0: System/Admin

        actionVo.setAdminId(adminId);

        try {
            reportService.processReport(actionVo.getReportId(), actionVo);
            result.put("success", true);
            result.put("message", "처리가 완료되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            result.put("success", false);
            result.put("message", "처리 중 오류 발생");
        }

        return result;
    }
}
