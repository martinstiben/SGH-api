package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Entidad que maneja la información de seguridad avanzada de usuarios en el sistema SGH.
 * Complementa UserCredentials con funcionalidades adicionales de seguridad como
 * bloqueo de cuentas, expiración de credenciales, autenticación multifactor (MFA)
 * y seguimiento de actividad de login.
 *
 * Esta entidad implementa el patrón de seguridad de Spring Security UserDetails,
 * proporcionando un control granular sobre el estado de seguridad de cada usuario.
 *
 * @author Sistema SGH
 * @version 1.0
 */
/**
 * Entidad que maneja la información de seguridad avanzada de usuarios en el sistema SGH.
 * Complementa UserCredentials con funcionalidades adicionales de seguridad como
 * bloqueo de cuentas, expiración de credenciales, autenticación multifactor (MFA)
 * y seguimiento de actividad de login.
 *
 * Esta entidad implementa el patrón de seguridad de Spring Security UserDetails,
 * proporcionando un control granular sobre el estado de seguridad de cada usuario.
 *
 * Extiende AbstractEntity para funcionalidades comunes como timestamps,
 * validación y operaciones estándar de entidades.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de manejar configuración de seguridad
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
@Entity(name = "user_security")
public class UserSecurity extends AbstractEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "security_id")
    private Long securityId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @NotNull(message = "El usuario es obligatorio")
    private User user;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "is_locked", nullable = false)
    private boolean locked = false;

    @Column(name = "credentials_expired", nullable = false)
    private boolean credentialsExpired = false;

    @Column(name = "account_expired", nullable = false)
    private boolean accountExpired = false;

    @Column(name = "last_login", columnDefinition = "TIMESTAMP")
    private LocalDateTime lastLogin;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    @Column(name = "last_failed_login", columnDefinition = "TIMESTAMP")
    private LocalDateTime lastFailedLogin;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled = false;

    @Column(name = "mfa_secret", length = 255)
    private String mfaSecret; // Secreto MFA (siempre encriptado por política de seguridad)

    /**
     * Constructor vacío requerido por JPA.
     */
    public UserSecurity() {
        super();
    }

    /**
     * Constructor con parámetros principales para crear configuración de seguridad de usuario.
     *
     * @param user usuario al que pertenece la configuración de seguridad
     */
    public UserSecurity(User user) {
        this();
        this.user = user;
    }

    /**
     * Obtiene el identificador único de la configuración de seguridad.
     *
     * @return ID de la configuración de seguridad
     */
    public Long getSecurityId() {
        return securityId;
    }

    /**
     * Establece el identificador único de la configuración de seguridad.
     *
     * @param securityId ID de la configuración de seguridad
     */
    public void setSecurityId(Long securityId) {
        this.securityId = securityId;
    }

    /**
     * Obtiene el usuario propietario de la configuración de seguridad.
     *
     * @return usuario asociado
     */
    public User getUser() {
        return user;
    }

    /**
     * Establece el usuario propietario de la configuración de seguridad.
     *
     * @param user usuario asociado
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Verifica si la cuenta de usuario está habilitada.
     *
     * @return true si la cuenta está habilitada
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Establece si la cuenta de usuario está habilitada.
     *
     * @param enabled true para habilitar la cuenta
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Verifica si la cuenta de usuario está bloqueada.
     *
     * @return true si la cuenta está bloqueada
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * Establece si la cuenta de usuario está bloqueada.
     *
     * @param locked true para bloquear la cuenta
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    /**
     * Verifica si las credenciales del usuario han expirado.
     *
     * @return true si las credenciales han expirado
     */
    public boolean isCredentialsExpired() {
        return credentialsExpired;
    }

    /**
     * Establece si las credenciales del usuario han expirado.
     *
     * @param credentialsExpired true si las credenciales han expirado
     */
    public void setCredentialsExpired(boolean credentialsExpired) {
        this.credentialsExpired = credentialsExpired;
    }

    /**
     * Verifica si la cuenta de usuario ha expirado.
     *
     * @return true si la cuenta ha expirado
     */
    public boolean isAccountExpired() {
        return accountExpired;
    }

    /**
     * Establece si la cuenta de usuario ha expirado.
     *
     * @param accountExpired true si la cuenta ha expirado
     */
    public void setAccountExpired(boolean accountExpired) {
        this.accountExpired = accountExpired;
    }

    /**
     * Obtiene la fecha del último login exitoso del usuario.
     *
     * @return fecha del último login
     */
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    /**
     * Establece la fecha del último login exitoso del usuario.
     *
     * @param lastLogin fecha del último login
     */
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    /**
     * Obtiene el número de intentos fallidos de login consecutivos.
     *
     * @return número de intentos fallidos
     */
    public int getFailedAttempts() {
        return failedAttempts;
    }

    /**
     * Establece el número de intentos fallidos de login consecutivos.
     *
     * @param failedAttempts número de intentos fallidos
     */
    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    /**
     * Obtiene la fecha del último intento fallido de login.
     *
     * @return fecha del último intento fallido
     */
    public LocalDateTime getLastFailedLogin() {
        return lastFailedLogin;
    }

    /**
     * Establece la fecha del último intento fallido de login.
     *
     * @param lastFailedLogin fecha del último intento fallido
     */
    public void setLastFailedLogin(LocalDateTime lastFailedLogin) {
        this.lastFailedLogin = lastFailedLogin;
    }

    /**
     * Verifica si la autenticación multifactor (MFA) está habilitada.
     *
     * @return true si MFA está habilitado
     */
    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    /**
     * Establece si la autenticación multifactor (MFA) está habilitada.
     *
     * @param mfaEnabled true para habilitar MFA
     */
    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    /**
     * Obtiene el secreto MFA del usuario (debe manejarse con cuidado por seguridad).
     *
     * @return secreto MFA
     */
    public String getMfaSecret() {
        return mfaSecret;
    }

    /**
     * Establece el secreto MFA del usuario.
     *
     * @param mfaSecret secreto MFA
     */
    public void setMfaSecret(String mfaSecret) {
        this.mfaSecret = mfaSecret;
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
    }

    /**
     * Verifica si la entidad es nueva (no persistida).
     * Una entidad es nueva si no tiene ID asignado.
     *
     * @return true si es una nueva entidad
     */
    @Override
    public boolean isNew() {
        return securityId == null;
    }

    /**
     * Obtiene una representación resumida de la configuración de seguridad.
     * Formato: "Seguridad [securityId] - Usuario [userId] - [estado]"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        String userId = user != null ? String.valueOf(user.getUserId()) : "Sin usuario";
        String estado = isAccountValid() ? "Válida" : "Inválida";
        return String.format("Seguridad %d - Usuario %s - %s",
                securityId != null ? securityId : 0,
                userId,
                estado);
    }

    /**
     * Verifica si la cuenta está en un estado válido para autenticación.
     * Una cuenta es válida si está habilitada, no bloqueada, no expirada y con credenciales válidas.
     *
     * @return true si la cuenta está en estado válido
     */
    public boolean isAccountValid() {
        return enabled && !locked && !accountExpired && !credentialsExpired;
    }

    /**
     * Incrementa el contador de intentos fallidos de login.
     */
    public void incrementFailedAttempts() {
        this.failedAttempts++;
        this.lastFailedLogin = LocalDateTime.now();
    }

    /**
     * Reinicia el contador de intentos fallidos de login.
     */
    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.lastFailedLogin = null;
    }

    /**
     * Representación en string de la configuración de seguridad.
     *
     * @return string con información de seguridad (sin datos sensibles)
     */
    @Override
    public String toString() {
        return "UserSecurity{" +
                "securityId=" + securityId +
                ", userId=" + (user != null ? user.getUserId() : "null") +
                ", enabled=" + enabled +
                ", locked=" + locked +
                ", mfaEnabled=" + mfaEnabled +
                ", failedAttempts=" + failedAttempts +
                '}';
    }
}
