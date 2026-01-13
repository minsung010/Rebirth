package com.rebirth.my.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;
import java.io.File;

/**
 * 업로드된 파일(의류 이미지 등)을 정적 리소스로 서빙하기 위한 설정
 */
@Configuration
public class UploadConfig implements WebMvcConfigurer {

    // 🌟 수정: 프로젝트 루트의 uploads/ 폴더 (WardrobeApiController와 동일 경로)
    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads/";

    @PostConstruct
    public void init() {
        // 업로드 디렉토리 생성
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            boolean created = uploadDir.mkdirs();
            if (created) {
                System.out.println("✅ Upload directory created: " + UPLOAD_DIR);
            }
        }

        // wardrobe 서브디렉토리 생성
        File wardrobeDir = new File(UPLOAD_DIR + "wardrobe/");
        if (!wardrobeDir.exists()) {
            wardrobeDir.mkdirs();
        }

        System.out.println("✅ UploadConfig initialized. Serving /uploads/** from " + UPLOAD_DIR);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // /uploads/** URL을 실제 파일 시스템 경로로 매핑
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:///" + UPLOAD_DIR.replace("\\", "/"));
    }
}
