package com.rebirth.my.chat.component;

import org.springframework.stereotype.Component;

/**
 * 위도/경도를 기상청 격자좌표(nx, ny)로 변환하는 유틸리티
 * 기상청 공식 변환 알고리즘 적용
 */
@Component
public class GridConverter {

    // 기상청 격자 변환 상수
    private static final double RE = 6371.00877; // 지구 반경(km)
    private static final double GRID = 5.0; // 격자 간격(km)
    private static final double SLAT1 = 30.0; // 표준위도1
    private static final double SLAT2 = 60.0; // 표준위도2
    private static final double OLON = 126.0; // 기준점 경도
    private static final double OLAT = 38.0; // 기준점 위도
    private static final double XO = 43; // 기준점 X좌표
    private static final double YO = 136; // 기준점 Y좌표

    /**
     * 위도/경도를 기상청 격자좌표로 변환
     * 
     * @param lat 위도 (예: 36.35)
     * @param lon 경도 (예: 127.38)
     * @return int[]{nx, ny} 격자좌표
     */
    public int[] toGrid(double lat, double lon) {
        double DEGRAD = Math.PI / 180.0;
        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5)
                / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);

        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;

        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + lat * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);

        double theta = lon * DEGRAD - olon;
        if (theta > Math.PI)
            theta -= 2.0 * Math.PI;
        if (theta < -Math.PI)
            theta += 2.0 * Math.PI;
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

        System.out.println("🗺️ [GridConverter] lat=" + lat + ", lon=" + lon + " → nx=" + nx + ", ny=" + ny);
        return new int[] { nx, ny };
    }

    /**
     * 주요 도시/구/동의 기본 격자좌표 (Fallback용)
     * 더 상세한 지역명이 있으면 우선 매칭
     */
    public int[] getDefaultGrid(String address) {
        if (address == null)
            return new int[] { 60, 127 }; // 서울 기본값

        String addr = address.trim();

        // ===== 대전광역시 구/동 단위 =====
        // 서구
        if (addr.contains("둔산동") || addr.contains("둔산"))
            return new int[] { 67, 100 }; // 둔산동
        if (addr.contains("도마동") || addr.contains("도마"))
            return new int[] { 67, 100 }; // 도마동
        if (addr.contains("월평동") || addr.contains("월평"))
            return new int[] { 67, 100 }; // 월평동
        if (addr.contains("탄방동") || addr.contains("탄방"))
            return new int[] { 67, 100 }; // 탄방동
        if (addr.contains("용문동") || addr.contains("용문"))
            return new int[] { 67, 100 }; // 용문동
        if (addr.contains("대전 서구") || addr.contains("대전서구"))
            return new int[] { 67, 100 };

        // 중구
        if (addr.contains("은행동") || addr.contains("대흥동") || addr.contains("선화동"))
            return new int[] { 68, 100 };
        if (addr.contains("대전 중구") || addr.contains("대전중구"))
            return new int[] { 68, 100 };

        // 동구
        if (addr.contains("판암동") || addr.contains("신흥동") || addr.contains("대동"))
            return new int[] { 68, 99 };
        if (addr.contains("대전 동구") || addr.contains("대전동구"))
            return new int[] { 68, 99 };

        // 유성구
        if (addr.contains("봉명동") || addr.contains("궁동") || addr.contains("어은동"))
            return new int[] { 67, 101 };
        if (addr.contains("대전 유성구") || addr.contains("대전유성구") || addr.contains("유성"))
            return new int[] { 67, 101 };

        // 대덕구
        if (addr.contains("신탄진") || addr.contains("오정동") || addr.contains("법동"))
            return new int[] { 68, 102 };
        if (addr.contains("대전 대덕구") || addr.contains("대전대덕구"))
            return new int[] { 68, 102 };

        // ===== 광역시/도 단위 =====
        if (addr.contains("서울"))
            return new int[] { 60, 127 };
        if (addr.contains("부산"))
            return new int[] { 98, 76 };
        if (addr.contains("대전"))
            return new int[] { 67, 100 };
        if (addr.contains("대구"))
            return new int[] { 89, 90 };
        if (addr.contains("인천"))
            return new int[] { 55, 124 };
        if (addr.contains("광주"))
            return new int[] { 58, 74 };
        if (addr.contains("울산"))
            return new int[] { 102, 84 };
        if (addr.contains("세종"))
            return new int[] { 66, 103 };
        if (addr.contains("수원"))
            return new int[] { 60, 121 };
        if (addr.contains("전주"))
            return new int[] { 63, 89 };
        if (addr.contains("청주"))
            return new int[] { 69, 107 };
        if (addr.contains("제주"))
            return new int[] { 52, 38 };

        return new int[] { 60, 127 }; // 서울 기본값
    }
}
