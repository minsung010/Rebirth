package com.rebirth.my.ootd;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ootd")
public class OotdController {

    @GetMapping("/list")
    public String list(Model model) {
        return "ootd/list";
    }
}
