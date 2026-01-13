package com.rebirth.my.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class ProfileConfig implements WebMvcConfigurer {

    // 🌟 ProfileController에 설정된 경로와 동일하게 맞춰주세요.
    private final String UPLOAD_DIR = "C:/profile_upload/"; 

    @PostConstruct
    public void init() {
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (!created) {
                throw new RuntimeException("프로필 업로드 디렉토리 생성 실패: " + UPLOAD_DIR);
            }
        }
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        
        // 1. URL 패턴 정의: "/profile-images/**" 로 들어오는 모든 요청을 처리합니다.
        // 2. 실제 파일 경로 지정: "file:///C:/profile_upload/" 에서 파일을 찾습니다.
        //    (주의: 파일 경로는 'file:///'로 시작하며, Windows 경로 구분자는 슬래시(/)를 사용해야 합니다.)
        registry.addResourceHandler("/profile-images/**")
                .addResourceLocations("file:///" + UPLOAD_DIR);
    }
}