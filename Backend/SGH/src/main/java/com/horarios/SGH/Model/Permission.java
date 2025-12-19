package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad que representa un permiso en el sistema SGH.
 * Los permisos definen las acciones específicas que pueden realizar los usuarios,
 * implementando control de acceso granular basado en permisos (PBAC).
 *
 * Un permiso puede estar asociado a múltiples roles a través de la entidad RolePermission,
 * permitiendo una gestión flexible de autorizaciones.
 *
 * Extiende AbstractEntity para funcionalidades comunes como timestamps,
 * validación y operaciones estándar de entidades.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar permisos
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
@Entity(name = "permissions")
public class Permission extends AbstractEntity {

    /**
     * Identificador único del permiso.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long permissionId;

    /**
     * Nombre único del permiso (ej: "CREATE_SCHEDULE", "DELETE_USER").
     */
    @Column(name = "permission_name", nullable = false, unique = true, length = 100)
    @NotNull(message = "El nombre del permiso es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre del permiso debe tener entre 3 y 100 caracteres")
    private String permissionName;

    /**
     * Descripción detallada del permiso y su funcionalidad.
     */
    @Column(name = "description", length = 255)
    @Size(max = 255, message = "La descripción debe tener máximo 255 caracteres")
    private String description;

    /**
     * Indica si el permiso está activo y puede ser asignado.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /**
     * Timestamp de creación del permiso.
     */
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización del permiso.
     */
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    /**
     * Relación uno-a-muchos con asignaciones de permisos a roles.
     */
    @OneToMany(mappedBy = "permission", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<RolePermission> rolePermissions = new HashSet<>();

    /**
     * Constructor vacío requerido por JPA.
     * Inicializa los timestamps heredados de AbstractEntity.
     */
    public Permission() {
        super();
    }

    /**
     * Constructor con parámetros principales para crear un permiso.
     * Inicializa los timestamps heredados de AbstractEntity.
     *
     * @param permissionName nombre único del permiso
     * @param description descripción del permiso
     */
    public Permission(String permissionName, String description) {
        super();
        this.permissionName = permissionName;
        this.description = description;
    }

    /**
     * Obtiene el identificador único del permiso.
     *
     * @return ID del permiso
     */
    public Long getPermissionId() {
        return permissionId;
    }

    /**
     * Establece el identificador único del permiso.
     *
     * @param permissionId ID del permiso
     */
    public void setPermissionId(Long permissionId) {
        this.permissionId = permissionId;
    }

    /**
     * Obtiene el nombre del permiso.
     *
     * @return nombre del permiso
     */
    public String getPermissionName() {
        return permissionName;
    }

    /**
     * Establece el nombre del permiso.
     *
     * @param permissionName nombre del permiso
     */
    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    /**
     * Obtiene la descripción del permiso.
     *
     * @return descripción del permiso
     */
    public String getDescription() {
        return description;
    }

    /**
     * Establece la descripción del permiso.
     *
     * @param description descripción del permiso
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Verifica si el permiso está activo.
     *
     * @return true si el permiso está activo
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Establece si el permiso está activo.
     *
     * @param active true para activar el permiso
     */
    public void setActive(boolean active) {
        isActive = active;
    }

    /**
     * Obtiene la fecha de creación del permiso.
     *
     * @return fecha de creación
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Establece la fecha de creación del permiso.
     *
     * @param createdAt fecha de creación
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Obtiene la fecha de última actualización del permiso.
     *
     * @return fecha de última actualización
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Establece la fecha de última actualización del permiso.
     *
     * @param updatedAt fecha de última actualización
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Obtiene las asignaciones de este permiso a roles.
     *
     * @return conjunto de RolePermission
     */
    public Set<RolePermission> getRolePermissions() {
        return rolePermissions;
    }

    /**
     * Establece las asignaciones de este permiso a roles.
     *
     * @param rolePermissions conjunto de RolePermission
     */
    public void setRolePermissions(Set<RolePermission> rolePermissions) {
        this.rolePermissions = rolePermissions;
    }

    /**
     * Valida la entidad antes de persistirla.
     * Verifica que los campos obligatorios del permiso sean válidos.
     *
     * @throws IllegalArgumentException si la validación falla
     */
    @Override
    public void validate() {
        if (permissionName == null || permissionName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del permiso es obligatorio");
        }
        if (permissionName.length() < 3 || permissionName.length() > 100) {
            throw new IllegalArgumentException("El nombre del permiso debe tener entre 3 y 100 caracteres");
        }
        if (description != null && description.length() > 255) {
            throw new IllegalArgumentException("La descripción debe tener máximo 255 caracteres");
        }
    }

    /**
     * Obtiene una representación resumida del permiso.
     *
     * @return resumen como String
     */
    @Override
    public String getSummary() {
        return "Permiso: " + (permissionName != null ? permissionName : "Sin nombre") +
               " (ID: " + permissionId + ")" +
               " - " + (isActive ? "Activo" : "Inactivo");
    }

    /**
     * Verifica si la entidad es nueva (no persistida).
     *
     * @return true si es nueva
     */
    @Override
    public boolean isNew() {
        return permissionId == null;
    }

    /**
     * Método de utilidad para logging y debugging.
     *
     * @return representación en string del permiso
     */
    @Override
    public String toString() {
        return "Permission{" +
                "permissionId=" + permissionId +
                ", permissionName='" + permissionName + '\'' +
                ", description='" + description + '\'' +
                ", isActive=" + isActive +
                ", createdAt=" + getCreatedAt() +
                ", updatedAt=" + getUpdatedAt() +
                '}';
    }

    /**
     * Compara dos permisos por su igualdad.
     *
     * @param o objeto a comparar
     * @return true si son iguales
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Permission that = (Permission) o;

        if (permissionId != null ? !permissionId.equals(that.permissionId) : that.permissionId != null) return false;
        return permissionName != null ? permissionName.equals(that.permissionName) : that.permissionName == null;
    }

    /**
     * Genera el código hash del permiso.
     *
     * @return código hash
     */
    @Override
    public int hashCode() {
        int result = permissionId != null ? permissionId.hashCode() : 0;
        result = 31 * result + (permissionName != null ? permissionName.hashCode() : 0);
        return result;
    }
}