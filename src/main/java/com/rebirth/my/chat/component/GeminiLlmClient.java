package com.rebirth.my.chat.component;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
// @Primary - Disable Gemini as default, switched to Groq
public class GeminiLlmClient implements LlmClient, InitializingBean {

    @Value("${google.gemini.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // Strategy configuration class
    static class ApiConfig {
        String version; // v1 or v1beta
        String modelName;

        ApiConfig(String version, String modelName) {
            this.version = version;
            this.modelName = modelName;
        }

        @Override
        public String toString() {
            return version + "/" + modelName;
        }
    }

    private static final ApiConfig[] TARGET_CONFIGS = {
            new ApiConfig("v1beta", "gemini-1.5-flash"), // 1. Stable & Fast
            new ApiConfig("v1beta", "gemini-1.5-pro"), // 2. High Intelligence
            new ApiConfig("v1beta", "gemini-1.0-pro") // 3. Legacy Backup
    };

    private ApiConfig workingConfig = null;

    @Override
    public void afterPropertiesSet() {
        System.out.println("====== GeminiLlmClient Initialized (2025 Update) ======");
        System.out.println("API Key Present: " + (apiKey != null && !apiKey.isEmpty()));
    }

    @Override
    public String generateResponse(String systemPrompt, String userMessage, String context) {
        // Create Request Entity
        HttpEntity<Map<String, Object>> entity = createRequestEntity(systemPrompt, userMessage, context);

        // 1. Try Cached Config
        if (workingConfig != null) {
            try {
                return executeRequest(workingConfig, entity);
            } catch (Exception e) {
                System.out.println("Cached config " + workingConfig + " failed. Resetting fallback.");
                workingConfig = null;
            }
        }

        // 2. Fallback Loop
        StringBuilder errorLog = new StringBuilder();
        for (ApiConfig config : TARGET_CONFIGS) {
            try {
                System.out.println("Trying Gemini Config: " + config);
                String result = executeRequest(config, entity);

                // Success!
                System.out.println("SUCCESS! Connected to " + config);
                workingConfig = config;
                return result;

            } catch (Exception e) {
                String msg = String.format("[%s] %s", config, e.getMessage());
                System.err.println(msg);
                errorLog.append(msg).append("\n");

                // Check specifically for 429 (Rate Limit) to trigger special fallback later
                if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                    if (((org.springframework.web.client.HttpClientErrorException) e).getStatusCode().value() == 429) {
                        System.err.println("Gemini 429 detected on " + config + ". Will try next model or fallback.");
                    }
                }
            }
        }

        // 3. All Failed -> Check if we need Rule-Based Fallback (Smart Mode)
        // If we reached here, it means ALL models failed (including 429s).
        System.err.println("All Gemini models failed. Switching to Rule-Based Fallback.");

        // 1. Critical Data Queries - Return "죄송합니다" to trigger ChatbotService's DB
        // lookup
        if ((userMessage.contains("에코") && (userMessage.contains("포인트") || userMessage.contains("점수")))
                || (userMessage.contains("옷장") && (userMessage.contains("몇")
                        || userMessage.contains("개") || userMessage.contains("목록")
                        || userMessage.contains("뭐") || userMessage.contains("있어") || userMessage.contains("보여줘")))) {
            return "죄송합니다. 데이터 시스템 연결을 시도합니다.";
        }

        // 2. Small Talk & Safety Responses
        if (userMessage.contains("확실해") || userMessage.contains("진짜")) {
            return "네, 확실합니다! 고객님의 정보는 시스템에서 실시간으로 조회한 정확한 데이터입니다. 믿으셔도 됩니다.";
        } else if (userMessage.contains("안녕") || userMessage.contains("반가")
                || userMessage.toLowerCase().contains("hello")) {
            return "안녕하세요! Re:birth 패션 AI입니다. 오늘도 스타일리시한 하루 되세요!";
        } else if (userMessage.contains("뭐해") || userMessage.contains("바빠")) {
            return "고객님의 옷장을 분석하고 가장 멋진 코디를 고민하고 있었어요. ";
        } else if (userMessage.contains("사랑") || userMessage.contains("좋아")) {
            return "어머, 감사합니다! 저도 고객님과 함께해서 행복해요. ";
        } else if (userMessage.contains("심심") || userMessage.contains("놀아")) {
            return "저랑 패션 밸런스 게임 어때요? '평생 패딩 입기' vs '평생 반팔 입기' 골라보세요! ";
        } else if (userMessage.contains("배고파") || userMessage.contains("메뉴")) {
            return "식사 메뉴 고르는 건 옷 고르는 것만큼 어렵죠. 오늘은 가벼운 샐러드 어떠세요? ";
        } else if (userMessage.contains("추천") || userMessage.contains("코디")) {
            return "지금 패션 AI의 영감이 잠시 충전 중입니다. \n대신 '내 옷장'을 열어보시면 잊고 있던 멋진 옷을 발견하실지도 몰라요!";
        } else if (userMessage.contains("방법") || userMessage.contains("어떻게")) {
            return "죄송합니다. 상세한 안내는 잠시 후 다시 시도해주세요. 궁금한 점은 언제든 물어봐주세요!";
        }

        // 3.5 Site Info Fallback (When LLM fails)
        if (userMessage.contains("사이트") || userMessage.contains("리버스") || userMessage.contains("누구")
                || userMessage.contains("뭐야")) {
            return "Re:birth는 안 입는 옷을 업사이클링하고, 나만의 디지털 옷장을 관리하며 탄소 중립을 실천하는 지속 가능한 패션 플랫폼입니다.";
        }

        // 4. Detailed Error Report (Last Resort)
        // Log errors to server console only, do NOT show to user
        if (errorLog.length() > 0) {
            System.err.println("Gemini API All Failed:\n" + errorLog.toString());
        }

        // Friendly Fallback Message for User
        return "죄송합니다. 현재 질문에 대해서는 답변을 드리기가 어렵습니다. \n다른 질문을 해주시면 답변 도와드리겠습니다.";
    }

