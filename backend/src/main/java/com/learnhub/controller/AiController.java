package com.learnhub.controller;

import com.learnhub.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final GeminiService geminiService;

    // POST /api/ai/doubt
    // Body: { "question": "What is React Hook?", "courseName": "Full Stack" }
    @PostMapping("/doubt")
    public ResponseEntity<Map<String, String>> solveDoubt(
            @RequestBody Map<String, String> body) {
        String answer = geminiService.solveDoubt(
                body.get("question"),
                body.getOrDefault("courseName", "Programming"));
        return ResponseEntity.ok(Map.of("answer", answer));
    }

    // POST /api/ai/recommend
    // Body: { "enrolledCourses": ["React", "Java"] }
    @PostMapping("/recommend")
    public ResponseEntity<Map<String, String>> recommend(
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> courses = (List<String>) body.getOrDefault("enrolledCourses", List.of());
        String recommendations = geminiService.recommendCourses(courses);
        return ResponseEntity.ok(Map.of("recommendations", recommendations));
    }

    // POST /api/ai/quiz
    // Body: { "topic": "React Hooks" }
    @PostMapping("/quiz")
    public ResponseEntity<Map<String, String>> generateQuiz(
            @RequestBody Map<String, String> body) {
        String quiz = geminiService.generateQuiz(
                body.getOrDefault("topic", "Programming"));
        return ResponseEntity.ok(Map.of("quiz", quiz));
    }
}
