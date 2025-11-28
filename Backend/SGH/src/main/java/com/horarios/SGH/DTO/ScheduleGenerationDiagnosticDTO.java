package com.horarios.SGH.DTO;

import com.horarios.SGH.Model.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "DTO para diagnóstico detallado del sistema de generación de horarios")
public class ScheduleGenerationDiagnosticDTO {

    @Schema(description = "Lista de cursos en el sistema")
    private List<CourseDiagnosticDTO> courses;

    @Schema(description = "Lista de profesores en el sistema")
    private List<TeacherDiagnosticDTO> teachers;

    @Schema(description = "Lista de disponibilidades de profesores")
    private List<TeacherAvailability> teacherAvailabilities;

    @Schema(description = "Lista de horarios existentes")
    private List<ScheduleDTO> existingSchedules;

    @Schema(description = "Cursos que podrían tener problemas para generar horarios")
    private List<CourseWithoutAvailabilityDTO> problematicCourses;

    @Schema(description = "Estadísticas generales del sistema")
    private SystemStatistics statistics;

    public ScheduleGenerationDiagnosticDTO() {}

    public ScheduleGenerationDiagnosticDTO(List<CourseDiagnosticDTO> courses, List<TeacherDiagnosticDTO> teachers, 
                                         List<TeacherAvailability> teacherAvailabilities, List<ScheduleDTO> existingSchedules,
                                         List<CourseWithoutAvailabilityDTO> problematicCourses, SystemStatistics statistics) {
        this.courses = courses;
        this.teachers = teachers;
        this.teacherAvailabilities = teacherAvailabilities;
        this.existingSchedules = existingSchedules;
        this.problematicCourses = problematicCourses;
        this.statistics = statistics;
    }

    // Getters y Setters
    public List<CourseDiagnosticDTO> getCourses() { return courses; }
    public void setCourses(List<CourseDiagnosticDTO> courses) { this.courses = courses; }

    public List<TeacherDiagnosticDTO> getTeachers() { return teachers; }
    public void setTeachers(List<TeacherDiagnosticDTO> teachers) { this.teachers = teachers; }

    public List<TeacherAvailability> getTeacherAvailabilities() { return teacherAvailabilities; }
    public void setTeacherAvailabilities(List<TeacherAvailability> teacherAvailabilities) { this.teacherAvailabilities = teacherAvailabilities; }

    public List<ScheduleDTO> getExistingSchedules() { return existingSchedules; }
    public void setExistingSchedules(List<ScheduleDTO> existingSchedules) { this.existingSchedules = existingSchedules; }

    public List<CourseWithoutAvailabilityDTO> getProblematicCourses() { return problematicCourses; }
    public void setProblematicCourses(List<CourseWithoutAvailabilityDTO> problematicCourses) { this.problematicCourses = problematicCourses; }

    public SystemStatistics getStatistics() { return statistics; }
    public void setStatistics(SystemStatistics statistics) { this.statistics = statistics; }

    // DTOs auxiliares
    @Schema(description = "Información básica de curso para diagnóstico")
    public static class CourseDiagnosticDTO {
        private Integer id;
        private String courseName;
        private Integer teacherId;
        private String teacherName;
        private Integer subjectId;
        private String subjectName;
        private boolean hasScheduleAssigned;
        private String status;

        public CourseDiagnosticDTO() {}

        public CourseDiagnosticDTO(Integer id, String courseName, Integer teacherId, String teacherName, 
                                Integer subjectId, String subjectName, boolean hasScheduleAssigned, String status) {
            this.id = id;
            this.courseName = courseName;
            this.teacherId = teacherId;
            this.teacherName = teacherName;
            this.subjectId = subjectId;
            this.subjectName = subjectName;
            this.hasScheduleAssigned = hasScheduleAssigned;
            this.status = status;
        }

        // Getters y Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }

        public Integer getTeacherId() { return teacherId; }
        public void setTeacherId(Integer teacherId) { this.teacherId = teacherId; }

        public String getTeacherName() { return teacherName; }
        public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

        public Integer getSubjectId() { return subjectId; }
        public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }

        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

