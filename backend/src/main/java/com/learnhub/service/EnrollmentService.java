package com.learnhub.service;

import com.learnhub.dto.CourseProgressDTO;
import com.learnhub.dto.LessonProgressDTO;
import com.learnhub.entity.*;
import com.learnhub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import java.util.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Transactional
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;

    public Map<String, Object> enroll(String userEmail, Long courseId) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User session not found");
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
            return Map.of(
                "success", true,
                "message", "You are already enrolled in this course",
                "courseId", courseId
            );
        }

        Enrollment enrollment = Enrollment.builder().user(user).course(course).progress(0).build();
        Enrollment saved = enrollmentRepository.save(enrollment);

        Map<String, Object> response = new HashMap<>();
        response.put("enrollmentId", saved.getId());
        response.put("courseTitle", course.getTitle());
        response.put("message", "Enrollment successful");
        return response;
    }

    public List<Map<String, Object>> getMyEnrollments(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return enrollmentRepository.findByUserId(user.getId()).stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("enrollmentId", e.getId());
                    map.put("courseId", e.getCourse().getId());
                    // Avoid lazy loading course title if possible, or ensure it's fetched
                    Course c = e.getCourse();
                    map.put("courseTitle", c.getTitle());
                    map.put("enrolledAt", e.getEnrolledAt());

                    // Calculate progress dynamically
                    List<Lesson> lessons = lessonRepository.findByCourseIdOrderByPosition(c.getId());
                    long totalLessons = lessons.size();
                    long completedLessons = lessonProgressRepository.countByEnrollmentIdAndCompletedTrue(e.getId());

                    int progress = (totalLessons > 0) ? (int) ((completedLessons * 100) / totalLessons) : 0;
                    map.put("progress", progress);
                    map.put("completedLessons", completedLessons);
                    map.put("totalLessons", totalLessons);

                    // Determine next lesson ID
                    Long nextLessonId = null;
                    for (int i = 0; i < lessons.size(); i++) {
                        Lesson currentLesson = lessons.get(i);
                        boolean isCompleted = lessonProgressRepository.findByEnrollmentIdAndLessonId(e.getId(), currentLesson.getId())
                                .map(LessonProgress::isCompleted)
                                .orElse(false);

                        if (!isCompleted) {
                            boolean isUnlocked = (i == 0) || lessonProgressRepository.findByEnrollmentIdAndLessonId(e.getId(), lessons.get(i - 1).getId())
                                    .map(LessonProgress::isCompleted)
                                    .orElse(false);
                            if (isUnlocked) {
                                nextLessonId = currentLesson.getId();
                                break;
                            }
                        }
                    }
                    map.put("nextLessonId", nextLessonId);

                    return map;
                }).toList();
    }

    public CourseProgressDTO getCourseProgress(String userEmail, Long courseId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));

        List<Lesson> courseLessons = lessonRepository.findByCourseIdOrderByPosition(courseId);
        Map<Long, LessonProgress> lessonProgressMap = lessonProgressRepository.findByEnrollmentId(enrollment.getId())
                .stream()
                .collect(Collectors.toMap(lp -> lp.getLesson().getId(), lp -> lp));

        long totalLessons = courseLessons.size();
        long completedLessonsCount = 0;
        Long nextLessonId = null;

        List<LessonProgressDTO> lessonProgressDTOs = new ArrayList<>();
        for (int i = 0; i < courseLessons.size(); i++) {
            Lesson lesson = courseLessons.get(i);
            LessonProgress lp = lessonProgressMap.get(lesson.getId());

            boolean isCompleted = (lp != null && lp.isCompleted());
            if (isCompleted) {
                completedLessonsCount++;
            }

            // Determine unlocked status
            boolean isUnlocked = (i == 0) || (lessonProgressMap.containsKey(courseLessons.get(i - 1).getId()) && lessonProgressMap.get(courseLessons.get(i - 1).getId()).isCompleted());

            if (!isCompleted && isUnlocked && nextLessonId == null) {
                nextLessonId = lesson.getId();
            }

            lessonProgressDTOs.add(LessonProgressDTO.builder()
                    .id(lesson.getId())
                    .title(lesson.getTitle())
                    .position(lesson.getPosition())
                    .videoUrl(lesson.getVideoUrl())
                    .duration(lesson.getDuration())
                    .completed(isCompleted)
                    .unlocked(isUnlocked)
                    .completedAt(lp != null ? lp.getCompletedAt() : null)
                    .build());
        }

        int overallProgress = (totalLessons > 0) ? (int) ((completedLessonsCount * 100) / totalLessons) : 0;

        // Update enrollment's aggregate progress
        enrollment.setProgress(overallProgress);
        enrollmentRepository.save(enrollment);

        return CourseProgressDTO.builder()
                .enrollmentId(enrollment.getId())
                .courseId(courseId)
                .courseTitle(enrollment.getCourse().getTitle())
                .progress(overallProgress)
                .completedLessons((int) completedLessonsCount)
                .totalLessons((int) totalLessons)
                .nextLessonId(nextLessonId)
                .lessons(lessonProgressDTOs)
                .build();
    }

    public CourseProgressDTO completeLesson(String userEmail, Long courseId, Long lessonId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(user.getId(), courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));
        Lesson lesson = lessonRepository.findByIdAndCourseId(lessonId, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lesson not found in this course"));

        // Check if the lesson is unlocked before allowing completion
        List<Lesson> courseLessons = lessonRepository.findByCourseIdOrderByPosition(courseId);
        int lessonIndex = courseLessons.indexOf(lesson);
        if (lessonIndex > 0) { // Not the first lesson
            Lesson previousLesson = courseLessons.get(lessonIndex - 1);
            boolean previousLessonCompleted = lessonProgressRepository.findByEnrollmentIdAndLessonId(enrollment.getId(), previousLesson.getId())
                    .map(LessonProgress::isCompleted)
                    .orElse(false);
            if (!previousLessonCompleted) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Previous lesson must be completed first.");
            }
        }

        LessonProgress lessonProgress = lessonProgressRepository.findByEnrollmentIdAndLessonId(enrollment.getId(), lessonId)
                .orElse(LessonProgress.builder().enrollment(enrollment).lesson(lesson).build());

        if (!lessonProgress.isCompleted()) {
            lessonProgress.setCompleted(true);
            lessonProgress.setCompletedAt(LocalDateTime.now());
             lessonProgressRepository.save(lessonProgress);
        }

        // Recalculate and return the updated course progress
        return getCourseProgress(userEmail, courseId);
    }
}