    private String executeRequest(ApiConfig config, HttpEntity<Map<String, Object>> entity) {
        // Build URI manually to prevent encoding issues with ':'
        String urlStr = String.format("https://generativelanguage.googleapis.com/%s/models/%s:generateContent?key=%s",
                config.version, config.modelName, apiKey);

        URI uri = URI.create(urlStr);
        ResponseEntity<Map> response = restTemplate.postForEntity(uri, entity, Map.class);
        return extractTextFromResponse(response.getBody());
    }

    private String fetchAvailableModelsDiagnostics() {
        try {
            String urlStr = String.format("https://generativelanguage.googleapis.com/v1beta/models?key=%s", apiKey);
            ResponseEntity<Map> response = restTemplate.getForEntity(URI.create(urlStr), Map.class);
            Map body = response.getBody();
            if (body != null && body.containsKey("models")) {
                List<Map> models = (List<Map>) body.get("models");
                StringBuilder sb = new StringBuilder();
                for (Map m : models) {
                    Object name = m.get("name");
                    sb.append("- ").append(name).append("\n");
                }
                return sb.toString();
            }
        } catch (Exception e) {
            return "목록 조회 실패: " + e.getMessage();
        }
        return "응답 없음";
    }

    private HttpEntity<Map<String, Object>> createRequestEntity(String systemPrompt, String userMessage,
            String context) {
        String finalPrompt = String.format(
                """
                        <System>
                        %s
                        </System>

                        <Context>
                        %s
                        </Context>

                        <User>
                        %s
                        </User>

                        IMPORTANT: If you need to use a tool/function based on the user request, response ONLY with the exact format: CALL: functionName:argument (if needed) or CALL: functionName
                        Available Functions:
                        - recommendOutfit (requires weather info implied or collected)
                        - getWardrobeSummary (userId implied)
                        - getEcoPoints (userId implied)
                        - searchStyle (keyword implied from user mood/request e.g. 'dating look', 'hip style', 'formal')

                        User question: %s
                        """,
                systemPrompt, context, userMessage, userMessage);

        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> contentPart = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> textPart = new HashMap<>();

        textPart.put("text", finalPrompt);
        parts.add(textPart);
        contentPart.put("parts", parts);
        contents.add(contentPart);

        requestBody.put("contents", contents);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return new HttpEntity<>(requestBody, headers);
    }

    private String extractTextFromResponse(Map responseBody) {
        if (responseBody == null)
            return "";
        try {
            List<Map> candidates = (List<Map>) responseBody.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map content = (Map) candidates.get(0).get("content");
                List<Map> parts = (List<Map>) content.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    return (String) parts.get(0).get("text");
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse Gemini response: " + responseBody);
        }
        return "답변을 생성하지 못했습니다.";
    }
}
