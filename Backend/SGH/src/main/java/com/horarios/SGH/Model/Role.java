package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad que representa un rol en el sistema SGH.
 * Los roles definen los permisos y responsabilidades de los usuarios,
 * permitiendo control de acceso basado en roles (RBAC).
 *
 * Un rol puede tener múltiples usuarios asignados y múltiples permisos asociados.
 *
 * Extiende AbstractEntity para funcionalidades comunes como timestamps,
 * validación y operaciones estándar de entidades.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar roles
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
@Entity(name = "roles")
public class Role extends AbstractEntity {

    /**
     * Identificador único del rol.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    /**
     * Nombre único del rol (ej: "ADMIN", "USER", "MODERATOR").
     */
    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    @NotNull(message = "El nombre del rol es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre del rol debe tener entre 3 y 50 caracteres")
    private String roleName;

    /**
     * Descripción del rol y sus responsabilidades.
     */
    @Column(name = "description", length = 255)
    @Size(max = 255, message = "La descripción debe tener máximo 255 caracteres")
    private String description;

    /**
     * Indica si el rol está activo y puede ser asignado a usuarios.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /**
     * Timestamp de creación del rol.
     */
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización del rol.
     */
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    /**
     * Relación muchos-a-muchos con usuarios a través de UserRole.
     */
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<UserRole> userRoles = new HashSet<>();

    /**
     * Relación uno-a-muchos con permisos del rol.
     */
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<RolePermission> rolePermissions = new HashSet<>();

    /**
     * Constructor vacío requerido por JPA.
     * Inicializa los timestamps heredados de AbstractEntity.
     */
    public Role() {
        super();
    }

    /**
     * Constructor con parámetros principales para crear un rol.
     * Inicializa los timestamps heredados de AbstractEntity.
     *
     * @param roleName nombre único del rol
     * @param description descripción del rol
     */
    public Role(String roleName, String description) {
        super();
        this.roleName = roleName;
        this.description = description;
    }

    /**
     * Obtiene el identificador único del rol.
     *
     * @return ID del rol
     */
    public Long getRoleId() {
        return roleId;
    }

    /**
     * Establece el identificador único del rol.
     *
     * @param roleId ID del rol
     */
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    /**
     * Obtiene el nombre del rol.
     *
     * @return nombre del rol
     */
    public String getRoleName() {
        return roleName;
    }

    /**
     * Establece el nombre del rol.
     *
     * @param roleName nombre del rol
     */
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    /**
     * Obtiene la descripción del rol.
     *
     * @return descripción del rol
     */
    public String getDescription() {
        return description;
    }

    /**
     * Establece la descripción del rol.
     *
     * @param description descripción del rol
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Verifica si el rol está activo.
     *
     * @return true si el rol está activo
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Establece si el rol está activo.
     *
     * @param active true para activar el rol
     */
    public void setActive(boolean active) {
        isActive = active;
    }

    /**
     * Obtiene la fecha de creación del rol.
     *
     * @return fecha de creación
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Establece la fecha de creación del rol.
     *
     * @param createdAt fecha de creación
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Obtiene la fecha de última actualización del rol.
     *
     * @return fecha de última actualización
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Establece la fecha de última actualización del rol.
     *
     * @param updatedAt fecha de última actualización
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Obtiene los roles de usuario asociados a este rol.
     *
     * @return conjunto de UserRole
     */
    public Set<UserRole> getUserRoles() {
        return userRoles;
    }

    /**
     * Establece los roles de usuario asociados a este rol.
     *
     * @param userRoles conjunto de UserRole
     */
    public void setUserRoles(Set<UserRole> userRoles) {
        this.userRoles = userRoles;
    }

    /**
     * Obtiene los permisos asociados a este rol.
     *
     * @return conjunto de RolePermission
     */
    public Set<RolePermission> getRolePermissions() {
        return rolePermissions;
    }

    /**
     * Establece los permisos asociados a este rol.
     *
     * @param rolePermissions conjunto de RolePermission
     */
    public void setRolePermissions(Set<RolePermission> rolePermissions) {
        this.rolePermissions = rolePermissions;
    }

    /**
     * Valida la entidad antes de persistirla.
     * Verifica que los campos obligatorios del rol sean válidos.
     *
     * @throws IllegalArgumentException si la validación falla
     */
    @Override
    public void validate() {
        if (roleName == null || roleName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del rol es obligatorio");
        }
        if (roleName.length() < 3 || roleName.length() > 50) {
            throw new IllegalArgumentException("El nombre del rol debe tener entre 3 y 50 caracteres");
        }
        if (description != null && description.length() > 255) {
            throw new IllegalArgumentException("La descripción debe tener máximo 255 caracteres");
        }
    }

    /**
     * Obtiene una representación resumida del rol.
     *
     * @return resumen como String
     */
    @Override
    public String getSummary() {
        return "Rol: " + (roleName != null ? roleName : "Sin nombre") + 
               " (ID: " + roleId + ")" +
               " - " + (isActive ? "Activo" : "Inactivo");
    }

    /**
     * Verifica si la entidad es nueva (no persistida).
     *
     * @return true si es nueva
     */
    @Override
    public boolean isNew() {
        return roleId == null;
    }

    /**
     * Método de utilidad para logging y debugging.
     *
     * @return representación en string del rol
     */
    @Override
    public String toString() {
        return "Role{" +
                "roleId=" + roleId +
                ", roleName='" + roleName + '\'' +
                ", description='" + description + '\'' +
                ", isActive=" + isActive +
                ", createdAt=" + getCreatedAt() +
                ", updatedAt=" + getUpdatedAt() +
                '}';
    }

    /**
     * Compara dos roles por su igualdad.
     *
     * @param o objeto a comparar
     * @return true si son iguales
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Role role = (Role) o;

        if (roleId != null ? !roleId.equals(role.roleId) : role.roleId != null) return false;
        return roleName != null ? roleName.equals(role.roleName) : role.roleName == null;
    }

    /**
     * Genera el código hash del rol.
     *
     * @return código hash
     */
    @Override
    public int hashCode() {
        int result = roleId != null ? roleId.hashCode() : 0;
        result = 31 * result + (roleName != null ? roleName.hashCode() : 0);
        return result;
    }
}