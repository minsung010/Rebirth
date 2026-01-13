package com.rebirth.my.chat.component;

import org.springframework.beans.factory.annotation.Value;
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
public class EmbeddingClient {

    @Value("${google.gemini.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // Use 'text-embedding-004' (newest) or 'embedding-001'
    private static final String MODEL_NAME = "text-embedding-004";

    public List<Float> getEmbedding(String text) {
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":embedContent?key="
                    + apiKey;

            Map<String, Object> requestBody = new HashMap<>();

            Map<String, Object> contentPart = new HashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> textPart = new HashMap<>();
            textPart.put("text", text);
            parts.add(textPart);
            contentPart.put("parts", parts);

            requestBody.put("content", contentPart);
            requestBody.put("model", "models/" + MODEL_NAME);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            return extractEmbedding((Map<String, Object>) response.getBody());

        } catch (Exception e) {
            System.err.println("❌ Embedding Generation Failed: " + e.getMessage());
            return new ArrayList<>(); // Return empty on failure
        }
    }

    @SuppressWarnings("unchecked")
    private List<Float> extractEmbedding(Map<String, Object> body) {
        if (body == null || !body.containsKey("embedding"))
            return new ArrayList<>();

        Map<String, Object> embeddingMap = (Map<String, Object>) body.get("embedding");
        if (embeddingMap == null || !embeddingMap.containsKey("values"))
            return new ArrayList<>();

        List<Double> values = (List<Double>) embeddingMap.get("values");
        List<Float> floatValues = new ArrayList<>();

        for (Double d : values) {
            floatValues.add(d.floatValue());
        }

        return floatValues;
    }
}
