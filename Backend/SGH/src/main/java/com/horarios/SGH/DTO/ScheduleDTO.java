package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * DTO para gestión de horarios académicos del sistema SGH.
 * Extiende AbstractDTO implementando el patrón Abstract Factory
 * con validaciones específicas para horarios escolares.
 *
 * Proporciona conversión automática entre formatos string y LocalTime,
 * validaciones de integridad horaria y métodos de utilidad.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar horarios
 * - OCP: Abierto para extensión
 * - LSP: Sustituye a AbstractDTO
 *
 * Patrones de diseño aplicados:
 * - Abstract Factory: Implementado a través de AbstractDTO
 * - Factory Method: Para creación de instancias
 *
 * @author Sistema SGH
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO para la gestión de horarios de cursos")
public class ScheduleDTO extends AbstractDTO {

    @Schema(description = "ID único del horario", example = "1")
    private Integer id;

    @NotNull(message = "El ID del curso es obligatorio")
    @Schema(description = "ID del curso al que pertenece el horario", example = "1", required = true)
    private Integer courseId;

    @NotNull(message = "El ID del profesor es obligatorio")
    @Schema(description = "ID del profesor (obligatorio)", example = "5", required = true)
    private Integer teacherId;

    @NotNull(message = "El ID de la materia es obligatorio")
    @Schema(description = "ID de la materia (obligatorio)", example = "3", required = true)
    private Integer subjectId;

    @NotBlank(message = "El día de la semana es obligatorio")
    @Pattern(regexp = "^(Lunes|Martes|Miércoles|Jueves|Viernes|Sábado|Domingo)$", message = "El día debe ser un día válido de la semana")
    @Schema(description = "Día de la semana", allowableValues = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes"}, example = "Lunes", required = true)
    private String day;

    @NotBlank(message = "La hora de inicio es obligatoria")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "La hora de inicio debe tener formato HH:mm válido")
    @Schema(description = "Hora de inicio del horario (formato HH:mm)", example = "08:00", required = true)
    private String startTime;

