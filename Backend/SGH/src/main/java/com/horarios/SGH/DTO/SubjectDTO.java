package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO para gestión de asignaturas/materias del sistema SGH.
 * Extiende AbstractDTO implementando el patrón Abstract Factory
 * con validaciones específicas para asignaturas académicas.
 *
 * Proporciona métodos de utilidad para validación y manipulación
 * de información de asignaturas con nombres normalizados.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar asignaturas
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
@Schema(description = "DTO para gestión de asignaturas académicas")
public class SubjectDTO extends AbstractDTO {

    /**
     * Identificador único de la asignatura en el sistema.
     * Generado automáticamente por la base de datos.
     */
    @Schema(description = "ID único de la asignatura", example = "1")
    private int subjectId;

    /**
     * Nombre de la asignatura (ej: "Matemáticas", "Física", "Historia").
     * Debe contener solo letras y espacios, sin números.
     */
    @NotNull(message = "El nombre de la materia no puede ser nulo")
    @NotBlank(message = "El nombre de la materia no puede estar vacío")
    @Size(min = 5, max = 20, message = "El nombre de la materia debe tener entre 5 y 20 caracteres")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s]+$", message = "El nombre de la materia solo puede contener letras y espacios")
    @Schema(description = "Nombre de la asignatura", example = "Matemáticas", required = true)
    private String subjectName;

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
    public SubjectDTO() {
        super();
    }

    /**
     * Constructor con parámetros principales.
     *
     * @param subjectId ID de la asignatura
     * @param subjectName Nombre de la asignatura
     */
    public SubjectDTO(int subjectId, String subjectName) {
        super();
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Método Factory para crear un SubjectDTO básico.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param subjectId ID de la asignatura
     * @param subjectName Nombre de la asignatura
     * @return SubjectDTO configurado
     */
    public static SubjectDTO create(int subjectId, String subjectName) {
        SubjectDTO dto = new SubjectDTO();
        dto.setSubjectId(subjectId);
        dto.setSubjectName(subjectName);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear un SubjectDTO vacío.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @return SubjectDTO con valores por defecto
     */
    public static SubjectDTO empty() {
        SubjectDTO dto = new SubjectDTO();
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    // Getters y Setters
    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
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
     * Obtiene el nombre de la asignatura en mayúsculas.
     * Método de utilidad para presentaciones formales.
     *
     * @return Nombre en mayúsculas
     */
    public String getSubjectNameUpperCase() {
        return subjectName != null ? subjectName.toUpperCase() : null;
    }

    /**
     * Obtiene el nombre de la asignatura capitalizado.
     * Primera letra de cada palabra en mayúscula.
     *
     * @return Nombre capitalizado
     */
    public String getSubjectNameCapitalized() {
        if (subjectName == null || subjectName.trim().isEmpty()) {
            return subjectName;
        }

        String[] words = subjectName.trim().toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(word.substring(0, 1).toUpperCase())
                      .append(word.substring(1))
                      .append(" ");
            }
        }

        return result.toString().trim();
    }

    /**
     * Obtiene las iniciales de la asignatura.
     * Ejemplo: "Matemáticas Avanzadas" -> "MA"
     *
     * @return Iniciales en mayúsculas
     */
    public String getInitials() {
        if (subjectName == null || subjectName.trim().isEmpty()) {
            return "";
        }

        String[] words = subjectName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                initials.append(word.charAt(0));
            }
        }

        return initials.toString().toUpperCase();
    }

    /**
     * Valida si el DTO tiene todos los campos obligatorios.
     * Método de validación de negocio.
     *
     * @return true si todos los campos obligatorios están presentes y válidos
     */
    @Override
    public boolean isValid() {
        return subjectName != null && !subjectName.trim().isEmpty() &&
                subjectName.length() >= 5 && subjectName.length() <= 20 &&
                subjectName.matches("^[a-zA-ZÀ-ÿ\\s]+$");
    }

    /**
     * Obtiene una representación resumida de la asignatura.
     * Formato: "ID: [subjectId] - [subjectName]"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        return String.format("ID: %d - %s", subjectId, subjectName != null ? subjectName : "Sin nombre");
    }

    /**
     * Verifica si el nombre de la asignatura contiene una palabra clave.
     *
     * @param keyword Palabra clave a buscar (case insensitive)
     * @return true si contiene la palabra clave
     */
    public boolean containsKeyword(String keyword) {
        if (subjectName == null || keyword == null) {
            return false;
        }
        return subjectName.toLowerCase().contains(keyword.toLowerCase());
    }
}