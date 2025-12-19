package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

/**
 * Entidad principal que representa un usuario del sistema SGH.
 * Un usuario puede ser un estudiante, maestro, coordinador o director de área,
 * y contiene toda la información necesaria para autenticación y autorización.
 *
 * Esta entidad es el núcleo del sistema de usuarios, conectando personas,
 * credenciales, roles y configuraciones de seguridad.
 *
 * @author Sistema SGH
 * @version 1.0
 */
/**
 * Entidad principal que representa un usuario del sistema SGH.
 * Un usuario puede ser un estudiante, maestro, coordinador o director de área,
 * y contiene toda la información necesaria para autenticación y autorización.
 *
 * Esta entidad es el núcleo del sistema de usuarios, conectando personas,
 * credenciales, roles y configuraciones de seguridad.
 *
 * Extiende AbstractEntity para funcionalidades comunes como timestamps,
 * validación y operaciones estándar de entidades.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar usuarios
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
@Entity(name = "users")
public class User extends AbstractEntity {
    
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

    /**
     * Relación uno-a-uno con UserCredentials para manejar las credenciales de autenticación.
     * Esta relación es obligatoria y se maneja en cascada.
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserCredentials userCredentials;


    /**
     * Relación muchos-a-muchos con Role para gestionar los roles del usuario.
     * Un usuario puede tener múltiples roles (ej: estudiante y maestro).
     */
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
        super();
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
     * Obtiene el identificador único del usuario.
     *
     * @return ID del usuario
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Establece el identificador único del usuario.
     *
     * @param userId ID del usuario
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * Obtiene el nombre de usuario único.
     *
     * @return nombre de usuario
     */
    public String getUsername() {
        return username;
    }

    /**
     * Establece el nombre de usuario único.
     *
     * @param username nombre de usuario
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Obtiene el correo electrónico del usuario.
     *
     * @return email del usuario
     */
    public String getEmail() {
        return email;
    }

    /**
     * Establece el correo electrónico del usuario.
     *
     * @param email email del usuario
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Obtiene el nombre del usuario.
     *
     * @return nombre del usuario
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Establece el nombre del usuario.
     *
     * @param firstName nombre del usuario
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Obtiene el apellido del usuario.
     *
     * @return apellido del usuario
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Establece el apellido del usuario.
     *
     * @param lastName apellido del usuario
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Obtiene la persona asociada al usuario.
     *
     * @return persona asociada
     */
    public People getPerson() {
        return person;
    }

    /**
     * Establece la persona asociada al usuario.
     *
     * @param person persona asociada
     */
    public void setPerson(People person) {
        this.person = person;
    }

    /**
     * Obtiene el curso al que pertenece el usuario (si es estudiante).
     *
     * @return curso del usuario
     */
    public courses getCourse() {
        return course;
    }

    /**
     * Establece el curso al que pertenece el usuario.
     *
     * @param course curso del usuario
     */
    public void setCourse(courses course) {
        this.course = course;
    }

    /**
     * Verifica si el usuario está verificado.
     *
     * @return true si está verificado
     */
    public boolean isVerified() {
        return isVerified;
    }

    /**
     * Establece si el usuario está verificado.
     *
     * @param verified true si está verificado
     */
    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    /**
     * Obtiene el estado de la cuenta del usuario.
     *
     * @return estado de la cuenta
     */
    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    /**
     * Establece el estado de la cuenta del usuario.
     *
     * @param accountStatus estado de la cuenta
     */
    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    /**
     * Valida si la entidad tiene información básica completa.
     * Método de validación de negocio.
     */
    @Override
    public void validate() {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de usuario no puede estar vacío");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email no puede estar vacío");
        }
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("El apellido no puede estar vacío");
        }
        if (person == null) {
            throw new IllegalArgumentException("La persona es obligatoria");
        }
        if (accountStatus == null) {
            throw new IllegalArgumentException("El estado de la cuenta es obligatorio");
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
        return userId == null;
    }

    /**
     * Obtiene una representación resumida del usuario.
     * Formato: "Usuario [userId] - [username] ([email])"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        return String.format("Usuario %d - %s (%s)",
                userId != null ? userId : 0,
                username != null ? username : "Sin usuario",
                email != null ? email : "Sin email");
    }

    /**
     * Obtiene las credenciales del usuario.
     *
     * @return credenciales del usuario
     */
    public UserCredentials getUserCredentials() {
        return userCredentials;
    }

    /**
     * Establece las credenciales del usuario.
     *
     * @param userCredentials credenciales del usuario
     */
    public void setUserCredentials(UserCredentials userCredentials) {
        this.userCredentials = userCredentials;
    }


    /**
     * Obtiene los roles asignados al usuario.
     *
     * @return conjunto de roles
     */
    public Set<Role> getRoles() {
        return roles;
    }

    /**
     * Establece los roles asignados al usuario.
     *
     * @param roles conjunto de roles
     */
    public void setRoles(Set<Role> roles) {
        this.roles = roles;
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
            if (roles == null) {
                roles = new HashSet<>();
            }
            roles.add(role);
        }
    }

    /**
     * Remueve un rol del usuario.
     *
     * @param role rol a remover
     */
    public void removeRole(Role role) {
        if (role != null && roles != null) {
            roles.remove(role);
        }
    }

    /**
     * Limpia todos los roles del usuario.
     */
    public void clearRoles() {
        if (roles != null) {
            roles.clear();
        }
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