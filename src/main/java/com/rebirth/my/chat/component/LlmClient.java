package com.rebirth.my.chat.component;

public interface LlmClient {
    /**
     * Sends a prompt to the LLM and gets a response.
     * 
     * @param systemPrompt The instruction for the system behavior.
     * @param userMessage  The actual user input.
     * @param context      Additional context (JSON string or text).
     * @return The LLM's text response.
     */
    String generateResponse(String systemPrompt, String userMessage, String context);
}
