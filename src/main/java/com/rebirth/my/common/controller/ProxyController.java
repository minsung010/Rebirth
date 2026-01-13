package com.rebirth.my.common.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class ProxyController {

    private final RestTemplate restTemplate;

    public ProxyController() {
        this.restTemplate = new RestTemplate();
    }

    @GetMapping("/api/proxy/image")
    public ResponseEntity<byte[]> proxyImage(@RequestParam("url") String imageUrl) {
        try {
            // URL 검증 - 절대 URL인지 확인
            if (imageUrl == null || imageUrl.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // 상대 경로인 경우 (내부 이미지) - 프록시 불필요
            if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                // 로컬 이미지는 직접 접근하도록 리다이렉트
                return ResponseEntity.status(302)
                        .header("Location", imageUrl)
                        .build();
            }

            // 외부 이미지 다운로드
            byte[] imageBytes = restTemplate.getForObject(imageUrl, byte[].class);

            if (imageBytes == null) {
                return ResponseEntity.notFound().build();
            }

            // 이미지 타입 추론
            MediaType contentType = MediaType.IMAGE_JPEG;
            String lowerUrl = imageUrl.toLowerCase();
            if (lowerUrl.endsWith(".png")) {
                contentType = MediaType.IMAGE_PNG;
            } else if (lowerUrl.endsWith(".gif")) {
                contentType = MediaType.IMAGE_GIF;
            } else if (lowerUrl.endsWith(".webp")) {
                contentType = MediaType.parseMediaType("image/webp");
            }

            return ResponseEntity.ok()
                    .contentType(contentType)
                    .body(imageBytes);

        } catch (Exception e) {
            System.err.println("이미지 프록시 실패: " + imageUrl + " - " + e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
