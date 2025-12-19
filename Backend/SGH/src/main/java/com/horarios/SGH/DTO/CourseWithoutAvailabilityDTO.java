package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO para representar cursos que no tienen profesores con disponibilidad horaria.
 * Extiende AbstractDTO implementando el patrón Abstract Factory
 * con enumeraciones para categorizar problemas de disponibilidad.
 *
 * Proporciona información detallada sobre por qué un curso no puede
 * ser asignado a un profesor debido a restricciones de horario.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar problemas de disponibilidad
 * - OCP: Abierto para extensión
 * - LSP: Sustituye a AbstractDTO
 *
 * Patrones de diseño aplicados:
 * - Abstract Factory: Implementado a través de AbstractDTO
 * - Factory Method: Para creación de instancias
 * - Enum: Para tipos seguros de razones
 *
 * @author Sistema SGH
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO para representar cursos que no tienen profesores con disponibilidad")
public class CourseWithoutAvailabilityDTO extends AbstractDTO {

    /**
     * Identificador único del curso problemático.
     */
    @Schema(description = "ID del curso", example = "1")
    private Integer courseId;

    /**
     * Nombre descriptivo del curso.
     */
    @Schema(description = "Nombre del curso", example = "Matemáticas 1A")
    private String courseName;

    /**
     * Identificador del profesor asignado al curso.
     */
    @Schema(description = "ID del profesor asignado", example = "5")
    private Integer teacherId;

    /**
     * Nombre completo del profesor asignado.
     */
    @Schema(description = "Nombre del profesor", example = "Juan Pérez")
    private String teacherName;

    /**
     * Categoría del problema de disponibilidad.
     * Define el tipo específico de restricción encontrada.
     */
    @Schema(description = "Razón por la cual no hay disponibilidad",
             allowableValues = {"NO_AVAILABILITY_DEFINED", "CONFLICTS_WITH_EXISTING", "NO_TIME_SLOTS_AVAILABLE"},
             example = "NO_AVAILABILITY_DEFINED")
    private String reason;

    /**
     * Descripción detallada del problema específico.
     * Proporciona contexto adicional sobre la restricción.
     */
    @Schema(description = "Descripción detallada del problema",
             example = "El profesor Juan Pérez no tiene disponibilidad configurada para ningún día de la semana")
    private String description;

