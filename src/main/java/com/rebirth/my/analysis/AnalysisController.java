package com.rebirth.my.analysis;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/analysis")
public class AnalysisController {

    @GetMapping("")
    public String analysisMain(Model model) {
        // TODO: 분석 메인 페이지 로직
        return "analysis/main";
    }

    @GetMapping("/disposal")
    public String disposal(Model model) {
        return "analysis/disposal";
    }
}
