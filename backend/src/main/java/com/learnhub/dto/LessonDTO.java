package com.learnhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class LessonDTO {
    private Long id;
    private String title;
    private String videoUrl;
    private String duration;
    private int position;
}