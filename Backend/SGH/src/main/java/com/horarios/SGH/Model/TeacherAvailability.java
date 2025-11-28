package com.horarios.SGH.Model;

import jakarta.persistence.*;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "TeacherAvailability", uniqueConstraints = @UniqueConstraint(columnNames = { "teacher_id", "day" }))
@Schema(description = "Entidad de disponibilidad de profesor")
public class TeacherAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único de la disponibilidad", example = "1")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    @Schema(description = "Profesor asociado")
    private teachers teacher;

    @Column(nullable = false)
    @Schema(description = "Día de la semana", example = "Lunes")
    private Days day;

    @Column
    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Hora de inicio de la mañana", example = "08:00", type = "string", format = "time")
    private LocalTime amStart;

    @Column
    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Hora de fin de la mañana", example = "12:00", type = "string", format = "time")
    private LocalTime amEnd;

    @Column
    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Hora de inicio de la tarde", example = "14:00", type = "string", format = "time")
    private LocalTime pmStart;

    @Column
    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Hora de fin de la tarde", example = "18:00", type = "string", format = "time")
    private LocalTime pmEnd;

    // Método auxiliar para verificar si hay al menos un horario válido
    public boolean hasValidSchedule() {
        return (amStart != null && amEnd != null) || (pmStart != null && pmEnd != null);
    }

    public TeacherAvailability() {
    }

    public TeacherAvailability(Long id, teachers teacher, Days day, LocalTime amStart, LocalTime amEnd,
            LocalTime pmStart, LocalTime pmEnd) {
        this.id = id;
        this.teacher = teacher;
        this.day = day;
        this.amStart = amStart;
        this.amEnd = amEnd;
        this.pmStart = pmStart;
        this.pmEnd = pmEnd;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public teachers getTeacher() {
        return teacher;
    }

    public void setTeacher(teachers teacher) {
        this.teacher = teacher;
    }

    public Days getDay() {
        return day;
    }

    public void setDay(Days day) {
        this.day = day;
    }

    public LocalTime getAmStart() {
        return amStart;
    }

    public void setAmStart(LocalTime amStart) {
        this.amStart = amStart;
    }

    public LocalTime getAmEnd() {
        return amEnd;
    }

    public void setAmEnd(LocalTime amEnd) {
        this.amEnd = amEnd;
    }

    public LocalTime getPmStart() {
        return pmStart;
    }

    public void setPmStart(LocalTime pmStart) {
        this.pmStart = pmStart;
    }

    public LocalTime getPmEnd() {
        return pmEnd;
    }

    public void setPmEnd(LocalTime pmEnd) {
        this.pmEnd = pmEnd;
    }
}