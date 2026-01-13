package com.rebirth.my.chat.component;

import org.springframework.stereotype.Component;
import java.util.logging.Logger;

@Component
public class MockLlmClient implements LlmClient {

    private static final Logger LOGGER = Logger.getLogger(MockLlmClient.class.getName());

    @Override
    public String generateResponse(String systemPrompt, String userMessage, String context) {
        LOGGER.info("[MockLLM] System: " + systemPrompt);
        LOGGER.info("[MockLLM] Context: " + context);
        LOGGER.info("[MockLLM] User: " + userMessage);

        // Simple Rule-based Simulation for "Real-world" feel
        String loweredMsg = userMessage.toLowerCase();

        if (loweredMsg.contains("안녕")) {
            return "안녕하세요! 리버스 고객센터 챗봇입니다. 무엇을 도와드릴까요? (Mock)";
        } else if (loweredMsg.contains("포인트") || loweredMsg.contains("점수")) {
            return "현재 고객님의 에코 포인트는 [Function: getUserPoints] 로 조회됩니다. (실제 DB 연동 예정)";
        } else if (loweredMsg.contains("추천") || loweredMsg.contains("코디")) {
            return "오늘 날씨에 맞춰 추천해드릴 코디는 [Function: recommendOutfit] 입니다. 마음에 드시나요?";
        }

        return "죄송합니다. 제가 답변하기 어려운 내용이네요. '포인트'나 '코디 추천'이라고 물어봐 주시겠어요?";
    }
}
