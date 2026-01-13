package com.rebirth.my.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.rebirth.my.auth.CustomOAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final com.rebirth.my.service.EmailService emailService;

    public AuthController(AuthService authService, com.rebirth.my.service.EmailService emailService) {
        this.authService = authService;
        this.emailService = emailService;
    }

    @GetMapping("/login")
    public String login(
            @org.springframework.web.bind.annotation.RequestParam(value = "error", required = false) String error,
            @org.springframework.web.bind.annotation.RequestParam(value = "exception", required = false) String exception,
            Model model) {
        model.addAttribute("pageTitle", "로그인 - Re:birth");
        if (error != null) {
            model.addAttribute("errorMessage", "아이디 또는 비밀번호가 틀립니다.");
        }
        return "auth/login";
    }

    @GetMapping("/join")
    public String join(Model model) {
        model.addAttribute("pageTitle", "회원가입 - Re:birth");
        return "auth/join";
    }

    @PostMapping("/join")
    public String joinProc(JoinRequest joinRequest, jakarta.servlet.http.HttpSession session) {
        authService.join(joinRequest, session);
        return "redirect:/auth/login";
    }

    @GetMapping("/check-id")
    @org.springframework.web.bind.annotation.ResponseBody
    public boolean checkId(@org.springframework.web.bind.annotation.RequestParam("loginId") String loginId) {
        return authService.checkLoginIdDuplicate(loginId);
    }

    @GetMapping("/check-email")
    @org.springframework.web.bind.annotation.ResponseBody
    public boolean checkEmail(@org.springframework.web.bind.annotation.RequestParam("email") String email) {
        return authService.checkEmailDuplicate(email);
    }

    @GetMapping("/social-join")
    public String socialJoinPage(@AuthenticationPrincipal CustomOAuth2User principal, Model model) {
        if (principal == null) {
            return "redirect:/auth/login";
        }
        com.rebirth.my.domain.User user = principal.getUser();
        if (!"PENDING".equals(user.getStatus())) {
            return "redirect:/main";
        }

        // Pre-fill data
        model.addAttribute("name", user.getName());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("pageTitle", "소셜 회원가입 - Re:birth");

        return "auth/social_join";
    }

    @PostMapping("/social-join")
    public String socialJoinProcess(JoinRequest joinRequest,
            @AuthenticationPrincipal CustomOAuth2User principal,
            jakarta.servlet.http.HttpSession session) {
        if (principal == null) {
            return "redirect:/auth/login";
        }

        // AuthService가 최종적으로 유효한(통합된 or 업데이트된) User 객체를 반환하도록 수정됨
        com.rebirth.my.domain.User finalUser = authService.updateSocialUser(principal.getUser().getId(), joinRequest,
                session);

        // Security Context 갱신 (통합된 경우 finalUser는 기존 회원, 아니면 현재 회원)
        CustomUserDetails newUserDetails = new CustomUserDetails(finalUser);
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken newAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                newUserDetails, null, newUserDetails.getAuthorities());

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(newAuth);

        return "redirect:/main";
    }

    // ==========================================
    // Password Recovery Flow
    // ==========================================

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot_password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@org.springframework.web.bind.annotation.RequestParam("email") String email,
            jakarta.servlet.http.HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        if (!authService.isEmailRegistered(email)) {
            redirectAttributes.addFlashAttribute("errorMessage", "등록되지 않은 이메일입니다.");
            return "redirect:/auth/forgot-password";
        }

        try {
            String code = emailService.sendPasswordResetCode(email);
            session.setAttribute("resetEmail", email);
            session.setAttribute("resetCode", code);
            session.setAttribute("resetCodeTime", System.currentTimeMillis());

            return "redirect:/auth/verify-code";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "이메일 발송 중 오류가 발생했습니다.");
            return "redirect:/auth/forgot-password";
        }
    }

    @GetMapping("/verify-code")
    public String verifyCodePage() {
        return "auth/verify_code";
    }

    @PostMapping("/verify-code")
    public String processVerifyCode(@org.springframework.web.bind.annotation.RequestParam("code") String code,
            jakarta.servlet.http.HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        String savedCode = (String) session.getAttribute("resetCode");
        Long sentTime = (Long) session.getAttribute("resetCodeTime");

        if (savedCode == null || sentTime == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "인증 세션이 만료되었습니다. 다시 시도해주세요.");
            return "redirect:/auth/forgot-password";
        }

        if (System.currentTimeMillis() - sentTime > 5 * 60 * 1000) { // 5 minutes validity
            session.removeAttribute("resetCode");
            session.removeAttribute("resetCodeTime");
            redirectAttributes.addFlashAttribute("errorMessage", "인증 시간이 초과되었습니다.");
            return "redirect:/auth/forgot-password";
        }

        if (!savedCode.equals(code)) {
            redirectAttributes.addFlashAttribute("errorMessage", "인증 코드가 일치하지 않습니다.");
            return "redirect:/auth/verify-code";
        }

        session.setAttribute("isVerifiedForReset", true);
        return "redirect:/auth/reset-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(jakarta.servlet.http.HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Boolean isVerified = (Boolean) session.getAttribute("isVerifiedForReset");
        if (isVerified == null || !isVerified) {
            redirectAttributes.addFlashAttribute("errorMessage", "잘못된 접근입니다.");
            return "redirect:/auth/login";
        }
        return "auth/reset_password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(
            @org.springframework.web.bind.annotation.RequestParam("password") String password,
            jakarta.servlet.http.HttpSession session,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        Boolean isVerified = (Boolean) session.getAttribute("isVerifiedForReset");
        String email = (String) session.getAttribute("resetEmail");

        if (isVerified == null || !isVerified || email == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "세션이 만료되었습니다. 처음부터 다시 시도해주세요.");
            return "redirect:/auth/forgot-password";
        }

        try {
            authService.updatePassword(email, password);

            // Cleanup session
            session.removeAttribute("resetEmail");
            session.removeAttribute("resetCode");
            session.removeAttribute("resetCodeTime");
            session.removeAttribute("isVerifiedForReset");

            redirectAttributes.addFlashAttribute("message", "비밀번호가 성공적으로 변경되었습니다.");
            return "redirect:/auth/login";
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "비밀번호 변경 중 오류가 발생했습니다.");
            return "redirect:/auth/reset-password";
        }
    }
}
