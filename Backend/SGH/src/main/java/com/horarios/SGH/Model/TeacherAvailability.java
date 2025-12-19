package com.horarios.SGH.Model;

import jakarta.persistence.*;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Entidad que representa la disponibilidad horaria de un docente en el sistema SGH.
 * Define los horarios en los que un profesor está disponible para impartir clases,
 * incluyendo turnos de mañana y tarde para cada día de la semana.
 *
 * Extiende AbstractEntity para funcionalidades comunes como timestamps,
 * validación y operaciones estándar de entidades.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar disponibilidad de docentes
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
@Entity
@Table(name = "TeacherAvailability", uniqueConstraints = @UniqueConstraint(columnNames = { "teacher_id", "day" }))
@Schema(description = "Entidad de disponibilidad de profesor")
public class TeacherAvailability extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único de la disponibilidad", example = "1")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "teacher_id", nullable = false)
    @Schema(description = "Profesor asociado")
    private teachers teacher;

    @Enumerated(EnumType.STRING)
    @Column(name = "day", nullable = false)
    @Schema(description = "Día de la semana", example = "Lunes")
    private Days day;

    @Column(columnDefinition = "TIME")
    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Hora de inicio de la mañana", example = "08:00", type = "string", format = "time")
    private LocalTime amStart;

    @Column(columnDefinition = "TIME")
    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Hora de fin de la mañana", example = "12:00", type = "string", format = "time")
    private LocalTime amEnd;

    @Column(columnDefinition = "TIME")
    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Hora de inicio de la tarde", example = "14:00", type = "string", format = "time")
    private LocalTime pmStart;

    @Column(columnDefinition = "TIME")
    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "Hora de fin de la tarde", example = "18:00", type = "string", format = "time")
    private LocalTime pmEnd;

    /**
     * Constructor vacío requerido por JPA.
     */
    public TeacherAvailability() {
        super();
    }

    /**
     * Constructor con parámetros para crear una disponibilidad de profesor.
     *
     * @param teacher profesor al que pertenece la disponibilidad
     * @param day día de la semana
     * @param amStart hora de inicio del turno mañana
     * @param amEnd hora de fin del turno mañana
     * @param pmStart hora de inicio del turno tarde
     * @param pmEnd hora de fin del turno tarde
     */
    public TeacherAvailability(teachers teacher, Days day, LocalTime amStart, LocalTime amEnd,
                              LocalTime pmStart, LocalTime pmEnd) {
        super();
        this.teacher = teacher;
        this.day = day;
        this.amStart = amStart != null ? amStart.withSecond(0) : null;
        this.amEnd = amEnd != null ? amEnd.withSecond(0) : null;
        this.pmStart = pmStart != null ? pmStart.withSecond(0) : null;
        this.pmEnd = pmEnd != null ? pmEnd.withSecond(0) : null;
    }

    /**
     * Verifica si hay al menos un horario válido definido (mañana o tarde).
     * Un horario es válido cuando tanto la hora de inicio como la de fin están definidas.
     *
     * @return true si existe al menos un horario válido
     */
    public boolean hasValidSchedule() {
        return (amStart != null && amEnd != null) || (pmStart != null && pmEnd != null);
    }

    /**
     * Verifica si tiene horario válido en la mañana.
     *
     * @return true si tiene horario mañana válido
     */
    public boolean hasValidMorningSchedule() {
        return amStart != null && amEnd != null && amStart.isBefore(amEnd);
    }

    /**
     * Verifica si tiene horario válido en la tarde.
     *
     * @return true si tiene horario tarde válido
     */
    public boolean hasValidAfternoonSchedule() {
        return pmStart != null && pmEnd != null && pmStart.isBefore(pmEnd);
    }

    /**
     * Valida si la entidad tiene información básica completa.
     * Método de validación de negocio.
     */
    @Override
    public void validate() {
        if (teacher == null) {
            throw new IllegalArgumentException("El profesor no puede ser nulo");
        }
        if (day == null) {
            throw new IllegalArgumentException("El día no puede ser nulo");
        }
        if (!hasValidSchedule()) {
            throw new IllegalArgumentException("Debe tener al menos un horario válido (mañana o tarde)");
        }
    }

    /**
     * Verifica si la entidad es nueva (no persistida).
     * Una entidad es nueva si no tiene ID asignado.
     *
     * @return true si es una nueva entidad
     */
    @Override
    public boolean isNew() {
        return id == null;
    }

    /**
     * Obtiene una representación resumida de la disponibilidad.
     * Formato: "Disponibilidad [id] - [teacherName] - [day]"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        String teacherName = teacher != null ? teacher.getTeacherName() : "Sin profesor";
        return String.format("Disponibilidad %d - %s - %s",
                id != null ? id : 0,
                teacherName,
                day != null ? day.name() : "Sin día");
    }

    /**
     * Representación en string de la disponibilidad de profesor.
     *
     * @return string con información detallada de la disponibilidad
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TeacherAvailability{")
          .append("id=").append(id)
          .append(", teacher=").append(teacher != null ? teacher.getTeacherName() : "null")
          .append(", day=").append(day)
          .append(", morning=");
        
        if (hasValidMorningSchedule()) {
            sb.append(amStart).append("-").append(amEnd);
        } else {
            sb.append("Sin horario");
        }
        
        sb.append(", afternoon=");
        if (hasValidAfternoonSchedule()) {
            sb.append(pmStart).append("-").append(pmEnd);
        } else {
            sb.append("Sin horario");
        }
        sb.append("} ");
        
        // Agregar información de timestamps si están disponibles
        if (getCreatedAt() != null) {
            sb.append("Creado: ").append(getCreatedAt());
        }
        
        return sb.toString();
    }

    // Getters y Setters
    /**
     * Obtiene el identificador único de la disponibilidad.
     *
     * @return ID de la disponibilidad
     */
    public Long getId() {
        return id;
    }

    /**
     * Establece el identificador único de la disponibilidad.
     *
     * @param id ID de la disponibilidad
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Obtiene el profesor al que pertenece esta disponibilidad.
     *
     * @return profesor asociado
     */
    public teachers getTeacher() {
        return teacher;
    }

    /**
     * Establece el profesor al que pertenece esta disponibilidad.
     *
     * @param teacher profesor asociado
     */
    public void setTeacher(teachers teacher) {
        this.teacher = teacher;
    }

    /**
     * Obtiene el día de la semana de la disponibilidad.
     *
     * @return día de la semana
     */
    public Days getDay() {
        return day;
    }

    /**
     * Establece el día de la semana de la disponibilidad.
     *
     * @param day día de la semana
     */
    public void setDay(Days day) {
        this.day = day;
    }

    /**
     * Obtiene la hora de inicio del turno mañana.
     *
     * @return hora de inicio mañana
     */
    public LocalTime getAmStart() {
        return amStart;
    }

    /**
     * Establece la hora de inicio del turno mañana.
     *
     * @param amStart hora de inicio mañana
     */
    public void setAmStart(LocalTime amStart) {
        this.amStart = amStart;
    }

    /**
     * Obtiene la hora de fin del turno mañana.
     *
     * @return hora de fin mañana
     */
    public LocalTime getAmEnd() {
        return amEnd;
    }

    /**
     * Establece la hora de fin del turno mañana.
     *
     * @param amEnd hora de fin mañana
     */
    public void setAmEnd(LocalTime amEnd) {
        this.amEnd = amEnd;
    }

    /**
     * Obtiene la hora de inicio del turno tarde.
     *
     * @return hora de inicio tarde
     */
    public LocalTime getPmStart() {
        return pmStart;
    }

    /**
     * Establece la hora de inicio del turno tarde.
     *
     * @param pmStart hora de inicio tarde
     */
    public void setPmStart(LocalTime pmStart) {
        this.pmStart = pmStart;
    }

    /**
     * Obtiene la hora de fin del turno tarde.
     *
     * @return hora de fin tarde
     */
    public LocalTime getPmEnd() {
        return pmEnd;
    }

    /**
     * Establece la hora de fin del turno tarde.
     *
     * @param pmEnd hora de fin tarde
     */
    public void setPmEnd(LocalTime pmEnd) {
        this.pmEnd = pmEnd;
    }
}