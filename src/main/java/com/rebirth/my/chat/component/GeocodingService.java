package com.rebirth.my.chat.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 주소를 위도/경도로 변환하는 서비스 (Kakao Geocoding API)
 * 
 * 3단계 Fallback 전략:
 * 1. 정제된 주소로 주소 검색 API 시도
 * 2. 원본 주소로 키워드 검색 API 시도
 * 3. 실패 시 null 반환 (GridConverter에서 도시명 기반 기본좌표 사용)
 */
@Component
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);

    @Value("${kakao.rest-api-key}")
    private String kakaoApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // 주소별 좌표 캐시 (영구 보관 - 주소는 변하지 않음)
    private final Map<String, double[]> cache = new ConcurrentHashMap<>();

    // 주소 검색 API
    private static final String ADDRESS_SEARCH_URL = "https://dapi.kakao.com/v2/local/search/address.json?query=";
    // 키워드 검색 API (건물명 인식 가능)
    private static final String KEYWORD_SEARCH_URL = "https://dapi.kakao.com/v2/local/search/keyword.json?query=";

    /**
     * 주소를 위도/경도로 변환 (3단계 Fallback 전략)
     * 
     * @param address 한국 주소 (예: "서울시 강남구 테헤란로 123 역삼빌딩 5층")
     * @return double[]{위도, 경도} 또는 null
     */
    public double[] getCoordinates(String address) {
        if (address == null || address.trim().isEmpty()) {
            log.debug("[Geocoding] 주소가 비어있음 - 스킵");
            return null;
        }

        // 캐시 확인
        if (cache.containsKey(address)) {
            log.debug("[Geocoding] 캐시 히트: {}", address);
            return cache.get(address);
        }

        log.debug("[Geocoding] 변환 시작: {}", address);

        // 1단계: 정제된 주소로 주소 검색 API
        String cleanedAddress = cleanAddress(address);
        double[] coords = searchByAddress(cleanedAddress);

        if (coords != null) {
            cache.put(address, coords);
            log.info("[Geocoding] 1단계 성공 (주소 검색): {}", cleanedAddress);
            return coords;
        }

        log.debug("[Geocoding] 1단계 실패, 2단계 시도...");

        // 2단계: 정제된 주소로 키워드 검색 API (건물명 인식)
        coords = searchByKeyword(cleanedAddress);

        if (coords != null) {
            cache.put(address, coords);
            log.info("[Geocoding] 2단계 성공 (키워드 검색): {}", cleanedAddress);
            return coords;
        }

        // 3단계: 모두 실패 - null 반환 (GridConverter에서 fallback 처리)
        log.warn("[Geocoding] 모든 단계 실패, 기본값 사용: {}", address);
        return null;
    }

    /**
     * 주소 정제 - 건물명, 층수, 호수 등 불필요한 정보 제거
     * 
     * 예: "서울시 강남구 테헤란로 123 역삼빌딩 5층" → "서울시 강남구 테헤란로 123"
     * 예: "대전 서구 도마동 333-9 아트빌 204호" → "대전광역시 서구 도마동 333-9"
     */
    private String cleanAddress(String address) {
        String cleaned = address;

        // 우편번호 제거 (예: "(12345)", "(123-456)")
        cleaned = cleaned.replaceAll("^\\s*\\(\\d{5}\\)\\s*", "");
        cleaned = cleaned.replaceAll("^\\s*\\(\\d{3}-\\d{3}\\)\\s*", "");

        // 도시명 정규화 (Kakao API 인식률 향상)
        // 줄임말 → 정식 명칭
        cleaned = cleaned.replaceAll("^서울\\s", "서울특별시 ");
        cleaned = cleaned.replaceAll("^부산\\s", "부산광역시 ");
        cleaned = cleaned.replaceAll("^대구\\s", "대구광역시 ");
        cleaned = cleaned.replaceAll("^인천\\s", "인천광역시 ");
        cleaned = cleaned.replaceAll("^광주\\s", "광주광역시 ");
        cleaned = cleaned.replaceAll("^대전\\s", "대전광역시 ");
        cleaned = cleaned.replaceAll("^울산\\s", "울산광역시 ");
        cleaned = cleaned.replaceAll("^세종\\s", "세종특별자치시 ");
        cleaned = cleaned.replaceAll("^제주\\s", "제주특별자치도 ");

        // 층수 정보 제거 (예: "5층", "B1층", "지하1층")
        cleaned = cleaned.replaceAll("\\s*(지하)?\\s*[B]?\\d+층.*$", "");

        // 호수 정보 제거 (예: "204호", "101-202호")
        cleaned = cleaned.replaceAll("\\s+\\d+[-]?\\d*호.*$", "");

        // 건물명 패턴 제거 (빌딩, 빌, 타워, 오피스텔 등)
        // 예: "역삼빌딩", "아트빌", "OO타워"
        cleaned = cleaned.replaceAll(
                "(\\d+[-]?\\d*)\\s+[가-힣A-Za-z]+(?:빌딩|빌|타워|오피스|센터|아파트|오피스텔|상가|프라자|몰|팰리스|파크|스퀘어|플라자|하우스|코아|맨션|빌라|주택).*$",
                "$1");

        // 상세주소 패턴 제거 (예: "101동 202호", "가동 301호", "A동")
        cleaned = cleaned.replaceAll("\\s+[가-힣A-Za-z]?\\d*동\\s*\\d*호?.*$", "");

        // 괄호 안 내용 제거 (예: "(역삼역 3번출구)")
        cleaned = cleaned.replaceAll("\\s*\\([^)]*\\)\\s*", " ");

        // 연속 공백 정리
        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        if (!cleaned.equals(address)) {
            log.debug("[Geocoding] 주소 정제: '{}' → '{}'", address, cleaned);
        } else {
            log.debug("[Geocoding] 주소 정제 없음 (원본 사용): {}", address);
        }

        return cleaned;
    }

    /**
     * Kakao 주소 검색 API 호출
     */
    private double[] searchByAddress(String address) {
        try {
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = ADDRESS_SEARCH_URL + encodedAddress;

            log.debug("[Geocoding] 주소 검색 API 호출: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            log.debug("[Geocoding] 응답 상태: {}", response.getStatusCode());
            log.debug("[Geocoding] 응답 본문: {}", response.getBody());

            if (response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> documents = (List<Map<String, Object>>) response.getBody().get("documents");

                log.debug("[Geocoding] documents 수: {}", documents != null ? documents.size() : "null");

                if (documents != null && !documents.isEmpty()) {
                    Map<String, Object> first = documents.get(0);
                    double lon = Double.parseDouble((String) first.get("x"));
                    double lat = Double.parseDouble((String) first.get("y"));

                    System.out.println("📍 [Geocoding] 주소 검색 결과: lat=" + lat + ", lon=" + lon);
                    return new double[] { lat, lon };
                }
            }
        } catch (Exception e) {
            System.err.println("❌ [Geocoding] 주소 검색 실패: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Kakao 키워드 검색 API 호출 (건물명, 장소명 인식 가능)
     */
    private double[] searchByKeyword(String keyword) {
        try {
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = KEYWORD_SEARCH_URL + encodedKeyword;

            System.out.println("📍 [Geocoding] 키워드 검색 API 호출: " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            System.out.println("📍 [Geocoding] 키워드 응답 상태: " + response.getStatusCode());
            System.out.println("📍 [Geocoding] 키워드 응답: " + response.getBody());

            if (response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> documents = (List<Map<String, Object>>) response.getBody().get("documents");

                System.out
                        .println("📍 [Geocoding] 키워드 documents 수: " + (documents != null ? documents.size() : "null"));

                if (documents != null && !documents.isEmpty()) {
                    Map<String, Object> first = documents.get(0);
                    // 키워드 검색은 x, y가 String이 아닌 경우도 있음
                    double lon = parseDouble(first.get("x"));
                    double lat = parseDouble(first.get("y"));

                    String placeName = (String) first.get("place_name");
                    System.out.println("📍 [Geocoding] 키워드 검색 결과: " + placeName + " → lat=" + lat + ", lon=" + lon);
                    return new double[] { lat, lon };
                }
            }
        } catch (Exception e) {
            System.err.println("❌ [Geocoding] 키워드 검색 실패: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Object를 double로 변환 (String 또는 Number 처리)
     */
    private double parseDouble(Object value) {
        if (value instanceof String) {
            return Double.parseDouble((String) value);
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        throw new IllegalArgumentException("Cannot parse to double: " + value);
    }

    /**
     * 캐시 초기화 (테스트용)
     */
    public void clearCache() {
        cache.clear();
    }
}
