package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO para gestión de cursos académicos del sistema SGH.
 * Extiende AbstractDTO implementando el patrón Abstract Factory
 * con validaciones específicas para cursos escolares.
 *
 * Proporciona métodos de utilidad para validación y manipulación
 * de información de cursos con directores de grado opcionales.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar cursos
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
@Schema(description = "DTO para gestión de cursos académicos")
public class CourseDTO extends AbstractDTO {

    /**
     * Identificador único del curso en el sistema.
     * Generado automáticamente por la base de datos.
     */
    @Schema(description = "ID único del curso", example = "1")
    private int courseId;

    /**
     * Nombre del curso (ej: "1A", "2B", "10C").
     * Debe tener máximo 2 caracteres alfanuméricos.
     */
    @NotNull(message = "El nombre del curso no puede ser nulo")
    @NotBlank(message = "El nombre del curso no puede estar vacío")
    @Size(min = 1, max = 2, message = "El nombre del curso solo puede tener dos caracteres, ejemplo: 1A")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ0-9\\s]+$", message = "El nombre del curso solo puede contener letras, números y espacios")
    @Schema(description = "Nombre del curso (ej: 1A, 2B)", example = "1A", required = true)
    private String courseName;

    /**
     * ID del docente que actúa como director de grado.
     * Campo opcional - puede ser null si no hay director asignado.
     */
    @Schema(description = "ID del director de grado (opcional)", example = "5")
    private Integer gradeDirectorId;

    /**
     * Timestamp de creación del registro.
     */
    @Schema(description = "Fecha de creación", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización.
     */
    @Schema(description = "Fecha de última actualización", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    /**
     * Constructor por defecto.
     */
    public CourseDTO() {
        super();
    }

    /**
     * Constructor con parámetros principales.
     *
     * @param courseId ID del curso
     * @param courseName Nombre del curso
     */
    public CourseDTO(int courseId, String courseName) {
        super();
        this.courseId = courseId;
        this.courseName = courseName;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor completo.
     *
     * @param courseId ID del curso
     * @param courseName Nombre del curso
     * @param gradeDirectorId ID del director de grado
     */
    public CourseDTO(int courseId, String courseName, Integer gradeDirectorId) {
        super();
        this.courseId = courseId;
        this.courseName = courseName;
        this.gradeDirectorId = gradeDirectorId;
        this.createdAt = LocalDateTime.now();
    }

    // Getters y Setters
    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Integer getGradeDirectorId() {
        return gradeDirectorId;
    }

    public void setGradeDirectorId(Integer gradeDirectorId) {
        this.gradeDirectorId = gradeDirectorId;
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
     * Método Factory para crear un CourseDTO básico.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param courseId ID del curso
     * @param courseName Nombre del curso
     * @return CourseDTO configurado
     */
    public static CourseDTO create(int courseId, String courseName) {
        CourseDTO dto = new CourseDTO();
        dto.setCourseId(courseId);
        dto.setCourseName(courseName);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear un CourseDTO con director de grado.
     *
     * @param courseId ID del curso
     * @param courseName Nombre del curso
     * @param gradeDirectorId ID del director de grado
     * @return CourseDTO con director asignado
     */
    public static CourseDTO createWithDirector(int courseId, String courseName, Integer gradeDirectorId) {
        CourseDTO dto = new CourseDTO();
        dto.setCourseId(courseId);
        dto.setCourseName(courseName);
        dto.setGradeDirectorId(gradeDirectorId);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear un CourseDTO vacío.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @return CourseDTO con valores por defecto
     */
    public static CourseDTO empty() {
        CourseDTO dto = new CourseDTO();
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Valida si el curso tiene un director de grado asignado.
     *
     * @return true si tiene director asignado
     */
    public boolean hasGradeDirector() {
        return gradeDirectorId != null && gradeDirectorId > 0;
    }

    /**
     * Extrae el grado del nombre del curso.
     * Ejemplo: "1A" -> 1, "10B" -> 10
     *
     * @return Número del grado, o -1 si no es válido
     */
    public int getGradeLevel() {
        if (courseName == null || courseName.trim().isEmpty()) {
            return -1;
        }

        try {
            // Extraer números del inicio del nombre
            String numericPart = courseName.replaceAll("[^0-9]", "");
            return numericPart.isEmpty() ? -1 : Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Extrae la sección del nombre del curso.
     * Ejemplo: "1A" -> "A", "10B" -> "B"
     *
     * @return Letra de la sección, o cadena vacía si no es válida
     */
    public String getSection() {
        if (courseName == null || courseName.trim().isEmpty()) {
            return "";
        }

        // Extraer letras del final del nombre
        return courseName.replaceAll("[0-9]", "").trim();
    }

    /**
     * Valida si el DTO tiene todos los campos obligatorios.
     * Método de validación de negocio.
     *
     * @return true si todos los campos obligatorios están presentes y válidos
     */
    @Override
    public boolean isValid() {
        return courseName != null && !courseName.trim().isEmpty() &&
                courseName.length() >= 1 && courseName.length() <= 2 &&
                courseName.matches("^[a-zA-ZÀ-ÿ0-9\\s]+$");
    }

    /**
     * Obtiene una representación resumida del curso.
     * Formato: "Curso [courseName]" o "Curso [courseName] (Director: [gradeDirectorId])"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        String summary = "Curso " + (courseName != null ? courseName : "Sin nombre");
        if (hasGradeDirector()) {
            summary += " (Director: " + gradeDirectorId + ")";
        }
        return summary;
    }

    /**
     * Verifica si dos cursos pertenecen al mismo grado.
     *
     * @param other Otro curso a comparar
     * @return true si pertenecen al mismo grado
     */
    public boolean isSameGrade(CourseDTO other) {
        return other != null && this.getGradeLevel() == other.getGradeLevel();
    }
}