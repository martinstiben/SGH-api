package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * DTO para gestión de roles del sistema SGH.
 * Implementa validaciones de negocio específicas para roles
 * y métodos de utilidad para gestión de permisos.
 *
 * Proporciona métodos Factory para crear roles comunes
 * y validaciones de nombres de rol.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO para gestión de roles del sistema")
public class RoleDTO extends AbstractDTO {

    /**
     * Identificador único del rol.
     */
    @Schema(description = "ID único del rol", example = "1")
    private Long roleId;

    /**
     * Nombre único del rol en mayúsculas.
     */
    @NotNull(message = "El nombre del rol es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre del rol debe tener entre 3 y 50 caracteres")
    @Schema(description = "Nombre único del rol", example = "ESTUDIANTE")
    private String roleName;

    /**
     * Descripción detallada del rol y sus responsabilidades.
     */
    @Size(max = 255, message = "La descripción debe tener máximo 255 caracteres")
    @Schema(description = "Descripción del rol", example = "Rol para estudiantes del sistema")
    private String description;

    /**
     * Estado de activación del rol.
     */
    @Schema(description = "Indica si el rol está activo", example = "true")
    private boolean isActive = true;

    /**
     * Timestamp de creación del rol.
     */
    @Schema(description = "Fecha de creación", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización.
     */
    @Schema(description = "Fecha de última actualización", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    /**
     * Método Factory para crear un rol básico.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param roleName nombre del rol
     * @param description descripción del rol
     * @return RoleDTO configurado
     */
    public static RoleDTO create(String roleName, String description) {
        RoleDTO dto = new RoleDTO();
        dto.setRoleName(roleName);
        dto.setDescription(description);
        dto.setActive(true);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear el rol de estudiante.
     *
     * @return RoleDTO para estudiante
     */
    public static RoleDTO createStudentRole() {
        return create("ESTUDIANTE", "Rol para estudiantes del sistema académico");
    }

    /**
     * Método Factory para crear el rol de maestro.
     *
     * @return RoleDTO para maestro
     */
    public static RoleDTO createTeacherRole() {
        return create("MAESTRO", "Rol para profesores del sistema académico");
    }

    /**
     * Método Factory para crear el rol de coordinador.
     *
     * @return RoleDTO para coordinador
     */
    public static RoleDTO createCoordinatorRole() {
        return create("COORDINADOR", "Rol para coordinadores académicos");
    }

    /**
     * Método Factory para crear el rol de director de área.
     *
     * @return RoleDTO para director de área
     */
    public static RoleDTO createDirectorRole() {
        return create("DIRECTOR_DE_AREA", "Rol para directores de área académica");
    }

    /**
     * Método Factory para crear un RoleDTO vacío.
     * Útil para inicialización o pruebas.
     *
     * @return RoleDTO con valores por defecto
     */
    public static RoleDTO empty() {
        RoleDTO dto = new RoleDTO();
        dto.setActive(true);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Verifica si el nombre del rol tiene formato válido.
     * Los roles deben estar en mayúsculas y pueden contener guiones bajos.
     *
     * @return true si el formato es válido
     */
    public boolean hasValidFormat() {
        return roleName != null && roleName.matches("^[A-Z][A-Z0-9_]*$");
    }

    /**
     * Verifica si el rol es un rol de sistema (predefinido).
     *
     * @return true si es un rol de sistema
     */
    public boolean isSystemRole() {
        List<String> systemRoles = Arrays.asList("ESTUDIANTE", "MAESTRO", "COORDINADOR", "DIRECTOR_DE_AREA");
        return roleName != null && systemRoles.contains(roleName.toUpperCase());
    }

    /**
     * Verifica si el rol tiene permisos administrativos.
     *
     * @return true si es un rol administrativo
     */
    public boolean isAdministrativeRole() {
        List<String> adminRoles = Arrays.asList("COORDINADOR", "DIRECTOR_DE_AREA");
        return roleName != null && adminRoles.contains(roleName.toUpperCase());
    }

    /**
     * Verifica si el rol tiene permisos académicos.
     *
     * @return true si es un rol académico
     */
    public boolean isAcademicRole() {
        List<String> academicRoles = Arrays.asList("MAESTRO", "DIRECTOR_DE_AREA");
        return roleName != null && academicRoles.contains(roleName.toUpperCase());
    }

    /**
     * Obtiene el nivel jerárquico del rol.
     * Valores más altos indican mayor jerarquía.
     *
     * @return nivel jerárquico (0-3)
     */
    public int getHierarchyLevel() {
        if (roleName == null) {
            return 0;
        }

        switch (roleName.toUpperCase()) {
            case "ESTUDIANTE":
                return 1;
            case "MAESTRO":
                return 2;
            case "COORDINADOR":
                return 3;
            case "DIRECTOR_DE_AREA":
                return 3;
            default:
                return 0;
        }
    }

    /**
     * Valida si el DTO tiene todos los campos obligatorios.
     * Método de validación de negocio.
     *
     * @return true si todos los campos obligatorios están presentes y válidos
     */
    @Override
    public boolean isValid() {
        return roleName != null && !roleName.trim().isEmpty() &&
               hasValidFormat();
    }

    /**
     * Obtiene una representación resumida del rol.
     * Formato: "[roleName] - [description]"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        return String.format("%s - %s",
                roleName != null ? roleName : "Sin nombre",
                description != null ? description : "Sin descripción");
    }

    // Getters y Setters
    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}