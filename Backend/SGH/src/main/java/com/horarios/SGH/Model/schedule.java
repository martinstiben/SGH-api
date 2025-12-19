package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

/**
 * Entidad que representa un horario de clase en el sistema SGH.
 * Un horario vincula un curso, docente, materia, día y horario específico.
 *
 * Extiende AbstractEntity para funcionalidades comunes como timestamps,
 * validación y operaciones estándar de entidades.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar horarios
 * - OCP: Abierto para extensión
 * - LSP: Sustituye a AbstractEntity
 *
 * Patrones de diseño aplicados:
 * - Template Method: Implementado a través de AbstractEntity
 * - Factory: Para creación centralizada (delegado a EntityFactory)
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Entity(name = "schedules")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"courseId", "day", "start_time", "end_time"}))
public class schedule extends AbstractEntity {

    /**
     * Identificador único del horario.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Integer id;

    /**
     * Curso al que pertenece el horario.
     */
    @ManyToOne
    @JoinColumn(name = "courseId", nullable = false)
    private courses courseId;

    /**
     * Docente que imparte la clase.
     */
    @ManyToOne
    @JoinColumn(name = "teacherId", nullable = false)
    private teachers teacherId;

    /**
     * Materia que se imparte en el horario.
     */
    @ManyToOne
    @JoinColumn(name = "subjectId", nullable = false)
    private subjects subjectId;

    /**
     * Día de la semana en que se realiza la clase.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "day", nullable = false)
    private Days day;

    /**
     * Hora de inicio de la clase.
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * Hora de fin de la clase.
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Nombre descriptivo del horario (opcional).
     */
    @Column(name = "schedule_name", length = 100)
    @Size(max = 100, message = "El nombre del horario debe tener máximo 100 caracteres")
    private String scheduleName;

    /**
     * Constructor por defecto requerido por JPA.
     * Inicializa los timestamps heredados de AbstractEntity.
     */
    public schedule() {
        super();
    }

    /**
     * Constructor completo para crear un horario.
     * Inicializa los timestamps heredados de AbstractEntity.
     *
     * @param id Identificador del horario
     * @param courseId Curso asociado
     * @param teacherId Docente asignado
     * @param subjectId Materia impartida
     * @param day Día de la semana
     * @param startTime Hora de inicio
     * @param endTime Hora de fin
     * @param scheduleName Nombre descriptivo
     */
    public schedule(Integer id, courses courseId, teachers teacherId, subjects subjectId, Days day, LocalTime startTime, LocalTime endTime, String scheduleName) {
        super();
        this.id = id;
        this.courseId = courseId;
        this.teacherId = teacherId;
        this.subjectId = subjectId;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.scheduleName = scheduleName;
    }

    /**
     * Obtiene el identificador del horario.
     *
     * @return ID del horario
     */
    public Integer getId() {
        return id;
    }

    /**
     * Establece el identificador del horario.
     *
     * @param id Nuevo ID del horario
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Obtiene el curso asociado al horario.
     *
     * @return Curso del horario
     */
    public courses getCourseId() {
        return courseId;
    }

    /**
     * Establece el curso del horario.
     *
     * @param courseId Nuevo curso
     */
    public void setCourseId(courses courseId) {
        this.courseId = courseId;
    }

    /**
     * Obtiene el docente asignado al horario.
     *
     * @return Docente del horario
     */
    public teachers getTeacherId() {
        return teacherId;
    }

    /**
     * Establece el docente del horario.
     *
     * @param teacherId Nuevo docente
     */
    public void setTeacherId(teachers teacherId) {
        this.teacherId = teacherId;
    }

    /**
     * Obtiene la materia impartida en el horario.
     *
     * @return Materia del horario
     */
    public subjects getSubjectId() {
        return subjectId;
    }

    /**
     * Establece la materia del horario.
     *
     * @param subjectId Nueva materia
     */
    public void setSubjectId(subjects subjectId) {
        this.subjectId = subjectId;
    }

    /**
     * Obtiene el día de la semana del horario.
     *
     * @return Día del horario
     */
    public Days getDay() {
        return day;
    }

    /**
     * Establece el día de la semana del horario.
     *
     * @param day Nuevo día
     */
    public void setDay(Days day) {
        this.day = day;
    }

    /**
     * Obtiene la hora de inicio del horario.
     *
     * @return Hora de inicio
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * Establece la hora de inicio del horario.
     *
     * @param startTime Nueva hora de inicio
     */
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    /**
     * Obtiene la hora de fin del horario.
     *
     * @return Hora de fin
     */
    public LocalTime getEndTime() {
        return endTime;
    }

    /**
     * Establece la hora de fin del horario.
     *
     * @param endTime Nueva hora de fin
     */
    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    /**
     * Obtiene el nombre descriptivo del horario.
     *
     * @return Nombre del horario
     */
    public String getScheduleName() {
        return scheduleName;
    }

    /**
     * Establece el nombre descriptivo del horario.
     *
     * @param scheduleName Nuevo nombre
     */
    public void setScheduleName(String scheduleName) {
        this.scheduleName = scheduleName;
    }

    /**
     * Valida la entidad antes de persistirla.
     * Verifica que los campos obligatorios del horario sean válidos.
     *
     * @throws IllegalArgumentException si la validación falla
     */
    @Override
    public void validate() {
        if (courseId == null) {
            throw new IllegalArgumentException("El curso es obligatorio");
        }
        if (teacherId == null) {
            throw new IllegalArgumentException("El docente es obligatorio");
        }
        if (subjectId == null) {
            throw new IllegalArgumentException("La materia es obligatoria");
        }
        if (day == null) {
            throw new IllegalArgumentException("El día es obligatorio");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("La hora de inicio es obligatoria");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("La hora de fin es obligatoria");
        }
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("La hora de fin debe ser posterior a la hora de inicio");
        }
        if (scheduleName != null && scheduleName.length() > 100) {
            throw new IllegalArgumentException("El nombre del horario debe tener máximo 100 caracteres");
        }
    }

    /**
     * Obtiene una representación resumida del horario.
     *
     * @return resumen como String
     */
    @Override
    public String getSummary() {
        String courseName = courseId != null ? "Curso " + courseId : "Sin curso";
        String teacherName = teacherId != null ? " - " + teacherId.getTeacherName() : " - Sin docente";
        String subjectName = subjectId != null ? " - " + subjectId.getSubjectName() : " - Sin materia";
        String timeSlot = day != null ? " - " + day.name() : " - Sin día";
        timeSlot += " " + (startTime != null ? startTime.toString() : "--:--") + 
                   (endTime != null ? "-" + endTime.toString() : "--:--");
        
        return courseName + teacherName + subjectName + timeSlot;
    }

    /**
     * Verifica si la entidad es nueva (no persistida).
     *
     * @return true si es nueva
     */
    @Override
    public boolean isNew() {
        return id == null;
    }

    /**
     * Método de utilidad para logging y debugging.
     *
     * @return representación en string del horario
     */
    @Override
    public String toString() {
        return "schedule{" +
                "id=" + id +
                ", courseId=" + (courseId != null ? courseId.getId() : "null") +
                ", teacherId=" + (teacherId != null ? teacherId.getId() : "null") +
                ", subjectId=" + (subjectId != null ? subjectId.getId() : "null") +
                ", day=" + day +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", scheduleName='" + scheduleName + '\'' +
                ", createdAt=" + getCreatedAt() +
                ", updatedAt=" + getUpdatedAt() +
                '}';
    }
}