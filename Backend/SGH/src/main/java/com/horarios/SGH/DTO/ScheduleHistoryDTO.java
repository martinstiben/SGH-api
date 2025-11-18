package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "DTO para la generación de horarios por cursos")
public class ScheduleHistoryDTO {

    // Respuesta
    @Schema(description = "ID único del registro de generación", example = "1")
    private Integer id;

    @Schema(description = "Usuario que ejecutó la generación", example = "admin")
    private String executedBy;

    @Schema(description = "Fecha y hora de ejecución", type = "string", format = "date-time", example = "2025-01-06T10:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime executedAt;

    @Schema(description = "Estado de la generación", allowableValues = {"RUNNING", "SUCCESS", "FAILED"}, example = "SUCCESS")
    private String status;

    @Schema(description = "Total de horarios generados", example = "15")
    private int totalGenerated;

    @Schema(description = "Mensaje descriptivo del resultado", example = "Generación completada exitosamente")
    private String message;

    // Petición
    @Schema(description = "Fecha de inicio del período", type = "string", format = "date", example = "2025-01-06", required = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate periodStart;

    @Schema(description = "Fecha de fin del período", type = "string", format = "date", example = "2025-01-10", required = true)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate periodEnd;

    @Schema(description = "Modo simulación (true: solo cuenta, false: genera realmente)", example = "false", required = true)
    private boolean dryRun;

    @Schema(description = "Forzar generación (ignora algunas validaciones)", example = "false", required = true)
    private boolean force;

    @Schema(description = "Parámetros adicionales o descripción", example = "Generación automática de horarios")
    private String params;

    // NUEVA FUNCIONALIDAD: Cursos sin disponibilidad de profesores
    @Schema(description = "Lista de cursos que no pudieron ser asignados por falta de disponibilidad de profesores", 
            implementation = CourseWithoutAvailabilityDTO.class)
    private List<CourseWithoutAvailabilityDTO> coursesWithoutAvailability;

    @Schema(description = "Total de cursos que no pudieron ser asignados", example = "3")
    private int totalCoursesWithoutAvailability;

    public ScheduleHistoryDTO() {}

    public ScheduleHistoryDTO(Integer id, String executedBy, LocalDateTime executedAt, String status,
                              int totalGenerated, String message, LocalDate periodStart, LocalDate periodEnd,
                              boolean dryRun, boolean force, String params) {
        this.id = id;
        this.executedBy = executedBy;
        this.executedAt = executedAt;
        this.status = status;
        this.totalGenerated = totalGenerated;
        this.message = message;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.dryRun = dryRun;
        this.force = force;
        this.params = params;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getExecutedBy() { return executedBy; }
    public void setExecutedBy(String executedBy) { this.executedBy = executedBy; }

    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTotalGenerated() { return totalGenerated; }
    public void setTotalGenerated(int totalGenerated) { this.totalGenerated = totalGenerated; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }

    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }

    public boolean isDryRun() { return dryRun; }
    public void setDryRun(boolean dryRun) { this.dryRun = dryRun; }

    public boolean isForce() { return force; }
    public void setForce(boolean force) { this.force = force; }

    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }

    public List<CourseWithoutAvailabilityDTO> getCoursesWithoutAvailability() {
        return coursesWithoutAvailability;
    }

    public void setCoursesWithoutAvailability(List<CourseWithoutAvailabilityDTO> coursesWithoutAvailability) {
        this.coursesWithoutAvailability = coursesWithoutAvailability;
    }

    public int getTotalCoursesWithoutAvailability() {
        return totalCoursesWithoutAvailability;
    }

    public void setTotalCoursesWithoutAvailability(int totalCoursesWithoutAvailability) {
        this.totalCoursesWithoutAvailability = totalCoursesWithoutAvailability;
    }
}