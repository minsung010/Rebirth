package com.rebirth.my.chat.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 기상청 단기예보 API를 사용하여 날씨 정보를 조회하는 서비스
 */
@Component
public class WeatherService {

    @Value("${kma.api-key}")
    private String apiKey;

    @Autowired
    private GeocodingService geocodingService;

    @Autowired
    private GridConverter gridConverter;

    private final RestTemplate restTemplate = new RestTemplate();

    // 캐시 (1시간)
    private WeatherInfo cachedWeather;
    private String cachedAddress;
    private long cacheTimestamp;

    /**
     * 주소를 기반으로 현재 날씨 정보 조회
     */
    public WeatherInfo getWeatherByAddress(String address) {
        // 캐시 확인 (1시간 이내 + 같은 주소)
        if (isCacheValid(address)) {
            System.out.println("🌤️ [Weather] 캐시 히트");
            return cachedWeather;
        }

        try {
            int[] grid;

            // 1. 주소 → 좌표 → 격자 변환
            if (address != null && !address.isEmpty()) {
                double[] coords = geocodingService.getCoordinates(address);
                if (coords != null) {
                    grid = gridConverter.toGrid(coords[0], coords[1]);
                } else {
                    // Geocoding 실패 시 주소에서 도시명 추출해서 기본값 사용
                    grid = gridConverter.getDefaultGrid(address);
                }
            } else {
                // 주소 없으면 서울 기본값
                grid = gridConverter.getDefaultGrid(null);
            }

            // 2. 기상청 API 호출
            cachedWeather = fetchWeather(grid[0], grid[1]);
            cachedAddress = address;
            cacheTimestamp = System.currentTimeMillis();

            return cachedWeather;

        } catch (Exception e) {
            System.err.println("❌ [Weather] 조회 실패: " + e.getMessage());
            e.printStackTrace();
            return getFallbackWeather();
        }
    }

    /**
     * 기상청 API 호출
     */
    private WeatherInfo fetchWeather(int nx, int ny) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        String baseDate = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = getBaseTime(now.getHour());

        // 자정~2시 사이는 전날 23시 발표분 사용
        if (now.getHour() < 2) {
            baseDate = today.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            baseTime = "2300";
        }

        // URL 문자열 생성 (이미 인코딩된 키 사용)
        String urlStr = String.format(
                "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst" +
                        "?serviceKey=%s&numOfRows=50&pageNo=1&dataType=JSON" +
                        "&base_date=%s&base_time=%s&nx=%d&ny=%d",
                apiKey, baseDate, baseTime, nx, ny);

        System.out.println(
                "🌤️ [Weather] API 호출: nx=" + nx + ", ny=" + ny + ", baseDate=" + baseDate + ", baseTime=" + baseTime);

