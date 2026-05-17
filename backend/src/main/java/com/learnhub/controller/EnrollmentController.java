package com.learnhub.controller;

import com.learnhub.dto.CourseProgressDTO;
import com.learnhub.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/{courseId}")
    public ResponseEntity<?> enroll(@PathVariable Long courseId, Principal principal) {
        return ResponseEntity.ok(enrollmentService.enroll(principal.getName(), courseId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Map<String, Object>>> myEnrollments(Principal principal) {
        return ResponseEntity.ok(enrollmentService.getMyEnrollments(principal.getName()));
    }

    @GetMapping("/progress/{courseId}")
    public ResponseEntity<CourseProgressDTO> getCourseProgress(
            @PathVariable Long courseId,
            Principal principal) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("PROGRESS AUTH = " + auth);
        System.out.println("PROGRESS AUTHORITIES = " + auth.getAuthorities());
        return ResponseEntity.ok(enrollmentService.getCourseProgress(principal.getName(), courseId));
    }

    @PostMapping("/progress/{courseId}/lessons/{lessonId}/complete")
    public ResponseEntity<CourseProgressDTO> completeLesson(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            Principal principal) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("COMPLETE AUTH = " + auth);
        System.out.println("COMPLETE AUTHORITIES = " + auth.getAuthorities());
        return ResponseEntity.ok(enrollmentService.completeLesson(principal.getName(), courseId, lessonId));
    }
}