        public boolean isHasScheduleAssigned() { return hasScheduleAssigned; }
        public void setHasScheduleAssigned(boolean hasScheduleAssigned) { this.hasScheduleAssigned = hasScheduleAssigned; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    @Schema(description = "Información básica de profesor para diagnóstico")
    public static class TeacherDiagnosticDTO {
        private Integer id;
        private String teacherName;
        private Integer subjectCount;
        private Integer availabilityCount;
        private boolean canTeachMultipleSubjects;

        public TeacherDiagnosticDTO() {}

        public TeacherDiagnosticDTO(Integer id, String teacherName, Integer subjectCount, 
                                  Integer availabilityCount, boolean canTeachMultipleSubjects) {
            this.id = id;
            this.teacherName = teacherName;
            this.subjectCount = subjectCount;
            this.availabilityCount = availabilityCount;
            this.canTeachMultipleSubjects = canTeachMultipleSubjects;
        }

        // Getters y Setters
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }

        public String getTeacherName() { return teacherName; }
        public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

        public Integer getSubjectCount() { return subjectCount; }
        public void setSubjectCount(Integer subjectCount) { this.subjectCount = subjectCount; }

        public Integer getAvailabilityCount() { return availabilityCount; }
        public void setAvailabilityCount(Integer availabilityCount) { this.availabilityCount = availabilityCount; }

        public boolean isCanTeachMultipleSubjects() { return canTeachMultipleSubjects; }
        public void setCanTeachMultipleSubjects(boolean canTeachMultipleSubjects) { this.canTeachMultipleSubjects = canTeachMultipleSubjects; }
    }

    @Schema(description = "Estadísticas del sistema de horarios")
    public static class SystemStatistics {
        private long totalCourses;
        private long coursesWithSchedule;
        private long coursesWithoutSchedule;
        private long totalTeachers;
        private long teachersWithAvailability;
        private long teachersWithoutAvailability;
        private long totalExistingSchedules;
        private long totalTeacherSubjects;

        public SystemStatistics() {}

        public SystemStatistics(long totalCourses, long coursesWithSchedule, long coursesWithoutSchedule,
                              long totalTeachers, long teachersWithAvailability, long teachersWithoutAvailability,
                              long totalExistingSchedules, long totalTeacherSubjects) {
            this.totalCourses = totalCourses;
            this.coursesWithSchedule = coursesWithSchedule;
            this.coursesWithoutSchedule = coursesWithoutSchedule;
            this.totalTeachers = totalTeachers;
            this.teachersWithAvailability = teachersWithAvailability;
            this.teachersWithoutAvailability = teachersWithoutAvailability;
            this.totalExistingSchedules = totalExistingSchedules;
            this.totalTeacherSubjects = totalTeacherSubjects;
        }

        // Getters y Setters
        public long getTotalCourses() { return totalCourses; }
        public void setTotalCourses(long totalCourses) { this.totalCourses = totalCourses; }

        public long getCoursesWithSchedule() { return coursesWithSchedule; }
        public void setCoursesWithSchedule(long coursesWithSchedule) { this.coursesWithSchedule = coursesWithSchedule; }

        public long getCoursesWithoutSchedule() { return coursesWithoutSchedule; }
        public void setCoursesWithoutSchedule(long coursesWithoutSchedule) { this.coursesWithoutSchedule = coursesWithoutSchedule; }

        public long getTotalTeachers() { return totalTeachers; }
        public void setTotalTeachers(long totalTeachers) { this.totalTeachers = totalTeachers; }

        public long getTeachersWithAvailability() { return teachersWithAvailability; }
        public void setTeachersWithAvailability(long teachersWithAvailability) { this.teachersWithAvailability = teachersWithAvailability; }

        public long getTeachersWithoutAvailability() { return teachersWithoutAvailability; }
        public void setTeachersWithoutAvailability(long teachersWithoutAvailability) { this.teachersWithoutAvailability = teachersWithoutAvailability; }

        public long getTotalExistingSchedules() { return totalExistingSchedules; }
        public void setTotalExistingSchedules(long totalExistingSchedules) { this.totalExistingSchedules = totalExistingSchedules; }

        public long getTotalTeacherSubjects() { return totalTeacherSubjects; }
        public void setTotalTeacherSubjects(long totalTeacherSubjects) { this.totalTeacherSubjects = totalTeacherSubjects; }
    }
}