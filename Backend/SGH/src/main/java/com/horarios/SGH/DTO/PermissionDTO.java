package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO para gestión de permisos del sistema SGH.
 * Implementa validaciones de negocio y métodos de utilidad
 * para el manejo de permisos de seguridad.
 *
 * Proporciona métodos Factory para crear permisos comunes
 * y validaciones específicas para nombres de permisos.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO para gestión de permisos del sistema")
public class PermissionDTO extends AbstractDTO {

    /**
     * Identificador único del permiso.
     */
    @Schema(description = "ID único del permiso", example = "1")
    private Long permissionId;

    /**
     * Nombre único del permiso en formato kebab-case.
     */
    @NotNull(message = "El nombre del permiso es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre del permiso debe tener entre 3 y 100 caracteres")
    @Schema(description = "Nombre único del permiso", example = "READ_SCHEDULE")
    private String permissionName;

    /**
     * Descripción detallada del permiso.
     */
    @Size(max = 255, message = "La descripción debe tener máximo 255 caracteres")
    @Schema(description = "Descripción del permiso", example = "Permite leer horarios académicos")
    private String description;

    /**
     * Estado de activación del permiso.
     */
    @Schema(description = "Indica si el permiso está activo", example = "true")
    private boolean isActive = true;

    /**
     * Timestamp de creación del permiso.
     */
    @Schema(description = "Fecha de creación", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización.
     */
    @Schema(description = "Fecha de última actualización", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    /**
     * Método Factory para crear un permiso básico.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param permissionName nombre del permiso
     * @param description descripción del permiso
     * @return PermissionDTO configurado
     */
    public static PermissionDTO create(String permissionName, String description) {
        PermissionDTO dto = new PermissionDTO();
        dto.setPermissionName(permissionName);
        dto.setDescription(description);
        dto.setActive(true);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear un permiso de lectura.
     *
     * @param resource nombre del recurso
     * @return PermissionDTO para lectura
     */
    public static PermissionDTO createReadPermission(String resource) {
        return create("READ_" + resource.toUpperCase(), "Permite leer " + resource.toLowerCase());
    }

    /**
     * Método Factory para crear un permiso de escritura.
     *
     * @param resource nombre del recurso
     * @return PermissionDTO para escritura
     */
    public static PermissionDTO createWritePermission(String resource) {
        return create("WRITE_" + resource.toUpperCase(), "Permite escribir " + resource.toLowerCase());
    }

    /**
     * Método Factory para crear un permiso de eliminación.
     *
     * @param resource nombre del recurso
     * @return PermissionDTO para eliminación
     */
    public static PermissionDTO createDeletePermission(String resource) {
        return create("DELETE_" + resource.toUpperCase(), "Permite eliminar " + resource.toLowerCase());
    }

    /**
     * Método Factory para crear un PermissionDTO vacío.
     * Útil para inicialización o pruebas.
     *
     * @return PermissionDTO con valores por defecto
     */
    public static PermissionDTO empty() {
        PermissionDTO dto = new PermissionDTO();
        dto.setActive(true);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Verifica si el nombre del permiso tiene formato válido.
     * Los permisos deben estar en mayúsculas y usar guiones bajos.
     *
     * @return true si el formato es válido
     */
    public boolean hasValidFormat() {
        return permissionName != null && permissionName.matches("^[A-Z][A-Z0-9_]*$");
    }

    /**
     * Obtiene el tipo de operación del permiso (READ, WRITE, DELETE, etc.).
     *
     * @return tipo de operación o null si no se puede determinar
     */
    public String getOperationType() {
        if (permissionName == null) {
            return null;
        }

        if (permissionName.startsWith("READ_")) {
            return "READ";
        } else if (permissionName.startsWith("WRITE_")) {
            return "WRITE";
        } else if (permissionName.startsWith("DELETE_")) {
            return "DELETE";
        } else if (permissionName.startsWith("UPDATE_")) {
            return "UPDATE";
        } else if (permissionName.startsWith("CREATE_")) {
            return "CREATE";
        }

        return "CUSTOM";
    }

    /**
     * Obtiene el recurso al que aplica el permiso.
     *
     * @return nombre del recurso o null si no se puede determinar
     */
    public String getResource() {
        if (permissionName == null) {
            return null;
        }

        String[] parts = permissionName.split("_", 2);
        return parts.length > 1 ? parts[1] : null;
    }

    /**
     * Valida si el DTO tiene todos los campos obligatorios.
     * Método de validación de negocio.
     *
     * @return true si todos los campos obligatorios están presentes y válidos
     */
    @Override
    public boolean isValid() {
        return permissionName != null && !permissionName.trim().isEmpty() &&
               hasValidFormat();
    }

    /**
     * Obtiene una representación resumida del permiso.
     * Formato: "[permissionName] - [description]"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        return String.format("%s - %s",
                permissionName != null ? permissionName : "Sin nombre",
                description != null ? description : "Sin descripción");
    }

    // Getters y Setters
    public Long getPermissionId() {
        return permissionId;
    }

    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
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