package com.rebirth.my.chat.component;

import lombok.Data;

/**
 * 날씨 정보 DTO
 */
@Data
public class WeatherInfo {
    private double temperature; // 기온 (°C)
    private String sky; // 하늘 상태 (맑음/구름많음/흐림)
    private String precipitation; // 강수 형태 (없음/비/비/눈/눈)
    private String description; // 종합 설명
    private int nx; // 격자 X좌표
    private int ny; // 격자 Y좌표

    /**
     * 날씨 종합 설명 생성
     */
    public void generateDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(sky);

        if (!"없음".equals(precipitation)) {
            sb.append(", ").append(precipitation).append(" 예보");
        }

        sb.append(", ").append((int) temperature).append("°C");
        this.description = sb.toString();
    }
}
