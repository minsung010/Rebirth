package com.rebirth.my.auth;

import com.rebirth.my.service.EmailService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<String> sendVerificationCode(@RequestParam String email, HttpSession session) {
        System.out.println("EmailController.sendVerificationCode called with email: " + email); // Debug Log
        try {
            String code = emailService.sendVerificationCode(email);
            session.setAttribute("emailVerificationCode", code);
            session.setAttribute("emailVerified", false);
            // 세션 유효 시간 설정 (3분)
            session.setMaxInactiveInterval(180);
            return ResponseEntity.ok("인증 코드가 발송되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("이메일 발송 실패: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyCode(@RequestParam String code, HttpSession session) {
        String savedCode = (String) session.getAttribute("emailVerificationCode");
        
        System.out.println("=========================================");
        System.out.println("Verifying Code");
        System.out.println("Input Code: [" + code + "]");
        System.out.println("Saved Code: [" + savedCode + "]");
        boolean isMatch = savedCode != null && savedCode.equals(code);
        System.out.println("Verification Result: " + isMatch);
        System.out.println("=========================================");

        if (isMatch) {
            session.setAttribute("emailVerified", true);
            return ResponseEntity.ok("인증이 완료되었습니다.");
        } else {
            return ResponseEntity.badRequest().body("인증 코드가 일치하지 않습니다.");
        }
    }
}
