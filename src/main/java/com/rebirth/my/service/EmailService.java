package com.rebirth.my.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    public String sendVerificationCode(String toEmail) {
        String code = createCode();
        // DEBUG: Print code to console
        System.out.println("=========================================");
        System.out.println("Email: " + toEmail);
        System.out.println("Verification Code: " + code);
        System.out.println("=========================================");

        try {
            System.out.println("Sending verification email to: " + toEmail); // Logging
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[Rebirth] 회원가입 인증 코드입니다.");
            helper.setText(buildEmailContent(code), true);

            javaMailSender.send(message);
            System.out.println("Email sent successfully to: " + toEmail); // Logging
            return code;
        } catch (MessagingException e) {
            System.err.println("Failed to send email: " + e.getMessage()); // Logging
            e.printStackTrace();
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }

    public String sendPasswordResetCode(String toEmail) {
        String code = createCode();
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("[Rebirth] 비밀번호 재설정 인증 코드입니다.");
            helper.setText(buildPasswordResetEmailContent(code), true);

            javaMailSender.send(message);
            return code;
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }

    private String createCode() {
        Random random = new Random();
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            key.append(random.nextInt(10));
        }
        return key.toString();
    }

    private String buildEmailContent(String code) {
        return "<div style='margin:20px;'>" +
                "<h1>Rebirth 회원가입 인증 코드</h1>" +
                "<br>" +
                "<p>아래 코드를 복사하여 입력해주세요.</p>" +
                "<br>" +
                "<div style='font-size:130%'>" +
                "CODE : <strong>" + code + "</strong>" +
                "</div>" +
                "<br>" +
                "<p>감사합니다.</p>" +
                "</div>";
    }

    private String buildPasswordResetEmailContent(String code) {
        return "<div style='margin:20px;'>" +
                "<h1>Rebirth 비밀번호 재설정</h1>" +
                "<br>" +
                "<p>아래 코드를 입력하여 비밀번호를 재설정하세요.</p>" +
                "<br>" +
                "<div style='font-size:130%'>" +
                "CODE : <strong>" + code + "</strong>" +
                "</div>" +
                "<br>" +
                "<p>요청하지 않았다면 이 메일을 무시하세요.</p>" +
                "</div>";
    }
}
