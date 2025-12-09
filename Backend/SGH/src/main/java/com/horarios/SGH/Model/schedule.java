package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

/**
 * Clase de compatibilidad para mantener la API funcionando.
 * Esta clase será deprecada en favor de Schedule.java
 */
@Entity
@Table(name = "schedules")
@Deprecated
public class schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    @NotNull(message = "El curso es obligatorio")
    private courses courseId;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    @NotNull(message = "El profesor es obligatorio")
    private teachers teacherId;

    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false)
    @NotNull(message = "La materia es obligatoria")
    private subjects subjectId;

    @Column(name = "day_of_week", nullable = false)
    @NotNull(message = "El día de la semana es obligatorio")
    private String day;

    @Column(name = "start_time", nullable = false)
    @NotNull(message = "La hora de inicio es obligatoria")
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    @NotNull(message = "La hora de fin es obligatoria")
    private LocalTime endTime;

    @Column(name = "schedule_name", length = 100)
    @Size(max = 100, message = "El nombre del horario debe tener máximo 100 caracteres")
    private String scheduleName;

    // Constructor vacío
    public schedule() {}

    // Constructor con parámetros principales
    public schedule(courses courseId, teachers teacherId, subjects subjectId, String day, 
                   LocalTime startTime, LocalTime endTime, String scheduleName) {
        this.courseId = courseId;
        this.teacherId = teacherId;
        this.subjectId = subjectId;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.scheduleName = scheduleName;
    }

    // Constructor completo con ID
    public schedule(Integer id, courses courseId, teachers teacherId, subjects subjectId, String day, 
                   LocalTime startTime, LocalTime endTime, String scheduleName) {
        this.id = id;
        this.courseId = courseId;
        this.teacherId = teacherId;
        this.subjectId = subjectId;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.scheduleName = scheduleName;
    }

    // Getters y setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public courses getCourseId() {
        return courseId;
    }

    public void setCourseId(courses courseId) {
        this.courseId = courseId;
    }

    public teachers getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(teachers teacherId) {
        this.teacherId = teacherId;
    }

    public subjects getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(subjects subjectId) {
        this.subjectId = subjectId;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getScheduleName() {
        return scheduleName;
    }

    public void setScheduleName(String scheduleName) {
        this.scheduleName = scheduleName;
    }

    @Override
    public String toString() {
        return String.format(
            "schedule{id=%d, course=%s, teacher=%s, subject=%s, day=%s, time=%s-%s, name=%s}",
            id, 
            courseId != null ? courseId.getCourseName() : "null",
            teacherId != null ? teacherId.getTeacherName() : "null",
            subjectId != null ? subjectId.getSubjectName() : "null",
            day,
            startTime,
            endTime,
            scheduleName
        );
    }
}