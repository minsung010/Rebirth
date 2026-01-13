package com.rebirth.my.chat.service;

import com.rebirth.my.chat.ChatDao;
import com.rebirth.my.chat.ChatVo;
import com.rebirth.my.chat.component.ContextBuilder;
import com.rebirth.my.chat.component.LlmClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatbotService {

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private ContextBuilder contextBuilder;

    @Autowired
    private ChatDao chatDao;

    // Define System Prompt
    private static final String SYSTEM_PROMPT = """
            당신은 패션 업사이클링 플랫폼 'Re:birth'의 AI 최고의 전문가 어시스턴트입니다.

            [지식 베이스 (Knowledge Base)]
            1. **Re:birth 플랫폼**: 사용자가 안 입는 옷을 등록(디지털 옷장)하고, 업사이클링 팁을 얻거나 탄소 배출 절감에 기여하는 지속 가능한 패션 플랫폼입니다.
            2. **취지 및 역할**: 버려지는 옷을 줄이고의류 순환을 장려합니다. AI 기술을 통해 개인 맞춤형 '디지털 옷장 관리'와 '코디 추천' 서비스를 제공합니다.
            3. **기대 효과 및 영향**: 의류 폐기물 감소를 통한 탄소 중립 실현, 사용자의 지속 가능한 라이프스타일 구축, 패션 산업의 환경적 영향 최소화에 기여합니다.
            4. **기능 안내**: "옷장 분석", "코디 추천", "업사이클링 아이디어"를 제공합니다.

            [상황별 옷차림 가이드 (Dress Code)]
            **중요**: 코디 추천 시 반드시 상황(TPO: Time, Place, Occasion)을 고려하세요!
            - **소개팅/데이트**: 깔끔하고 세련된 캐주얼 (셔츠, 니트, 블라우스, 슬랙스, 청바지 OK, 츄리닝/조거팬츠 ❌)
            - **면접/비즈니스**: 정장, 셔츠, 블라우스, 슬랙스 (캐주얼 ❌)
            - **결혼식/경조사**: 포멀/세미포멀 (청바지 ❌, 흰색 드레스 ❌)
            - **운동/헬스**: 운동복, 레깅스, 조거팬츠 OK
            - **일상/캐주얼**: 자유롭게 추천
            - **파티/클럽**: 화려하고 개성있는 스타일

            **잘못된 추천 예시 (절대 하지 마세요)**:
            - 소개팅에 츄리닝/조거팬츠 추천 ❌
            - 면접에 후드티/운동복 추천 ❌
            - 결혼식에 청바지/운동화 추천 ❌

            [사이트 이동 가이드 (Navigation)]
            사용자가 특정 기능을 찾거나 이동을 원할 경우, 반드시 아래 **마크다운 링크 형식**으로 안내하십시오.
            - **AI 의류 분석 이동**: `[AI 의류 분석 페이지로 이동](/analysis)`
            - **나만의 옷장 이동**: `[나만의 옷장 바로가기](/wardrobe)`
            - **커뮤니티/게시판**: `[커뮤니티 구경하기](/community)`
            - **공지사항**: `[공지사항 확인하기](/community)`
            - **로그인/회원가입**: `[로그인 하러가기](/auth/login)`
            - **채팅목록**: `[채팅목록](/chat/list)`
            - **OOTD**: `[OOTD 구경하기](/ootd/list)`
            - **마이페이지**: `[마이페이지](/profile)`
            - **마켓**: `[마켓 바로가기](/market/list)`
            - **판매등록**: `[판매하기](/market/register)`
            - **기부**: `[자원 순환 기부](/donation/guide)`
            - **폐기/수거**: `[폐기/수거](/analysis/disposal)`

            [핵심 행동 지침]
            1. **언어**: 반드시 '순수 한국어'로만 답변하십시오. **영어 단어를 절대 사용하지 마세요.** 영어 표현 대신 한국어로 바꿔서 말하세요 (예: "좋으시다면" ✅, "good" ❌, "like" ❌).
            2. **스타일**: 답변은 **친근하고 재치 있게**, 그러나 정보는 정확하게 전달하십시오.
            3. **사이트 소개 요청 시**: 위 [지식 베이스]의 내용을 바탕으로 취지, 역할, 기대효과를 요약하여 **3문장 내외**로 설명해주십시오.
            4. **데이터 준수**: 제공된 [Context] 내의 정보(에코포인트, 옷장 내역 등)를 정확히 있는 그대로 사용하십시오.
            5. **캐싱**: 동일한 질문에는 일관된 답변을 제공하십시오.
            6. **TPO 고려**: 코디 추천 시 **반드시** 상황에 맞는 옷차림을 추천하세요.

            질문에 대해 가장 빠르고 정확한 정보를 제공하는 것이 당신의 최우선 임무입니다.
            """;

    // Simple In-Memory Cache for Repeated Queries (Optimization)
    private static final java.util.Map<String, String> RESPONSE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    @Autowired
    private com.rebirth.my.chat.component.FunctionDispatcher functionDispatcher;

    /**
     * Processes a user message and returns the bot's response.
     * 
     * @param userId      The ID of the user sending the message.
     * @param userMessage The text message from the user.
     * @return The bot's response text.
     */
    public String processUserMessage(String userId, String userMessage) {
        // 1. Build Context
        String userContext = contextBuilder.buildUserContext(userId);

        // 2. Find or Create Chat Room
        Long roomId = chatDao.selectBotRoomId(userId);
        if (roomId == null) {
            // Create Logic
            try {
                java.util.Map<String, Object> roomParam = new java.util.HashMap<>();
                roomParam.put("userId", userId);
                chatDao.createBotRoom(roomParam);
                roomId = (Long) roomParam.get("id");

                // Add User
                java.util.Map<String, Object> memberParam1 = new java.util.HashMap<>();
                memberParam1.put("roomId", roomId);
                memberParam1.put("userId", userId);
                chatDao.insertRoomMember(memberParam1);

                // Add Bot (User 0) - This requires User 0 to exist!
                // If User 0 doesn't exist, this will throw FK Error.
                java.util.Map<String, Object> memberParam2 = new java.util.HashMap<>();
                memberParam2.put("roomId", roomId);
                memberParam2.put("userId", "0");
                chatDao.insertRoomMember(memberParam2);

            } catch (Exception e) {
                e.printStackTrace();
                // Return detailed error for debugging
                return "채팅방 생성 오류: " + e.getMessage() + "\n(Tip: DB에 시스템 봇[ID=0] 계정이 있는지 확인해주세요.)";
            }
        }

        Long senderId = Long.parseLong(userId);
        Long botId = 0L;

        // 3. Save User Message
        saveMessageLog(roomId, senderId, "TEXT", userMessage, null);

        // [OPTIMIZATION] Check Cache first to save Quota
        if (RESPONSE_CACHE.containsKey(userMessage)) {
            String cachedResponse = RESPONSE_CACHE.get(userMessage);
            saveMessageLog(roomId, botId, "TEXT", cachedResponse, null);
            return cachedResponse;
        }

        // 3.5 대화 문맥(Conversation History) 조회 - 최근 6개 메시지
        String conversationHistory = "";
        try {
            java.util.Map<String, Object> historyParam = new java.util.HashMap<>();
            historyParam.put("roomId", roomId);
            historyParam.put("limit", 6);
            java.util.List<ChatVo> recentMessages = chatDao.selectRecentMessages(historyParam);

            if (recentMessages != null && !recentMessages.isEmpty()) {
                StringBuilder historyBuilder = new StringBuilder();
                historyBuilder.append("\n[최근 대화 히스토리]\n");
                for (ChatVo msg : recentMessages) {
                    String role = msg.getSenderId().equals(botId) ? "AI" : "사용자";
                    String content = msg.getContent();
                    if (content != null && content.length() > 100) {
                        content = content.substring(0, 100) + "...";
                    }
                    historyBuilder.append(role).append(": ").append(content).append("\n");
                }
                conversationHistory = historyBuilder.toString();
            }
        } catch (Exception e) {
            System.err.println("대화 히스토리 조회 실패: " + e.getMessage());
        }

        // 3.6 특정 질문은 LLM 호출 없이 직접 처리 (정확도 보장)
        String botResponse = null;

        // 판매중인 옷 조회 (LLM 호출 전 직접 처리)
        if ((userMessage.contains("판매") || userMessage.contains("팔고") || userMessage.contains("팔아"))
                && (userMessage.contains("옷") || userMessage.contains("뭐") || userMessage.contains("있")
                        || userMessage.contains("목록"))) {
            java.util.Map<String, Object> args = new java.util.HashMap<>();
            args.put("userId", userId);
            String resultJson = functionDispatcher.dispatch("getItemsForSale", args);

            // Parse count
            java.util.regex.Matcher countMatcher = java.util.regex.Pattern.compile("\"count\"\\s*:\\s*(\\d+)")
                    .matcher(resultJson);
            String count = countMatcher.find() ? countMatcher.group(1) : "0";

            if ("0".equals(count)) {
                botResponse = "현재 고객님이 판매중인 옷이 없습니다. [판매하기](/market/register)에서 옷을 등록해보세요!";
            } else {
                // Parse results
                java.util.regex.Matcher resultMatcher = java.util.regex.Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"")
                        .matcher(resultJson);
                StringBuilder itemList = new StringBuilder();
                while (resultMatcher.find()) {
                    if (itemList.length() > 0)
                        itemList.append(", ");
                    itemList.append("**").append(resultMatcher.group(1)).append("**");
                }
                botResponse = "고객님이 현재 판매중인 옷은 총 **" + count + "벌**입니다: " + itemList.toString() +
                        "\n\n[Re:Store 마켓](/market/list)에서 확인하실 수 있습니다.";
            }
            System.out.println("🏷️ [Direct] 판매중 옷 조회 결과: " + botResponse);
        }

        // OOTD 캘린더 스케줄 조회 (LLM 호출 전 직접 처리)
        if (botResponse == null &&
                (userMessage.contains("일에") || userMessage.contains("일 ") || userMessage.contains("날")
                        || userMessage.contains("언제"))
                &&
                (userMessage.contains("입") || userMessage.contains("뭐") || userMessage.contains("계획")
                        || userMessage.contains("OOTD") || userMessage.contains("코디"))) {

            // 날짜 추출 시도
            String extractedDate = extractDateFromMessage(userMessage);

            if (extractedDate != null) {
                java.util.Map<String, Object> args = new java.util.HashMap<>();
                args.put("userId", userId);
                args.put("date", extractedDate);
                String resultJson = functionDispatcher.dispatch("getOotdSchedule", args);

                // Parse result
                if (resultJson.contains("\"found\": true")) {
                    java.util.regex.Matcher memoMatcher = java.util.regex.Pattern
                            .compile("\"memo\"\\s*:\\s*\"([^\"]+)\"")
                            .matcher(resultJson);
                    String memo = memoMatcher.find() ? memoMatcher.group(1) : "메모 없음";

                    // 날짜 포맷팅
                    String[] dateParts = extractedDate.split("-");
                    String displayDate = dateParts[1] + "월 " + dateParts[2] + "일";

                    botResponse = "📅 고객님은 **" + displayDate + "**에 **" + memo + "** 룩을 계획하셨습니다!\n\n" +
                            "자세한 코디를 확인하시려면 [OOTD 캘린더](/ootd/list)에서 확인해보세요!";
                } else {
                    String[] dateParts = extractedDate.split("-");
                    String displayDate = dateParts[1] + "월 " + dateParts[2] + "일";
                    botResponse = "📅 " + displayDate + "에는 아직 저장된 OOTD가 없습니다.\n\n" +
                            "[피팅룸](/ootd/list)에서 코디를 저장해보세요!";
                }
                System.out.println("📅 [Direct] OOTD 스케줄 조회 결과: " + botResponse);
            }
        }

        // 직접 처리되지 않은 경우에만 LLM 호출
        if (botResponse == null) {
            // 4. Call LLM (with conversation history)
            String enrichedContext = userContext + conversationHistory;
            botResponse = llmClient.generateResponse(SYSTEM_PROMPT, userMessage, enrichedContext);
        }

        // [OPTIMIZATION] Save successful response to Cache (Simple LRU strategy implied
        // by ConcurrentMap for now)
        if (!botResponse.contains("죄송합니다") && !botResponse.contains("오류")) {
            RESPONSE_CACHE.put(userMessage, botResponse);
        }

        // 4.5 Error Handling Logic (Rule-Based Fallback)
        // If LLM failed (Quota or Connection) and returned specific fallback signals,
        // execute the logic manually.
        if (botResponse.contains("무료 사용량 한도") || botResponse.startsWith("죄송합니다.")
                || botResponse.contains("시스템 연결을 시도")) {

            // 4.5.1 Eco Points
            if (userMessage.contains("에코") && (userMessage.contains("포인트") || userMessage.contains("점수"))) {
                java.util.Map<String, Object> args = new java.util.HashMap<>();
                args.put("userId", userId);
                String resultJson = functionDispatcher.dispatch("getEcoPoints", args);
                // Parse simple JSON (Mocking Parser) -> resultJson example: {"currentPoints":
                // 0}
                String points = resultJson.replaceAll("[^0-9]", "");
                botResponse = "고객님의 현재 에코 포인트는 **" + points + "점**입니다. ";

                // 4.5.2 Wardrobe
            } else if (userMessage.contains("옷장") && (userMessage.contains("몇") || userMessage.contains("개")
                    || userMessage.contains("목록") || userMessage.contains("뭐") || userMessage.contains("있어"))) {
                java.util.Map<String, Object> args = new java.util.HashMap<>();
                args.put("userId", userId);
                String resultJson = functionDispatcher.dispatch("getWardrobeSummary", args);

                // Regex to extract value of "totalItems"
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"totalItems\"\\s*:\\s*(\\d+)")
                        .matcher(resultJson);
                String items = m.find() ? m.group(1) : "0";

                // Fallback implies simple list is hard, so return summary + link
                botResponse = "고객님의 옷장에는 현재 **" + items
                        + "벌**의 옷이 등록되어 있습니다. \n자세한 목록은 [나만의 옷장](/wardrobe)에서 확인하실 수 있습니다.";

                // 4.5.3 판매중인 옷 조회
            } else if ((userMessage.contains("판매") || userMessage.contains("팔고"))
                    && (userMessage.contains("옷") || userMessage.contains("뭐") || userMessage.contains("있"))) {
                java.util.Map<String, Object> args = new java.util.HashMap<>();
                args.put("userId", userId);
                String resultJson = functionDispatcher.dispatch("getItemsForSale", args);

                // Parse count
                java.util.regex.Matcher countMatcher = java.util.regex.Pattern.compile("\"count\"\\s*:\\s*(\\d+)")
                        .matcher(resultJson);
                String count = countMatcher.find() ? countMatcher.group(1) : "0";

                if ("0".equals(count)) {
                    botResponse = "현재 고객님이 판매중인 옷이 없습니다. [판매하기](/market/register)에서 옷을 등록해보세요!";
                } else {
                    // Parse results
                    java.util.regex.Matcher resultMatcher = java.util.regex.Pattern
                            .compile("\"name\"\\s*:\\s*\"([^\"]+)\"")
                            .matcher(resultJson);
                    StringBuilder itemList = new StringBuilder();
                    while (resultMatcher.find()) {
                        if (itemList.length() > 0)
                            itemList.append(", ");
                        itemList.append("**").append(resultMatcher.group(1)).append("**");
                    }
                    botResponse = "고객님이 현재 판매중인 옷은 총 **" + count + "벌**입니다: " + itemList.toString() +
                            "\n\n[Re:Store 마켓](/market/list)에서 확인하실 수 있습니다.";
                }
            }
        }

        // 5. Check for Function Call (Robust Regex Matching)
        // Regex allows spaces around colons: "CALL : function : arg" or
        // "CALL:function:arg"
        java.util.regex.Pattern callPattern = java.util.regex.Pattern.compile(
                "CALL\\s*[:\\s]\\s*([a-zA-Z0-9_]+)(?:\\s*[:\\s]\\s*(.*))?", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = callPattern.matcher(botResponse);

        if (matcher.find()) {
            String functionName = matcher.group(1).trim();
            String argument = matcher.group(2) != null ? matcher.group(2).trim() : null;

            // Prepare arguments
            java.util.Map<String, Object> args = new java.util.HashMap<>();
            args.put("userId", userId);
            if (argument != null) {
                args.put("keyword", argument);
            }

            // Execute Function
            String functionResult = functionDispatcher.dispatch(functionName, args);

            // 6. Re-prompt LLM with function result
            String secondPrompt = String.format(
                    """
                            고객님의 질문: %s

                            검색 결과 (Tool Result):
                            %s

                            [필수 규칙]
                            1. 위 검색 결과의 "name" 필드를 **반드시** 그대로 사용하세요.
                            2. 예시: {"name":"화이트 기본 티셔츠", "brand":"Nike"} → "[보유] 상의/Nike/화이트 기본 티셔츠"
                            3. **절대** "상의/Nike/"처럼 이름 없이 말하지 마세요. 이름을 꼭 붙이세요.
                            4. 상황에 맞지 않으면 "적절한 옷이 없네요"라고 말하고 일반 아이템을 추천하세요.
                            5. 고객님이라고 호칭하세요.
                            6. **순수 한국어로만** 답변하세요. 영어 단어(예: good, like, want)는 절대 사용하지 마세요.
                            """,
                    userMessage, functionResult);

            System.out.println("🔍 [DEBUG] ChatbotService secondPrompt:");
            System.out.println(secondPrompt);

            // Get Final Answer
            botResponse = llmClient.generateResponse(SYSTEM_PROMPT, secondPrompt, userContext);
        }

        // 7. Save Bot Response (Only the final natural language answer)
        saveMessageLog(roomId, botId, "TEXT", botResponse, null);

        return botResponse;
    }

    public void saveMessage(com.rebirth.my.domain.ChatMessage msg) {
        Long roomId = msg.getRoomId();
        // SenderId is String in ChatMessage? Let's check ChatMessage definition.
        // It has senderId as String.
        Long senderId = null;
        try {
            senderId = Long.parseLong(msg.getSenderId());
        } catch (NumberFormatException e) {
            // handle error or ignore
            System.err.println("Invalid senderId: " + msg.getSenderId());
            return;
        }

        String type = "TEXT";
        if (msg.getType() != null) {
            type = msg.getType().toString();
        }

        saveMessageLog(roomId, senderId, type, msg.getContent(), msg.getImageUrl());
    }

    private void saveMessageLog(Long roomId, Long senderId, String type, String content, String imageUrl) {
        ChatVo vo = new ChatVo();
        vo.setRoomId(roomId);
        vo.setSenderId(senderId);
        vo.setMessageType(type);
        vo.setContent(content);
        vo.setImageUrl(imageUrl);
        // CreatedAt is handled by DB (SYSDATE)

        try {
            chatDao.insertChat(vo);
        } catch (Exception e) {
            System.err.println("Failed to save chat log: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 사용자 메시지에서 날짜 추출 (예: "31일", "이번달 31일", "12월 31일")
     * 
     * @return YYYY-MM-DD 형식 문자열, 추출 실패시 null
     */
    private String extractDateFromMessage(String message) {
        try {
            java.time.LocalDate now = java.time.LocalDate.now();
            int year = now.getYear();
            int month = now.getMonthValue();
            int day = -1;

            // 패턴 1: "12월 31일" 또는 "12월31일"
            java.util.regex.Matcher monthDayMatcher = java.util.regex.Pattern.compile("(\\d{1,2})월\\s*(\\d{1,2})일")
                    .matcher(message);
            if (monthDayMatcher.find()) {
                month = Integer.parseInt(monthDayMatcher.group(1));
                day = Integer.parseInt(monthDayMatcher.group(2));
            }

            // 패턴 2: "31일" (월 없이 일만)
            if (day == -1) {
                java.util.regex.Matcher dayOnlyMatcher = java.util.regex.Pattern.compile("(\\d{1,2})일")
                        .matcher(message);
                if (dayOnlyMatcher.find()) {
                    day = Integer.parseInt(dayOnlyMatcher.group(1));
                }
            }

            // 패턴 3: "내일", "모레" 처리
            if (message.contains("내일")) {
                java.time.LocalDate tomorrow = now.plusDays(1);
                return tomorrow.toString();
            }
            if (message.contains("모레")) {
                java.time.LocalDate dayAfter = now.plusDays(2);
                return dayAfter.toString();
            }
            if (message.contains("오늘")) {
                return now.toString();
            }

            if (day > 0 && day <= 31) {
                return String.format("%04d-%02d-%02d", year, month, day);
            }

        } catch (Exception e) {
            System.err.println("날짜 추출 실패: " + e.getMessage());
        }
        return null;
    }

}