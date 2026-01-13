package com.rebirth.my.chat.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Primary
public class GroqLlmClient implements LlmClient {

    @Value("${groq.api-key}")
    private String apiKey;

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    // Using Llama 3.3 70B (Versatile) - Latest Stable as of late 2024/2025
    private static final String MODEL_NAME = "llama-3.3-70b-versatile";

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String generateResponse(String systemPrompt, String userMessage, String context) {
        try {
            // 1. Prepare Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // 2. Prepare Body (OpenAI Chat Completion Format)
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODEL_NAME);
            requestBody.put("temperature", 0.7); // Creative but focused

            List<Map<String, String>> messages = new ArrayList<>();

            // System Message
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");

            // Inject Context into System Prompt
            String finalSystemPrompt = String.format(
                    """
                            %s

                            [Context Information]
                            %s

                            [CRITICAL RULES - MUST FOLLOW]
                            0. **STAY ON TOPIC (최우선)**:
                               - If user says ONLY greeting (안녕, 하이, 반가워 등) → ONLY greet back. "안녕하세요 고객님! 무엇을 도와드릴까요?"
                               - Do NOT add weather info, clothing recommendations, or any extra information unless asked.
                               - ONLY recommend clothes when user explicitly asks: "뭐 입을까?", "추천해줘", "스타일 추천", "외출복", "오늘 입을 옷" etc.
                            1. **ADDRESS**: ALWAYS call user **'고객님'**.
                            2. **NAME IS MANDATORY**:
                               - You MUST use the EXACT **'name'** field from tool results.
                               - Example: If tool returns {"name":"화이트 기본 티셔츠"}, you MUST say "화이트 기본 티셔츠".
                               - **NEVER** output just "상의/Nike/" - this is WRONG.
                               - **ALWAYS** output "상의/Nike/화이트 기본 티셔츠" - this is CORRECT.
                            3. **FORMAT**: **[보유] [Category]/[Brand]/[EXACT NAME]**
                               - Good: **[보유] 상의/Nike/레드 체크 남방**
                               - Bad: **상의/Nike/** (missing name = WRONG)
                               - **IMPORTANT**: Wrap recommended items in **double asterisks** for bold display.
                               - Example: "**[보유] 하의/Adidas/블랙 조거 팬츠**를 추천드려요."
                            4. **TERMINOLOGY**: Use **'상의'** and **'하의'**, not TOP/BOTTOM.
                            5. **CONTEXT LOGIC**:
                               - Formal (Wedding, 소개팅, 면접): Sportswear (Nike/Adidas) = BAD. Say "적절한 옷이 없네요" and recommend general formal item.
                               - Casual (PC Bang, 집콕): Sportswear = GOOD. Recommend owned items.
                            6. **LANGUAGE**: Korean ONLY.
                               - NO foreign words: Chinese, Japanese, Arabic, Vietnamese (thật, rất), Turkish (ayrıca), etc.
                               - If you don't know how to say something in Korean, skip it entirely.
                            7. **WARDROBE LISTING RULE**:
                               - If user asks "뭐 입을까?", "추천해줘" → Do NOT list all items. Just recommend 1-2 items directly.
                               - If user asks "내 옷장에 뭐 있어?", "옷 목록 보여줘" → List all items.
                               - NEVER mention "고객님의 옷장에는 X, Y, Z가 있네요" unless asked.
                            8. **CONCISE RESPONSES**:
                               - Say "[보유] 상의/Nike/레드 체크 남방이 있어요" directly.
                               - Do NOT say "X가 아닌 Y가 있어요" or compare items unnecessarily.
                               - Complete your sentences fully. Never end mid-word.
                               - **ONLY answer what the user asked. Do NOT add unsolicited clothing recommendations or lunch suggestions.**
                               - If user asks about a person, historical event, or general knowledge → Just answer that question. Do NOT recommend clothes.
                               - Only recommend clothes when user explicitly asks: "뭐 입을까?", "추천해줘", "스타일 추천", "외출복" etc.
                            9. **SITE NAVIGATION LINKS**:
                               When user asks to navigate or for page links, use these EXACT paths in markdown format:
                               - AI 분석/등록: [AI 옷 분류/분석](/analysis)
                               - 채팅방: [채팅 바로가기](/chat)
                               - 내 옷장: [나만의 옷장 바로가기](/wardrobe)
                               - 마켓: [Re:Store 바로가기](/market/list)
                               - 판매등록: [판매하기](/market/register)
                               - 기부: [자원 순환 기부](/donation/guide)
                               - 공지사항: [공지사항 확인하기](/community)
                               - 내 정보: [마이페이지](/profile)
                               Example: "옷장을 확인하시려면 [나만의 옷장 바로가기](/wardrobe)로 이동하세요!"
                            10. **GENERAL FASHION RECOMMENDATIONS**:
                               - FIRST: Recommend from user's owned items [보유].
                               - IF no suitable owned items OR user asks for general suggestions:
                                 → Mark as [추천] and provide general fashion advice.
                                 → Example: "고객님 옷장에는 면접에 적합한 옷이 없네요. [추천] 네이비 슬랙스와 화이트 셔츠를 추천드려요!"
                               - Offer to browse Re:Store: "새 옷을 구경하시려면 [Re:Store 바로가기](/market/list)를 확인해보세요!"
                            11. **WEATHER-BASED RECOMMENDATIONS (매우 중요)**:
                               - Context에 weather, temperature, sky, precipitation, currentSeason 정보가 있으면 **반드시** 참고하세요.
                               - 외출 복장 추천 시 먼저 날씨를 언급: "고객님 지역은 오늘 {sky}, {temperature}입니다."
                               - **비(precipitation=비) 예보 시**: 우산, 방수 아우터, 레인부츠 언급
                               - **눈(precipitation=눈) 예보 시**: 따뜻한 아우터, 부츠 추천
                               - **currentSeason=겨울**: 패딩, 코트, 니트 등 보온성 좋은 옷 추천
                               - **currentSeason=여름**: 반팔, 린넨, 시원한 소재 추천
                               - 추천 아이템 목록에는 이미 계절에 맞는 옷만 포함되어 있으니 그 중에서 추천하세요.
                            """,
                    systemPrompt, context);

            systemMsg.put("content", finalSystemPrompt);
            messages.add(systemMsg);

            // User Message
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            requestBody.put("messages", messages);

            // 3. Execute Request
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_API_URL, entity, Map.class);

