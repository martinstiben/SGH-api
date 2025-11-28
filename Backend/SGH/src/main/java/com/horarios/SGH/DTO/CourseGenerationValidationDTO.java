package com.horarios.SGH.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseGenerationValidationDTO {
    private Integer courseId;
    private String courseName;
    private String teacherName;
    private boolean canGenerate;
    private List<String> issues;
    private List<String> recommendedDays;
}