    /**
     * Timestamp cuando se detectó el problema.
     */
    @Schema(description = "Fecha de detección del problema", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime detectedAt;

    /**
     * Timestamp de creación del registro.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización.
     */
    private LocalDateTime updatedAt;

    /**
     * Severidad del problema (LOW, MEDIUM, HIGH, CRITICAL).
     */
    @Schema(description = "Severidad del problema", example = "HIGH",
             allowableValues = {"LOW", "MEDIUM", "HIGH", "CRITICAL"})
    private String severity;

    /**
     * Sugerencias para resolver el problema.
     */
    @Schema(description = "Sugerencias para resolver el problema",
             example = "Configure la disponibilidad horaria del profesor o reasigne el curso")
    private String suggestions;

    /**
     * Enumeración de razones posibles para falta de disponibilidad.
     * Implementa patrón Enum para tipos seguros.
     */
    public enum AvailabilityReason {
        NO_AVAILABILITY_DEFINED("El profesor no tiene disponibilidad configurada"),
        CONFLICTS_WITH_EXISTING("Conflicto con horarios existentes"),
        NO_TIME_SLOTS_AVAILABLE("No hay franjas horarias disponibles"),
        TEACHER_OVERLOADED("El profesor tiene demasiadas asignaturas"),
        INVALID_TIME_FORMAT("Formato de tiempo inválido");

        private final String description;

        AvailabilityReason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Método Factory para crear un CourseWithoutAvailabilityDTO básico.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param courseId ID del curso
     * @param courseName Nombre del curso
     * @param teacherId ID del profesor
     * @param teacherName Nombre del profesor
     * @param reason Razón del problema
     * @param description Descripción detallada
     * @return CourseWithoutAvailabilityDTO configurado
     */
    public static CourseWithoutAvailabilityDTO create(Integer courseId, String courseName,
                                                      Integer teacherId, String teacherName,
                                                      String reason, String description) {
        CourseWithoutAvailabilityDTO dto = new CourseWithoutAvailabilityDTO();
        dto.setCourseId(courseId);
        dto.setCourseName(courseName);
        dto.setTeacherId(teacherId);
        dto.setTeacherName(teacherName);
        dto.setReason(reason);
        dto.setDescription(description);
        dto.setDetectedAt(LocalDateTime.now());
        dto.setSeverity("HIGH");
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear un DTO con sugerencias.
     *
     * @param courseId ID del curso
     * @param courseName Nombre del curso
     * @param teacherId ID del profesor
     * @param teacherName Nombre del profesor
     * @param reason Razón del problema
     * @param description Descripción detallada
     * @param suggestions Sugerencias para resolver
     * @return CourseWithoutAvailabilityDTO con sugerencias
     */
    public static CourseWithoutAvailabilityDTO createWithSuggestions(Integer courseId, String courseName,
                                                                    Integer teacherId, String teacherName,
                                                                    String reason, String description,
                                                                    String suggestions) {
        CourseWithoutAvailabilityDTO dto = new CourseWithoutAvailabilityDTO();
        dto.setCourseId(courseId);
        dto.setCourseName(courseName);
        dto.setTeacherId(teacherId);
        dto.setTeacherName(teacherName);
        dto.setReason(reason);
        dto.setDescription(description);
        dto.setSuggestions(suggestions);
        dto.setDetectedAt(LocalDateTime.now());
        dto.setSeverity("MEDIUM");
        return dto;
    }

    /**
     * Método Factory para crear un CourseWithoutAvailabilityDTO vacío.
     * Útil para respuestas vacías o inicialización.
     *
     * @return CourseWithoutAvailabilityDTO con valores por defecto
     */
    public static CourseWithoutAvailabilityDTO empty() {
        CourseWithoutAvailabilityDTO dto = new CourseWithoutAvailabilityDTO();
        dto.setDetectedAt(LocalDateTime.now());
        dto.setSeverity("LOW");
        return dto;
    }

    // Getters y Setters
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

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(String suggestions) {
        this.suggestions = suggestions;
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

    /**
     * Verifica si el problema es crítico.
     *
     * @return true si requiere atención inmediata
     */
    public boolean isCritical() {
        return "CRITICAL".equals(severity) || "HIGH".equals(severity);
    }

    /**
     * Obtiene la razón como enum tipado.
     *
     * @return AvailabilityReason correspondiente o null si no es válido
     */
    public AvailabilityReason getReasonAsEnum() {
        if (reason == null) {
            return null;
        }
        try {
            return AvailabilityReason.valueOf(reason);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Valida si el DTO tiene información básica completa.
     * Método de validación de negocio.
     *
     * @return true si tiene la información esencial
     */
    @Override
    public boolean isValid() {
        return courseId != null && courseId > 0 &&
                courseName != null && !courseName.trim().isEmpty() &&
                teacherId != null && teacherId > 0 &&
                teacherName != null && !teacherName.trim().isEmpty() &&
                reason != null && !reason.trim().isEmpty();
    }

    /**
     * Obtiene una representación resumida del problema.
     * Formato: "Curso [courseName]: [reason] - [teacherName]"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        return String.format("Curso %s: %s - %s",
                courseName != null ? courseName : "Sin nombre",
                reason != null ? reason : "Sin razón",
                teacherName != null ? teacherName : "Sin profesor");
    }

    /**
     * Genera sugerencias automáticas basadas en la razón.
     * Método de utilidad para proporcionar ayuda automática.
     */
    public void generateSuggestions() {
        if (suggestions != null && !suggestions.trim().isEmpty()) {
            return; // Ya tiene sugerencias
        }

        AvailabilityReason reasonEnum = getReasonAsEnum();
        if (reasonEnum != null) {
            switch (reasonEnum) {
                case NO_AVAILABILITY_DEFINED:
                    suggestions = "Configure la disponibilidad horaria del profesor en la sección de perfil docente.";
                    break;
                case CONFLICTS_WITH_EXISTING:
                    suggestions = "Revise los horarios existentes del profesor y ajuste los conflictos.";
                    break;
                case NO_TIME_SLOTS_AVAILABLE:
                    suggestions = "Considere cambiar el día de la semana o buscar otro profesor disponible.";
                    break;
                case TEACHER_OVERLOADED:
                    suggestions = "Reduzca la carga horaria del profesor o reasigne algunas asignaturas.";
                    break;
                default:
                    suggestions = "Contacte al administrador del sistema para asistencia técnica.";
            }
        } else {
            suggestions = "Contacte al administrador del sistema para resolver este problema.";
        }
    }
}