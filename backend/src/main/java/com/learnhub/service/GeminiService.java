package com.learnhub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String geminiKey;

    @Value("${gemini.api.url:}")
    private String geminiUrl;

    @Value("${grok.api.key:}")
    private String grokKey;

    @Value("${grok.api.url:https://api.x.ai/v1/chat/completions}")
    private String grokUrl;

    @Value("${grok.model:grok-3-mini}")
    private String grokModel;

    private final WebClient webClient = WebClient.builder().build();

    // ── Route to correct provider ─────────────────────────
    public String ask(String prompt, String model) {
        return "grok".equalsIgnoreCase(model) ? askGrok(prompt) : askGemini(prompt);
    }

    // ── Gemini ────────────────────────────────────────────
    private String askGemini(String prompt) {
        Map<String, Object> body = Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );
        try {
            Map response = webClient.post()
                .uri(geminiUrl + "?key=" + geminiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .onStatus(s -> s.value() == 429,
                    r -> r.bodyToMono(String.class).map(b -> new RuntimeException("QUOTA_EXCEEDED")))
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
                    r -> r.bodyToMono(String.class).map(b -> new RuntimeException("API_ERROR: " + b)))
                .bodyToMono(Map.class)
                .block();

            var candidates = (List<?>) response.get("candidates");
            var content    = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
            var parts      = (List<?>) content.get("parts");
            return ((Map<?, ?>) parts.get(0)).get("text").toString();

        } catch (RuntimeException e) {
            return e.getMessage() != null && e.getMessage().contains("QUOTA_EXCEEDED")
                ? "ERROR:QUOTA_EXCEEDED" : "ERROR:" + e.getMessage();
        } catch (Exception e) {
            return "ERROR:Gemini unavailable.";
        }
    }

    // ── Grok (xAI — OpenAI-compatible format) ────────────
    private String askGrok(String prompt) {
        Map<String, Object> body = Map.of(
            "model", grokModel,
            "messages", List.of(Map.of("role", "user", "content", prompt))
        );
        try {
            Map response = webClient.post()
                .uri(grokUrl)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + grokKey)
                .bodyValue(body)
                .retrieve()
                .onStatus(s -> s.value() == 429,
                    r -> r.bodyToMono(String.class).map(b -> new RuntimeException("QUOTA_EXCEEDED")))
                .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
                    r -> r.bodyToMono(String.class).map(b -> new RuntimeException("API_ERROR: " + b)))
                .bodyToMono(Map.class)
                .block();

            var choices = (List<?>) response.get("choices");
            var message = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("message");
            return message.get("content").toString();

        } catch (RuntimeException e) {
            return e.getMessage() != null && e.getMessage().contains("QUOTA_EXCEEDED")
                ? "ERROR:QUOTA_EXCEEDED" : "ERROR:" + e.getMessage();
        } catch (Exception e) {
            return "ERROR:Grok unavailable.";
        }
    }

    // ── Helper Methods for AiController ───────────────────
    public String solveDoubt(String question, String courseName, String model) {
        String prompt = String.format("I am studying %s. Can you explain: %s", courseName, question);
        return ask(prompt, model);
    }

    public String recommendCourses(List<String> enrolledCourses, String model) {
        String prompt = "Based on these courses I've taken: " + String.join(", ", enrolledCourses) + 
                        ". What should I learn next? Give me 3 suggestions.";
        return ask(prompt, model);
    }

    public String generateQuiz(String topic, String model) {
        String prompt = "Generate a 3-question multiple choice quiz about " + topic + " with answers at the end.";
        return ask(prompt, model);
    }
}