        // URI 객체로 변환하여 RestTemplate의 자동 인코딩 방지
        java.net.URI uri = java.net.URI.create(urlStr);
        Map response = restTemplate.getForObject(uri, Map.class);
        return parseWeatherResponse(response, nx, ny);
    }

    /**
     * 기상청 발표 시각 계산 (02, 05, 08, 11, 14, 17, 20, 23시)
     */
    private String getBaseTime(int hour) {
        int[] baseTimes = { 23, 20, 17, 14, 11, 8, 5, 2 };
        for (int bt : baseTimes) {
            if (hour >= bt)
                return String.format("%02d00", bt);
        }
        return "2300";
    }

    /**
     * 기상청 응답 파싱
     */
    @SuppressWarnings("unchecked")
    private WeatherInfo parseWeatherResponse(Map response, int nx, int ny) {
        WeatherInfo info = new WeatherInfo();
        info.setNx(nx);
        info.setNy(ny);
        info.setSky("맑음");
        info.setPrecipitation("없음");
        info.setTemperature(15); // 기본값

        try {
            Map body = (Map) ((Map) response.get("response")).get("body");
            Map items = (Map) body.get("items");
            List<Map> itemList = (List<Map>) items.get("item");

            for (Map item : itemList) {
                String category = (String) item.get("category");
                String value = (String) item.get("fcstValue");

                switch (category) {
                    case "TMP": // 기온
                        info.setTemperature(Double.parseDouble(value));
                        break;
                    case "SKY": // 하늘 상태
                        info.setSky(parseSky(value));
                        break;
                    case "PTY": // 강수 형태
                        info.setPrecipitation(parsePrecipitation(value));
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ [Weather] 파싱 실패: " + e.getMessage());
        }

        info.generateDescription();
        System.out.println("🌤️ [Weather] 결과: " + info.getDescription());
        return info;
    }

    private String parseSky(String value) {
        return switch (value) {
            case "1" -> "맑음";
            case "3" -> "구름많음";
            case "4" -> "흐림";
            default -> "맑음";
        };
    }

    private String parsePrecipitation(String value) {
        return switch (value) {
            case "1" -> "비";
            case "2" -> "비/눈";
            case "3" -> "눈";
            case "4" -> "소나기";
            default -> "없음";
        };
    }

    /**
     * 현재 계절 판단 (기온 기반)
     */
    public String getCurrentSeason(String address) {
        WeatherInfo weather = getWeatherByAddress(address);
        return mapTempToSeason(weather.getTemperature());
    }

    private String mapTempToSeason(double temp) {
        if (temp >= 24)
            return "여름";
        if (temp >= 15)
            return "봄,가을";
        if (temp >= 5)
            return "봄,가을,겨울";
        return "겨울";
    }

    private boolean isCacheValid(String address) {
        if (cachedWeather == null)
            return false;
        if (cachedAddress == null || !cachedAddress.equals(address))
            return false;
        return (System.currentTimeMillis() - cacheTimestamp) < 3600000; // 1시간
    }

    private WeatherInfo getFallbackWeather() {
        WeatherInfo info = new WeatherInfo();

        // 월 기반으로 기온 추정 (서울 기준 평균 기온)
        int month = java.time.LocalDate.now().getMonthValue();
        double estimatedTemp;
        String sky;

        switch (month) {
            case 12, 1, 2 -> {
                estimatedTemp = -2;
                sky = "맑음";
            } // 겨울
            case 3, 4 -> {
                estimatedTemp = 12;
                sky = "맑음";
            } // 봄
            case 5, 6 -> {
                estimatedTemp = 22;
                sky = "맑음";
            } // 초여름
            case 7, 8 -> {
                estimatedTemp = 28;
                sky = "구름많음";
            } // 여름
            case 9, 10 -> {
                estimatedTemp = 18;
                sky = "맑음";
            } // 가을
            case 11 -> {
                estimatedTemp = 8;
                sky = "맑음";
            } // 늦가을
            default -> {
                estimatedTemp = 15;
                sky = "맑음";
            }
        }

        info.setTemperature(estimatedTemp);
        info.setSky(sky);
        info.setPrecipitation("없음");
        info.setNx(60);
        info.setNy(127);
        info.generateDescription();

        System.out.println("🌤️ [Weather] Fallback 사용: " + month + "월, 추정 기온=" + estimatedTemp + "°C");
        return info;
    }

    /**
     * 캐시된 날씨 정보 반환 (이미 조회된 경우)
     */
    public WeatherInfo getCachedWeather() {
        return cachedWeather;
    }

    /**
     * 특정 시간대 날씨 예보 조회 (외출 시간 기반)
     * 
     * @param address    사용자 주소
     * @param targetHour 외출 예정 시간 (0-23)
     * @return 해당 시간대 날씨 설명 문자열
     */
    public String getWeatherByTime(String address, int targetHour) {
        try {
            // 현재 날씨 조회 (캐시 활용)
            WeatherInfo currentWeather = getWeatherByAddress(address);

            // 현재 시간
            int currentHour = java.time.LocalTime.now().getHour();

            // 시간 차이 계산
            int hourDiff = targetHour - currentHour;
            if (hourDiff < 0)
                hourDiff += 24; // 다음날

            // 시간에 따른 기온 변화 예측 (간단한 모델)
            double tempChange = 0;
            if (targetHour >= 6 && targetHour <= 14) {
                // 오전~점심: 기온 상승
                tempChange = Math.min(hourDiff * 0.5, 5);
            } else if (targetHour >= 15 && targetHour <= 20) {
                // 오후: 약간 하강
                tempChange = -Math.min(hourDiff * 0.3, 3);
            } else {
                // 밤: 기온 하강
                tempChange = -Math.min(hourDiff * 0.5, 5);
            }

            double estimatedTemp = currentWeather.getTemperature() + tempChange;
            String timeLabel = String.format("%02d시", targetHour);
            String sky = currentWeather.getSky();
            String precipitation = currentWeather.getPrecipitation();

            // 결과 문자열 생성
            StringBuilder result = new StringBuilder();
            result.append(String.format("%s 예상 날씨: ", timeLabel));
            result.append(String.format("%.0f°C, %s", estimatedTemp, sky));
            if (!"없음".equals(precipitation)) {
                result.append(", ").append(precipitation);
            }

            // 옷차림 조언 추가
            if (estimatedTemp < 5) {
                result.append(" (두꺼운 외투 필수!)");
            } else if (estimatedTemp < 12) {
                result.append(" (가디건이나 자켓 추천)");
            } else if (estimatedTemp > 28) {
                result.append(" (시원한 옷 추천)");
            }

            System.out.println("🌤️ [Weather] 시간별 예보: " + result);
            return result.toString();

        } catch (Exception e) {
            System.err.println("❌ [Weather] 시간별 조회 실패: " + e.getMessage());
            return String.format("%02d시 날씨를 조회할 수 없습니다.", targetHour);
        }
    }
}
