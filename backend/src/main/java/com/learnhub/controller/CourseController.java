package com.learnhub.controller;

import com.learnhub.dto.CourseDTO;
import com.learnhub.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    // Public - anyone can browse courses
    @GetMapping
    public ResponseEntity<List<CourseDTO>> getAllCourses(
            @RequestParam(required = false) String search) {
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(courseService.searchCourses(search));
        }
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    // Public - anyone can view details
    @GetMapping("/{id}/details")
    public ResponseEntity<CourseDTO> getCourse(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    // Protected - only INSTRUCTOR or ADMIN
    @PostMapping
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR','ADMIN')")
    public ResponseEntity<CourseDTO> createCourse(@Valid @RequestBody CourseDTO dto,
                                                   Principal principal) {
        return ResponseEntity.ok(courseService.createCourse(dto, principal.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR','ADMIN')")
    public ResponseEntity<CourseDTO> updateCourse(@PathVariable Long id,
                                                   @Valid @RequestBody CourseDTO dto) {
        return ResponseEntity.ok(courseService.updateCourse(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }
}
