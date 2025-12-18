package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad que representa un usuario del sistema SGH.
 * Un usuario puede ser un estudiante, maestro, coordinador o director de área.
 * 
 * @author Sistema SGH
 * @version 1.0
 */
@Entity(name = "users")
@Data
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, unique = true, length = 30)
    @NotNull(message = "El nombre de usuario es obligatorio")
    @Size(min = 4, max = 30, message = "El nombre de usuario debe tener entre 4 y 30 caracteres")
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    @NotNull(message = "El email es obligatorio")
    @Size(max = 100, message = "El email debe tener máximo 100 caracteres")
    private String email;

    @Column(name = "first_name", nullable = false, length = 50)
    @NotNull(message = "El nombre es obligatorio")
    @Size(min = 1, max = 50, message = "El nombre debe tener entre 1 y 50 caracteres")
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    @NotNull(message = "El apellido es obligatorio")
    @Size(min = 1, max = 50, message = "El apellido debe tener entre 1 y 50 caracteres")
    private String lastName;

    @OneToOne
    @JoinColumn(name = "person_id", nullable = false)
    @NotNull(message = "La persona es obligatoria")
    private People person;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private courses course;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 15)
    @NotNull(message = "El estado de la cuenta es obligatorio")
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    // Relación con UserCredentials para manejar las credenciales
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserCredentials userCredentials;

    // Relación con UserSecurity para manejar la seguridad
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserSecurity userSecurity;

    // Relación con UserRole para gestionar roles
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Constructor vacío requerido por JPA.
     */
    public User() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor con parámetros principales.
     * 
     * @param person persona asociada al usuario
     */
    public User(People person) {
        this();
        this.person = person;
    }

    /**
     * Constructor completo para creación de usuarios.
     * 
     * @param username nombre de usuario único
     * @param email correo electrónico único
     * @param firstName nombre del usuario
     * @param lastName apellido del usuario
     * @param person persona asociada
     */
    public User(String username, String email, String firstName, String lastName, People person) {
        this(person);
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    /**
     * Obtiene el nombre completo del usuario (concatenación de nombre y apellido).
     * 
     * @return nombre completo del usuario
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Verifica si el usuario tiene un rol específico.
     * 
     * @param roleName nombre del rol a verificar
     * @return true si el usuario tiene el rol
     */
    public boolean hasRole(String roleName) {
        return roles != null && roles.stream()
            .anyMatch(role -> role.getRoleName().equals(roleName));
    }

    /**
     * Obtiene el primer rol del usuario (para compatibilidad).
     * 
     * @return primer rol del usuario o null si no tiene roles
     */
    public Role getFirstRole() {
        if (roles != null && !roles.isEmpty()) {
            return roles.iterator().next();
        }
        return null;
    }

    /**
     * Verifica si el usuario está activo.
     * 
     * @return true si el usuario está activo
     */
    public boolean isActive() {
        return accountStatus == AccountStatus.ACTIVE;
    }

    /**
     * Verifica si el usuario está verificado.
     * 
     * @return true si el usuario está verificado
     */
    public boolean isVerified() {
        return isVerified;
    }

    /**
     * Activa la cuenta del usuario.
     */
    public void activate() {
        this.accountStatus = AccountStatus.ACTIVE;
    }

    /**
     * Desactiva la cuenta del usuario.
     */
    public void deactivate() {
        this.accountStatus = AccountStatus.INACTIVE;
    }

    /**
     * Suspende la cuenta del usuario.
     */
    public void suspend() {
        this.accountStatus = AccountStatus.BLOCKED;
    }

    /**
     * Agrega un rol al usuario.
     * 
     * @param role rol a agregar
     */
    public void addRole(Role role) {
        if (role != null) {
            roles.add(role);
        }
    }

    /**
     * Remueve un rol del usuario.
     * 
     * @param role rol a remover
     */
    public void removeRole(Role role) {
        if (role != null) {
            roles.remove(role);
        }
    }

    /**
     * Limpia todos los roles del usuario.
     */
    public void clearRoles() {
        roles.clear();
    }

    /**
     * Verifica si el usuario es estudiante.
     * 
     * @return true si es estudiante
     */
    public boolean isStudent() {
        return hasRole("ESTUDIANTE");
    }

    /**
     * Verifica si el usuario es maestro.
     * 
     * @return true si es maestro
     */
    public boolean isTeacher() {
        return hasRole("MAESTRO");
    }

    /**
     * Verifica si el usuario es coordinador.
     * 
     * @return true si es coordinador
     */
    public boolean isCoordinator() {
        return hasRole("COORDINADOR");
    }

    /**
     * Verifica si el usuario es director de área.
     * 
     * @return true si es director de área
     */
    public boolean isDirector() {
        return hasRole("DIRECTOR_DE_AREA");
    }

    /**
     * Método de utilidad para logging y debugging.
     * 
     * @return representación en string del usuario
     */
    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", isVerified=" + isVerified +
                ", accountStatus=" + accountStatus +
                ", roles=" + (roles != null ? roles.size() : 0) +
                '}';
    }
}