            // 4. Extract Response
            return extractContent(response.getBody());

        } catch (Exception e) {
            System.err.println("Groq API Error: " + e.getMessage());
            e.printStackTrace();
            return "죄송합니다. AI 시스템 연결 중 오류가 발생했습니다. (Groq API)";
        }
    }

    private String extractContent(Map<String, Object> responseBody) {
        if (responseBody == null) {
            return "오류: 응답이 비어있습니다.";
        }

        try {
            // Traverse Map: choices[0].message.content
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> firstChoice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                if (message != null) {
                    String content = (String) message.get("content");
                    // Critical: Sanitize Content (Remove Hanja, Kana, etc.)
                    return sanitizeContent(content);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse Groq response structure: " + e.getMessage());
        }
        return "죄송합니다. 답변을 생성하지 못했습니다.";
    }

    private String sanitizeContent(String text) {
        if (text == null)
            return "";

        String sanitized = text
                // 1. 불완전한 HTML 태그 제거 (class="..." 패턴 포함)
                .replaceAll("class=\"[^\"]*\"", "")
                .replaceAll("<[^>]*class[^>]*>", "")
                // 2. 독립적인 HTML 속성 제거
                .replaceAll("\\s*(id|style|class|href|src|alt|title)=\"[^\"]*\"\\s*", " ")
                // 3. CJK, Kana, Arabic, Cyrillic
                .replaceAll("[\\u4E00-\\u9FFF\\u3040-\\u309F\\u30A0-\\u30FF\\u0600-\\u06FF\\u0400-\\u04FF]+", "")
                // 4. Thai
                .replaceAll("[\\u0E00-\\u0E7F]+", "")
                // 5. Vietnamese/Turkish 등 문제 단어
                .replaceAll("\\b(khá|thật|rất|ayrıca|için|sehr|très|muito)\\b", "")
                // 6. 베트남어 특수 모음 포함 단어
                .replaceAll("\\b\\w*[àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ]\\w*\\b", "")
                // 7. 여러 공백 정리
                .replaceAll("\\s{2,}", " ");

        return sanitized.trim();
    }
}
