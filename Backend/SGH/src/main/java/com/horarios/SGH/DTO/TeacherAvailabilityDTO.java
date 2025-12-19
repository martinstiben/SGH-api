package com.horarios.SGH.DTO;

import java.time.LocalDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.horarios.SGH.Model.Days;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO para la disponibilidad de un profesor del sistema SGH.
 * Implementa validaciones de negocio específicas para horarios académicos
 * y métodos de utilidad para gestión de disponibilidad docente.
 *
 * Proporciona métodos Factory para crear disponibilidades
 * y validaciones de solapamiento de horarios.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO para la disponibilidad de un profesor")
public class TeacherAvailabilityDTO extends AbstractDTO {
    @NotNull(message = "El ID del profesor es obligatorio")
    @Schema(description = "ID del profesor", example = "1")
    private Integer teacherId;

    @NotNull(message = "El día es obligatorio")
    @Pattern(regexp = "^(LUNES|MARTES|MIÉRCOLES|JUEVES|VIERNES|SÁBADO|DOMINGO)$", message = "El día debe ser un día válido de la semana en mayúsculas")
    @Schema(description = "Día de la semana", example = "Lunes")
    private Days day;

    @JsonFormat(pattern = "HH:mm", shape = Shape.STRING)
    @Schema(description = "Hora de inicio de la mañana", example = "08:00", type = "string", format = "time")
    private LocalTime amStart;

    @JsonFormat(pattern = "HH:mm", shape = Shape.STRING)
    @Schema(description = "Hora de fin de la mañana", example = "12:00", type = "string", format = "time")
    private LocalTime amEnd;

    @JsonFormat(pattern = "HH:mm", shape = Shape.STRING)
    @Schema(description = "Hora de inicio de la tarde", example = "14:00", type = "string", format = "time")
    private LocalTime pmStart;

    @JsonFormat(pattern = "HH:mm", shape = Shape.STRING)
    @Schema(description = "Hora de fin de la tarde", example = "18:00", type = "string", format = "time")
    private LocalTime pmEnd;

    /**
     * Timestamp de creación del registro.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización.
     */
    private LocalDateTime updatedAt;

    public TeacherAvailabilityDTO() {
        super();
        this.createdAt = LocalDateTime.now();
    }

    public TeacherAvailabilityDTO(Integer teacherId, Days day, LocalTime amStart, LocalTime amEnd, LocalTime pmStart, LocalTime pmEnd) {
        super();
        this.teacherId = teacherId;
        this.day = day;
        this.amStart = amStart != null ? amStart.withSecond(0) : null;
        this.amEnd = amEnd != null ? amEnd.withSecond(0): null;
        this.pmStart = pmStart != null ? pmStart.withSecond(0): null;
        this.pmEnd = pmEnd != null ? pmEnd.withSecond(0) : null;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Método Factory para crear una disponibilidad básica.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param teacherId ID del profesor
     * @param day Día de la semana
     * @param amStart Hora de inicio mañana
     * @param amEnd Hora de fin mañana
     * @param pmStart Hora de inicio tarde
     * @param pmEnd Hora de fin tarde
     * @return TeacherAvailabilityDTO configurado
     */
    public static TeacherAvailabilityDTO create(Integer teacherId, Days day, LocalTime amStart, LocalTime amEnd, LocalTime pmStart, LocalTime pmEnd) {
        TeacherAvailabilityDTO dto = new TeacherAvailabilityDTO();
        dto.setTeacherId(teacherId);
        dto.setDay(day);
        dto.setAmStart(amStart);
        dto.setAmEnd(amEnd);
        dto.setPmStart(pmStart);
        dto.setPmEnd(pmEnd);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear un TeacherAvailabilityDTO vacío.
     * Útil para inicialización o pruebas.
     *
     * @return TeacherAvailabilityDTO con valores por defecto
     */
    public static TeacherAvailabilityDTO empty() {
        TeacherAvailabilityDTO dto = new TeacherAvailabilityDTO();
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
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
     * Verifica si tiene algún horario válido.
     *
     * @return true si tiene al menos un horario válido
     */
    public boolean hasValidSchedule() {
        return hasValidMorningSchedule() || hasValidAfternoonSchedule();
    }

    /**
     * Verifica si los horarios se solapan.
     *
     * @return true si hay solapamiento entre mañana y tarde
     */
    public boolean hasScheduleOverlap() {
        if (!hasValidMorningSchedule() || !hasValidAfternoonSchedule()) {
            return false;
        }
        return amEnd.isAfter(pmStart);
    }

    /**
     * Obtiene la duración total en horas del horario disponible.
     *
     * @return duración total en horas
     */
    public double getTotalHours() {
        double morningHours = hasValidMorningSchedule() ?
                amStart.until(amEnd, java.time.temporal.ChronoUnit.MINUTES) / 60.0 : 0;
        double afternoonHours = hasValidAfternoonSchedule() ?
                pmStart.until(pmEnd, java.time.temporal.ChronoUnit.MINUTES) / 60.0 : 0;
        return morningHours + afternoonHours;
    }

    /**
     * Verifica si el día es válido (lunes a viernes).
     *
     * @return true si es un día laboral válido
     */
    public boolean isValidWorkDay() {
        if (day == null) {
            return false;
        }
        Days[] validDays = {Days.Lunes, Days.Martes, Days.Miércoles, Days.Jueves, Days.Viernes};
        for (Days validDay : validDays) {
            if (validDay.equals(day)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Valida si el DTO tiene información básica completa.
     * Método de validación de negocio.
     *
     * @return true si tiene la información esencial
     */
    @Override
    public boolean isValid() {
        return teacherId != null && teacherId > 0 &&
               day != null &&
               hasValidSchedule() &&
               !hasScheduleOverlap();
    }

    /**
     * Obtiene una representación resumida de la disponibilidad.
     * Formato: "Disponibilidad [teacherId] - [day] - [horarios]"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        return String.format("Disponibilidad %d - %s - %s",
                teacherId != null ? teacherId : 0,
                day != null ? day.toString() : "Sin día",
                getFormattedSchedule());
    }

    /**
     * Obtiene una representación formateada del horario.
     *
     * @return horario formateado
     */
    public String getFormattedSchedule() {
        StringBuilder sb = new StringBuilder();
        sb.append(day).append(": ");

        if (hasValidMorningSchedule()) {
            sb.append(amStart.toString()).append("-").append(amEnd.toString());
        }

        if (hasValidMorningSchedule() && hasValidAfternoonSchedule()) {
            sb.append(", ");
        }

        if (hasValidAfternoonSchedule()) {
            sb.append(pmStart.toString()).append("-").append(pmEnd.toString());
        }

        return sb.toString();
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}