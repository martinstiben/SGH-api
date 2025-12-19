package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Entidad que maneja toda la información de seguridad y autenticación de usuarios en el sistema SGH.
 * Centraliza credenciales, estado de cuenta, MFA, intentos fallidos y bloqueos de seguridad.
 *
 * Esta entidad implementa el patrón de seguridad completo de Spring Security UserDetails,
 * proporcionando control granular sobre autenticación, autorización y estado de seguridad.
 *
 * Extiende AbstractEntity para funcionalidades comunes como timestamps,
 * validación y operaciones estándar de entidades.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de manejar credenciales de seguridad
 * - OCP: Abierto para extensión
 * - LSP: Sustituye a AbstractEntity
 *
 * Patrones de diseño aplicados:
 * - Template Method: Implementado a través de AbstractEntity
 * - Factory: Para creación centralizada (delegado a EntityFactory)
 *
 * @author Sistema SGH
 * @version 2.0 - Fusionada con UserSecurity para centralizar lógica de seguridad
 */
@Entity(name = "user_credentials")
public class UserCredentials extends AbstractEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credential_id")
    private Long credentialId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @NotNull(message = "El usuario es obligatorio")
    private User user;

    @Column(name = "password_hash", nullable = false, length = 72)
    @NotNull(message = "El hash de la contraseña es obligatorio")
    private String passwordHash; // BCrypt genera exactamente 72 caracteres (60 hash + 12 salt integrados)

    @Column(name = "password_changed_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime passwordChangedAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "account_locked_until", columnDefinition = "TIMESTAMP")
    private LocalDateTime accountLockedUntil;

    // Campos fusionados de UserSecurity para centralizar lógica de seguridad
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

    @Column(name = "last_failed_login", columnDefinition = "TIMESTAMP")
    private LocalDateTime lastFailedLogin;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled = false;

    @Column(name = "mfa_secret", length = 255)
    private String mfaSecret; // Secreto MFA (siempre encriptado por política de seguridad)

    @Column(name = "last_password_reset_request", columnDefinition = "TIMESTAMP")
    private LocalDateTime lastPasswordResetRequest;

    /**
     * Constructor vacío requerido por JPA.
     */
    public UserCredentials() {
        super();
    }

    /**
     * Constructor con parámetros principales para crear credenciales de usuario.
     *
     * @param user usuario propietario de las credenciales
     * @param passwordHash hash de la contraseña
     */
    public UserCredentials(User user, String passwordHash) {
        super();
        this.user = user;
        this.passwordHash = passwordHash;
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
        if (passwordHash == null || passwordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("El hash de la contraseña es obligatorio");
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
        return credentialId == null;
    }

    /**
     * Obtiene una representación resumida de las credenciales.
     * Formato: "Credenciales [credentialId] - Usuario [userId] - [estado]"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        String userId = user != null ? String.valueOf(user.getUserId()) : "Sin usuario";
        String estado = isAccountValid() ? "Válidas" : "Inválidas";
        return String.format("Credenciales %d - Usuario %s - %s",
                credentialId != null ? credentialId : 0,
                userId,
                estado);
    }

    // Getters y Setters
    /**
     * Obtiene el identificador único de las credenciales.
     *
     * @return ID de las credenciales
     */
    public Long getCredentialId() {
        return credentialId;
    }

    /**
     * Establece el identificador único de las credenciales.
     *
     * @param credentialId ID de las credenciales
     */
    public void setCredentialId(Long credentialId) {
        this.credentialId = credentialId;
    }

    /**
     * Obtiene el usuario propietario de las credenciales.
     *
     * @return usuario asociado
     */
    public User getUser() {
        return user;
    }

    /**
     * Establece el usuario propietario de las credenciales.
     *
     * @param user usuario asociado
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Obtiene el hash de la contraseña.
     *
     * @return hash de la contraseña
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Establece el hash de la contraseña.
     *
     * @param passwordHash hash de la contraseña
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Obtiene la fecha del último cambio de contraseña.
     *
     * @return fecha del último cambio
     */
    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    /**
     * Establece la fecha del último cambio de contraseña.
     *
     * @param passwordChangedAt fecha del último cambio
     */
    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }

    /**
     * Obtiene el número de intentos fallidos de login.
     *
     * @return número de intentos fallidos
     */
    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    /**
     * Establece el número de intentos fallidos de login.
     *
     * @param failedLoginAttempts número de intentos fallidos
     */
    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    /**
     * Obtiene la fecha hasta la cual la cuenta está bloqueada.
     *
     * @return fecha de desbloqueo de la cuenta
     */
    public LocalDateTime getAccountLockedUntil() {
        return accountLockedUntil;
    }

    /**
     * Establece la fecha hasta la cual la cuenta está bloqueada.
     *
     * @param accountLockedUntil fecha de desbloqueo de la cuenta
     */
    public void setAccountLockedUntil(LocalDateTime accountLockedUntil) {
        this.accountLockedUntil = accountLockedUntil;
    }

    /**
     * Obtiene la fecha de la última solicitud de reset de contraseña.
     *
     * @return fecha de la última solicitud de reset
     */
    public LocalDateTime getLastPasswordResetRequest() {
        return lastPasswordResetRequest;
    }

    /**
     * Establece la fecha de la última solicitud de reset de contraseña.
     *
     * @param lastPasswordResetRequest fecha de la última solicitud de reset
     */
    public void setLastPasswordResetRequest(LocalDateTime lastPasswordResetRequest) {
        this.lastPasswordResetRequest = lastPasswordResetRequest;
    }

    /**
     * Verifica si la cuenta está bloqueada por intentos fallidos de login.
     *
     * @return true si la cuenta está bloqueada
     */
    public boolean isAccountLocked() {
        return accountLockedUntil != null && LocalDateTime.now().isBefore(accountLockedUntil);
    }

    /**
     * Incrementa el contador de intentos fallidos de login.
     */
    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
        this.lastFailedLogin = LocalDateTime.now();
    }

    /**
     * Reinicia el contador de intentos fallidos de login.
     */
    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
        this.lastFailedLogin = null;
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
     * Registra un login exitoso, actualizando la fecha del último login.
     */
    public void recordSuccessfulLogin() {
        this.lastLogin = LocalDateTime.now();
        this.failedLoginAttempts = 0; // Resetear intentos fallidos
        this.accountLockedUntil = null; // Desbloquear cuenta
        setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Verifica si se debe bloquear la cuenta por demasiados intentos fallidos.
     * Método de utilidad para lógica de bloqueo automático.
     *
     * @param maxAttempts número máximo de intentos permitidos
     * @return true si la cuenta debe ser bloqueada
     */
    public boolean shouldLockAccount(int maxAttempts) {
        return failedLoginAttempts >= maxAttempts;
    }

    /**
     * Verifica si la contraseña necesita ser cambiada (política de expiración).
     *
     * @param maxAgeDays edad máxima de la contraseña en días
     * @return true si la contraseña ha expirado
     */
    public boolean isPasswordExpired(int maxAgeDays) {
        if (passwordChangedAt == null) {
            return false; // Si nunca se cambió, no ha expirado
        }
        return passwordChangedAt.isBefore(LocalDateTime.now().minusDays(maxAgeDays));
    }

    /**
     * Actualiza la contraseña y registra el cambio.
     *
     * @param newPasswordHash nuevo hash de contraseña
     */
    public void updatePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.passwordChangedAt = LocalDateTime.now();
        this.failedLoginAttempts = 0; // Resetear intentos fallidos
        this.accountLockedUntil = null; // Desbloquear cuenta
        setUpdatedAt(LocalDateTime.now());
    }

    /**
     * Verifica si el usuario puede solicitar un reset de contraseña.
     * Método para prevenir abuso de la funcionalidad de reset.
     *
     * @param cooldownMinutes tiempo de espera mínimo entre solicitudes
     * @return true si puede solicitar reset
     */
    public boolean canRequestPasswordReset(int cooldownMinutes) {
        if (lastPasswordResetRequest == null) {
            return true;
        }
        return lastPasswordResetRequest.isBefore(LocalDateTime.now().minusMinutes(cooldownMinutes));
    }

    /**
     * Registra una solicitud de reset de contraseña.
     */
    public void recordPasswordResetRequest() {
        this.lastPasswordResetRequest = LocalDateTime.now();
        setUpdatedAt(LocalDateTime.now());
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
     * Representación en string de la información de seguridad del usuario.
     *
     * @return string con información de seguridad (sin datos sensibles)
     */
    @Override
    public String toString() {
        return "UserCredentials{" +
                "credentialId=" + credentialId +
                ", userId=" + (user != null ? user.getUserId() : "null") +
                ", enabled=" + enabled +
                ", locked=" + locked +
                ", mfaEnabled=" + mfaEnabled +
                ", failedLoginAttempts=" + failedLoginAttempts +
                ", accountLockedUntil=" + accountLockedUntil +
                '}';
    }
}
