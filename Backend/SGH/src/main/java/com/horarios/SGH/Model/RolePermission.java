package com.horarios.SGH.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad de relación muchos-a-muchos entre roles y permisos en el sistema SGH.
 * Representa la asignación de un permiso específico a un rol,
 * permitiendo que un rol tenga múltiples permisos y que un permiso
 * sea asignado a múltiples roles.
 *
 * Esta entidad es fundamental para el sistema de control de acceso basado en roles (RBAC),
 * estableciendo las relaciones que determinan qué acciones puede realizar cada rol.
 *
 * Extiende AbstractEntity para funcionalidades comunes como timestamps,
 * validación y operaciones estándar de entidades.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar relaciones rol-permiso
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
@Entity(name = "role_permissions")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "permission_id"}))
public class RolePermission extends AbstractEntity {

    /**
     * Identificador único de la relación rol-permiso.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_permission_id")
    private Long rolePermissionId;

    /**
     * Rol al que se asigna el permiso.
     * Relación obligatoria con la entidad Role.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /**
     * Permiso asignado al rol.
     * Relación obligatoria con la entidad Permission.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    /**
     * Timestamp de creación de la asignación.
     */
    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización de la asignación.
     */
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    /**
     * Constructor vacío requerido por JPA.
     * Inicializa los timestamps heredados de AbstractEntity.
     */
    public RolePermission() {
        super();
    }

    /**
     * Constructor con parámetros para crear una asignación rol-permiso.
     * Inicializa los timestamps heredados de AbstractEntity.
     *
     * @param role rol al que se asigna el permiso
     * @param permission permiso asignado al rol
     */
    public RolePermission(Role role, Permission permission) {
        super();
        this.role = role;
        this.permission = permission;
    }

    /**
     * Obtiene el identificador único de la relación.
     *
     * @return ID de la relación rol-permiso
     */
    public Long getRolePermissionId() {
        return rolePermissionId;
    }

    /**
     * Establece el identificador único de la relación.
     *
     * @param rolePermissionId ID de la relación rol-permiso
     */
    public void setRolePermissionId(Long rolePermissionId) {
        this.rolePermissionId = rolePermissionId;
    }

    /**
     * Obtiene el rol de la relación.
     *
     * @return rol asignado
     */
    public Role getRole() {
        return role;
    }

    /**
     * Establece el rol de la relación.
     *
     * @param role rol asignado
     */
    public void setRole(Role role) {
        this.role = role;
    }

    /**
     * Obtiene el permiso de la relación.
     *
     * @return permiso asignado
     */
    public Permission getPermission() {
        return permission;
    }

    /**
     * Establece el permiso de la relación.
     *
     * @param permission permiso asignado
     */
    public void setPermission(Permission permission) {
        this.permission = permission;
    }

    /**
     * Obtiene la fecha de creación de la asignación.
     *
     * @return fecha de creación
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Establece la fecha de creación de la asignación.
     *
     * @param createdAt fecha de creación
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Obtiene la fecha de última actualización de la asignación.
     *
     * @return fecha de última actualización
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Establece la fecha de última actualización de la asignación.
     *
     * @param updatedAt fecha de última actualización
     */
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Valida la entidad antes de persistirla.
     * Verifica que los campos obligatorios de la relación rol-permiso sean válidos.
     *
     * @throws IllegalArgumentException si la validación falla
     */
    @Override
    public void validate() {
        if (role == null) {
            throw new IllegalArgumentException("El rol es obligatorio");
        }
        if (permission == null) {
            throw new IllegalArgumentException("El permiso es obligatorio");
        }
    }

    /**
     * Obtiene una representación resumida de la relación rol-permiso.
     *
     * @return resumen como String
     */
    @Override
    public String getSummary() {
        return "Asignación: " + 
               (role != null ? role.getRoleName() : "Sin rol") + 
               " -> " + 
               (permission != null ? permission.getPermissionName() : "Sin permiso");
    }

    /**
     * Verifica si la entidad es nueva (no persistida).
     *
     * @return true si es nueva
     */
    @Override
    public boolean isNew() {
        return rolePermissionId == null;
    }

    /**
     * Método de utilidad para logging y debugging.
     *
     * @return representación en string de la relación rol-permiso
     */
    @Override
    public String toString() {
        return "RolePermission{" +
                "rolePermissionId=" + rolePermissionId +
                ", role=" + (role != null ? role.getRoleName() : "null") +
                ", permission=" + (permission != null ? permission.getPermissionName() : "null") +
                ", createdAt=" + getCreatedAt() +
                ", updatedAt=" + getUpdatedAt() +
                '}';
    }

    /**
     * Compara dos asignaciones rol-permiso por su igualdad.
     *
     * @param o objeto a comparar
     * @return true si son iguales
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RolePermission that = (RolePermission) o;

        if (rolePermissionId != null ? !rolePermissionId.equals(that.rolePermissionId) : that.rolePermissionId != null) return false;
        if (role != null ? !role.equals(that.role) : that.role != null) return false;
        return permission != null ? permission.equals(that.permission) : that.permission == null;
    }

    /**
     * Genera el código hash de la asignación rol-permiso.
     *
     * @return código hash
     */
    @Override
    public int hashCode() {
        int result = rolePermissionId != null ? rolePermissionId.hashCode() : 0;
        result = 31 * result + (role != null ? role.hashCode() : 0);
        result = 31 * result + (permission != null ? permission.hashCode() : 0);
        return result;
    }
}