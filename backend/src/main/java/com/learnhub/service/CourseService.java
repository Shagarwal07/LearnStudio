package com.learnhub.service;

import com.learnhub.dto.CourseDTO;
import com.learnhub.dto.LessonDTO;
import com.learnhub.entity.Course;
import com.learnhub.repository.CourseRepository;
import com.learnhub.repository.LessonRepository;
import com.learnhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;

    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll().stream().map(this::toDTO).toList();
    }

    public CourseDTO getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        CourseDTO dto = toDTO(course);
        
        // Fetch lessons separately to avoid LazyInitializationException
        List<LessonDTO> lessons = lessonRepository.findByCourseIdOrderByPosition(id).stream()
                .map(l -> LessonDTO.builder()
                        .id(l.getId()).title(l.getTitle()).videoUrl(l.getVideoUrl())
                        .duration(l.getDuration()).position(l.getPosition()).build())
                .toList();
        
        dto.setLessons(lessons);
        dto.setTotalLessons(lessons.size());
        return dto;
    }

    public List<CourseDTO> searchCourses(String keyword) {
        return courseRepository.searchByTitle(keyword).stream().map(this::toDTO).toList();
    }

    public CourseDTO createCourse(CourseDTO dto, String instructorEmail) {
        var instructor = userRepository.findByEmail(instructorEmail)
                .orElseThrow(() -> new RuntimeException("Instructor not found"));
        Course course = Course.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .thumbnail(dto.getThumbnail())
                .level(dto.getLevel())
                .instructor(instructor)
                .build();
        return toDTO(courseRepository.save(course));
    }

    public CourseDTO updateCourse(Long id, CourseDTO dto) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        course.setTitle(dto.getTitle());
        course.setDescription(dto.getDescription());
        course.setPrice(dto.getPrice());
        course.setLevel(dto.getLevel());
        return toDTO(courseRepository.save(course));
    }

    public void deleteCourse(Long id) {
        courseRepository.deleteById(id);
    }

    private CourseDTO toDTO(Course c) {
        CourseDTO dto = new CourseDTO();
        dto.setId(c.getId());
        dto.setTitle(c.getTitle());
        dto.setDescription(c.getDescription());
        dto.setPrice(c.getPrice());
        dto.setThumbnail(c.getThumbnail());
        dto.setLevel(c.getLevel());
        dto.setInstructorName(c.getInstructor() != null ? c.getInstructor().getName() : "");
        // lessons and totalLessons are handled in getCourseById or via separate query to avoid LazyInitializationException
        dto.setTotalLessons(0);
        return dto;
    }
}
