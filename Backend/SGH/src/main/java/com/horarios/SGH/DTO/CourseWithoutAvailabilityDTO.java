package com.horarios.SGH.DTO;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO para representar cursos que no tienen profesores con disponibilidad")
public class CourseWithoutAvailabilityDTO {

    @Schema(description = "ID del curso", example = "1")
    private Integer courseId;

    @Schema(description = "Nombre del curso", example = "Matemáticas 1A")
    private String courseName;

    @Schema(description = "ID del profesor asignado", example = "5")
    private Integer teacherId;

    @Schema(description = "Nombre del profesor", example = "Juan Pérez")
    private String teacherName;

    @Schema(description = "Razón por la cual no hay disponibilidad", 
            allowableValues = {"NO_AVAILABILITY_DEFINED", "CONFLICTS_WITH_EXISTING", "NO_TIME_SLOTS_AVAILABLE"},
            example = "NO_AVAILABILITY_DEFINED")
    private String reason;

    @Schema(description = "Descripción detallada del problema", 
            example = "El profesor Juan Pérez no tiene disponibilidad configurada para ningún día de la semana")
    private String description;

    public CourseWithoutAvailabilityDTO() {}

    public CourseWithoutAvailabilityDTO(Integer courseId, String courseName, Integer teacherId, 
                                       String teacherName, String reason, String description) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.reason = reason;
        this.description = description;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}