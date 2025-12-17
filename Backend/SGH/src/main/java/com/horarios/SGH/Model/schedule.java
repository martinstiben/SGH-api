package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalTime;

@Entity(name = "schedules")
@Data
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"courseId", "day", "start_time", "end_time"}))
public class schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "courseId", nullable = false)
    private courses courseId;

    @ManyToOne
    @JoinColumn(name = "teacherId", nullable = false)
    private teachers teacherId;

    @ManyToOne
    @JoinColumn(name = "subjectId", nullable = false)
    private subjects subjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day", nullable = false)
    private Days day;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "schedule_name", length = 100)
    @Size(max = 100, message = "El nombre del horario debe tener máximo 100 caracteres")
    private String scheduleName;

    public schedule() {}

    public schedule(Integer id, courses courseId, teachers teacherId, subjects subjectId, Days day, LocalTime startTime, LocalTime endTime, String scheduleName) {
        this.id = id;
        this.courseId = courseId;
        this.teacherId = teacherId;
        this.subjectId = subjectId;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.scheduleName = scheduleName;
    }

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

    public Days getDay() {
        return day;
    }

    public void setDay(Days day) {
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
}