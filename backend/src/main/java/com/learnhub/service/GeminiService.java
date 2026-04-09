package com.learnhub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final WebClient webClient = WebClient.builder().build();

    // Core method — sends prompt to Gemini, returns text response
    public String ask(String prompt) {
        Map<String, Object> body = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
            )
        );

        try {
            Map response = webClient.post()
                .uri(apiUrl + "?key=" + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            // Extract text from Gemini response
            var candidates = (List<?>) response.get("candidates");
            var content    = (Map<?, ?>) ((Map<?, ?>) candidates.get(0)).get("content");
            var parts      = (List<?>) content.get("parts");
            return ((Map<?, ?>) parts.get(0)).get("text").toString();

        } catch (Exception e) {
            return "AI service unavailable. Please try again later.";
        }
    }

    // ── Feature 1: Doubt Solver ───────────────────────────
    public String solveDoubt(String question, String courseName) {
        String prompt = String.format(
            "You are a helpful tutor for the course '%s'. " +
            "Answer this student's question clearly and concisely in 3-5 lines: %s",
            courseName, question
        );
        return ask(prompt);
    }

    // ── Feature 2: Course Recommender ────────────────────
    public String recommendCourses(List<String> enrolledCourses) {
        String enrolled = String.join(", ", enrolledCourses);
        String prompt = String.format(
            "A student is enrolled in these courses: %s. " +
            "Recommend 3 next courses they should take to advance their career. " +
            "Format as a numbered list with course name and one-line reason. Keep it brief.",
            enrolled.isEmpty() ? "none yet" : enrolled
        );
        return ask(prompt);
    }

    // ── Feature 3: Quiz Generator ─────────────────────────
    public String generateQuiz(String topic) {
        String prompt = String.format(
            "Generate 5 multiple choice questions about '%s'. " +
            "Format each question as:\n" +
            "Q1. [question]\n" +
            "A) option  B) option  C) option  D) option\n" +
            "Answer: [correct option]\n\n" +
            "Keep questions practical and beginner-friendly.",
            topic
        );
        return ask(prompt);
    }
}
