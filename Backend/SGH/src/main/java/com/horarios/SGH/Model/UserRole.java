package com.horarios.SGH.Model;

import jakarta.persistence.*;

/**
 * Entidad de relación muchos-a-muchos entre usuarios y roles en el sistema SGH.
 * Representa la asignación de un rol específico a un usuario,
 * permitiendo que un usuario tenga múltiples roles y que un rol
 * sea asignado a múltiples usuarios.
 *
 * Esta entidad es fundamental para el sistema de control de acceso basado en roles (RBAC),
 * estableciendo las relaciones que determinan qué roles tiene cada usuario.
 *
 * @author Sistema SGH
 * @version 1.0
 */
/**
 * Entidad de relación muchos-a-muchos entre usuarios y roles en el sistema SGH.
 * Representa la asignación de un rol específico a un usuario,
 * permitiendo que un usuario tenga múltiples roles y que un rol
 * sea asignado a múltiples usuarios.
 *
 * Esta entidad es fundamental para el sistema de control de acceso basado en roles (RBAC),
 * estableciendo las relaciones que determinan qué roles tiene cada usuario.
 *
 * Extiende AbstractEntity para funcionalidades comunes como timestamps,
 * validación y operaciones estándar de entidades.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar relaciones usuario-rol
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
@Entity(name = "user_roles")
public class UserRole extends AbstractEntity {

    /**
     * Identificador único de la relación usuario-rol.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_role_id")
    private Long userRoleId;

    /**
     * Usuario al que se asigna el rol.
     * Relación obligatoria con la entidad User.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Rol asignado al usuario.
     * Relación obligatoria con la entidad Role.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;



    /**
     * Constructor vacío requerido por JPA.
     */
    public UserRole() {
        super();
    }

    /**
     * Constructor con parámetros para crear una asignación usuario-rol.
     *
     * @param user usuario al que se asigna el rol
     * @param role rol asignado al usuario
     */
    public UserRole(User user, Role role) {
        this();
        this.user = user;
        this.role = role;
    }

    /**
     * Obtiene el identificador único de la relación.
     *
     * @return ID de la relación usuario-rol
     */
    public Long getUserRoleId() {
        return userRoleId;
    }

    /**
     * Establece el identificador único de la relación.
     *
     * @param userRoleId ID de la relación usuario-rol
     */
    public void setUserRoleId(Long userRoleId) {
        this.userRoleId = userRoleId;
    }

    /**
     * Obtiene el usuario de la relación.
     *
     * @return usuario asignado
     */
    public User getUser() {
        return user;
    }

    /**
     * Establece el usuario de la relación.
     *
     * @param user usuario asignado
     */
    public void setUser(User user) {
        this.user = user;
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
     * Valida si la entidad tiene información básica completa.
     * Método de validación de negocio.
     */
    @Override
    public void validate() {
        if (user == null) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
        if (role == null) {
            throw new IllegalArgumentException("El rol es obligatorio");
        }
    }

    /**
     * Verifica si la entidad es nueva (no persistida).
     * Una entidad es nueva si no tiene ID asignado.
     *
     * @return true si es una nueva entidad
     */
    @Override
    public boolean isNew() {
        return userRoleId == null;
    }

    /**
     * Obtiene una representación resumida de la relación.
     * Formato: "Relación [userRoleId] - [username] -> [roleName]"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        String username = user != null ? user.getUsername() : "Sin usuario";
        String roleName = role != null ? role.getRoleName() : "Sin rol";
        return String.format("Relación %d - %s -> %s",
                userRoleId != null ? userRoleId : 0,
                username,
                roleName);
    }

    /**
     * Representación en string de la relación usuario-rol.
     *
     * @return string con información de la relación
     */
    @Override
    public String toString() {
        return "UserRole{" +
                "userRoleId=" + userRoleId +
                ", user=" + (user != null ? user.getUsername() : "null") +
                ", role=" + (role != null ? role.getRoleName() : "null") +
                '}';
    }

    /**
     * Compara dos asignaciones usuario-rol por su igualdad.
     *
     * @param o objeto a comparar
     * @return true si son iguales
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserRole userRole = (UserRole) o;

        if (userRoleId != null ? !userRoleId.equals(userRole.userRoleId) : userRole.userRoleId != null) return false;
        if (user != null ? !user.equals(userRole.user) : userRole.user != null) return false;
        return role != null ? role.equals(userRole.role) : userRole.role == null;
    }

    /**
     * Genera el código hash de la asignación usuario-rol.
     *
     * @return código hash
     */
    @Override
    public int hashCode() {
        int result = userRoleId != null ? userRoleId.hashCode() : 0;
        result = 31 * result + (user != null ? user.hashCode() : 0);
        result = 31 * result + (role != null ? role.hashCode() : 0);
        return result;
    }
}