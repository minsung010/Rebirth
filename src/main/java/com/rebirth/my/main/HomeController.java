package com.rebirth.my.main;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // 메인 홈: /, /home 둘 다 접속 가능 하게
    @GetMapping({"/", "/home"})
    public String home(Model model) {
        model.addAttribute("pageTitle", "ReBirth 메인 홈");
        return "home";
    }
    
    // 리버스 메인 페이지
    @GetMapping("/main")
    public String main(Model model) {
        model.addAttribute("pageTitle", "리버스 메인");
        return "main";
    }
}