    @NotBlank(message = "La hora de fin es obligatoria")
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "La hora de fin debe tener formato HH:mm válido")
    @Schema(description = "Hora de fin del horario (formato HH:mm)", example = "09:00", required = true)
    private String endTime;

    @NotBlank(message = "El nombre del horario es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre del horario debe tener entre 3 y 100 caracteres")
    @Schema(description = "Nombre descriptivo del horario", example = "Matemáticas - Juan Pérez", required = true)
    private String scheduleName;

    @Schema(description = "Nombre del profesor (calculado automáticamente)", example = "Juan Pérez", accessMode = Schema.AccessMode.READ_ONLY)
    private String teacherName; // derivado

    @Schema(description = "Nombre de la materia (calculado automáticamente)", example = "Matemáticas", accessMode = Schema.AccessMode.READ_ONLY)
    private String subjectName; // derivado


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    // Método auxiliar para obtener LocalTime
    @JsonIgnore
    public LocalTime getStartTimeAsLocalTime() {
        return startTime != null ? LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm")) : null;
    }

    // Método auxiliar para setear desde LocalTime
    @JsonIgnore
    public void setStartTimeFromLocalTime(LocalTime startTime) {
        this.startTime = startTime != null ? startTime.format(DateTimeFormatter.ofPattern("HH:mm")) : null;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    // Método auxiliar para obtener LocalTime
    @JsonIgnore
    public LocalTime getEndTimeAsLocalTime() {
        return endTime != null ? LocalTime.parse(endTime, DateTimeFormatter.ofPattern("HH:mm")) : null;
    }

    // Método auxiliar para setear desde LocalTime
    @JsonIgnore
    public void setEndTimeFromLocalTime(LocalTime endTime) {
        this.endTime = endTime != null ? endTime.format(DateTimeFormatter.ofPattern("HH:mm")) : null;
    }

    public String getScheduleName() {
        return scheduleName;
    }

    public void setScheduleName(String scheduleName) {
        this.scheduleName = scheduleName;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    /**
     * Constructor por defecto.
     */
    public ScheduleDTO() {
        super();
    }

    /**
     * Constructor con parámetros principales.
     *
     * @param courseId ID del curso
     * @param teacherId ID del profesor
     * @param subjectId ID de la materia
     * @param day Día de la semana
     * @param startTime Hora de inicio (HH:mm)
     * @param endTime Hora de fin (HH:mm)
     * @param scheduleName Nombre del horario
     */
    public ScheduleDTO(Integer courseId, Integer teacherId, Integer subjectId,
                      String day, String startTime, String endTime, String scheduleName) {
        super();
        this.courseId = courseId;
        this.teacherId = teacherId;
        this.subjectId = subjectId;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.scheduleName = scheduleName;
    }

    /**
     * Método Factory para crear un ScheduleDTO básico.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param courseId ID del curso
     * @param teacherId ID del profesor
     * @param subjectId ID de la materia
     * @param day Día de la semana
     * @param startTime Hora de inicio (HH:mm)
     * @param endTime Hora de fin (HH:mm)
     * @param scheduleName Nombre del horario
     * @return ScheduleDTO configurado
     */
    public static ScheduleDTO create(Integer courseId, Integer teacherId, Integer subjectId,
                                   String day, String startTime, String endTime, String scheduleName) {
        ScheduleDTO dto = new ScheduleDTO();
        dto.setCourseId(courseId);
        dto.setTeacherId(teacherId);
        dto.setSubjectId(subjectId);
        dto.setDay(day);
        dto.setStartTime(startTime);
        dto.setEndTime(endTime);
        dto.setScheduleName(scheduleName);
        return dto;
    }

    /**
     * Método Factory para crear un ScheduleDTO vacío.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @return ScheduleDTO con valores por defecto
     */
    public static ScheduleDTO empty() {
        return new ScheduleDTO();
    }

    /**
     * Valida si el horario tiene una duración válida.
     * La hora de fin debe ser posterior a la hora de inicio.
     *
     * @return true si la duración es válida
     */
    public boolean hasValidDuration() {
        if (startTime == null || endTime == null) {
            return false;
        }
        LocalTime start = getStartTimeAsLocalTime();
        LocalTime end = getEndTimeAsLocalTime();
        return start != null && end != null && end.isAfter(start);
    }

    /**
     * Calcula la duración del horario en minutos.
     *
     * @return Duración en minutos, o 0 si no es válida
     */
    public int getDurationMinutes() {
        if (!hasValidDuration()) {
            return 0;
        }
        LocalTime start = getStartTimeAsLocalTime();
        LocalTime end = getEndTimeAsLocalTime();
        return end.toSecondOfDay() / 60 - start.toSecondOfDay() / 60;
    }

    /**
     * Valida si el DTO tiene todos los campos obligatorios.
     * Método de validación de negocio.
     *
     * @return true si todos los campos obligatorios están presentes y válidos
     */
    public boolean isValid() {
        return courseId != null && courseId > 0 &&
               teacherId != null && teacherId > 0 &&
               subjectId != null && subjectId > 0 &&
               day != null && !day.trim().isEmpty() &&
               startTime != null && !startTime.trim().isEmpty() &&
               endTime != null && !endTime.trim().isEmpty() &&
               scheduleName != null && !scheduleName.trim().isEmpty() &&
               hasValidDuration();
    }

    /**
     * Obtiene una representación resumida del horario.
     * Formato: "[scheduleName] - [day] [startTime]-[endTime]"
     *
     * @return Representación resumida
     */
    public String getSummary() {
        return String.format("%s - %s %s-%s",
                scheduleName != null ? scheduleName : "Sin nombre",
                day != null ? day : "Sin día",
                startTime != null ? startTime : "--:--",
                endTime != null ? endTime : "--:--");
    }

    /**
     * Verifica si el horario se solapa con otro horario en el mismo día.
     *
     * @param other Otro horario a comparar
     * @return true si hay solapamiento
     */
    public boolean overlapsWith(ScheduleDTO other) {
        if (other == null || !day.equals(other.day)) {
            return false;
        }

        LocalTime thisStart = getStartTimeAsLocalTime();
        LocalTime thisEnd = getEndTimeAsLocalTime();
        LocalTime otherStart = other.getStartTimeAsLocalTime();
        LocalTime otherEnd = other.getEndTimeAsLocalTime();

        if (thisStart == null || thisEnd == null || otherStart == null || otherEnd == null) {
            return false;
        }

        // Hay solapamiento si un horario no termina antes de que el otro comience
        return !(thisEnd.isBefore(otherStart) || thisStart.isAfter(otherEnd));
    }
}