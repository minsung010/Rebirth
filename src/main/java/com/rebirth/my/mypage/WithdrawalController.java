package com.rebirth.my.mypage;

import java.security.Principal;
import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rebirth.my.domain.User;
import com.rebirth.my.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/mypage/withdraw")
@RequiredArgsConstructor
public class WithdrawalController {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("")
    public String withdrawForm() {
        return "mypage/withdraw";
    }

    @PostMapping("")
    public String processWithdraw(@RequestParam("password") String password,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        if (principal == null)
            return "redirect:/auth/login";

        User user = userMapper.findByEmailOrLoginId(principal.getName()).orElse(null);

        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            // 탈퇴 유예 기간 설정 (3일 후)
            user.setStatus("PENDING_WITHDRAWAL");
            user.setWithdrawalAt(LocalDateTime.now().plusDays(3));
            userMapper.update(user);

            redirectAttributes.addFlashAttribute("successMessage", "탈퇴 신청이 완료되었습니다. 3일 후 계정이 영구 삭제됩니다.");
            return "redirect:/mypage";
        } else {
            redirectAttributes.addFlashAttribute("error", "비밀번호가 일치하지 않습니다.");
            return "redirect:/mypage/withdraw";
        }
    }

    @PostMapping("/cancel")
    public String cancelWithdraw(Principal principal,
            RedirectAttributes redirectAttributes) {
        if (principal == null)
            return "redirect:/auth/login";

        User user = userMapper.findByEmailOrLoginId(principal.getName()).orElse(null);

        if (user != null && "PENDING_WITHDRAWAL".equals(user.getStatus())) {
            user.setStatus("ACTIVE");
            user.setWithdrawalAt(null);
            userMapper.update(user);

            redirectAttributes.addFlashAttribute("successMessage", "탈퇴 신청이 취소되었습니다.");
        }

        return "redirect:/mypage";
    }
}
