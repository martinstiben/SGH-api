package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * DTO para gestión de información de seguridad de usuario del sistema SGH.
 * Implementa validaciones de negocio específicas para estados de cuenta
 * y métodos de utilidad para gestión de bloqueos y expiraciones.
 *
 * Proporciona métodos Factory para crear estados de seguridad
 * y validaciones de políticas de seguridad.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO para gestión de información de seguridad de usuario")
public class UserSecurityDTO extends AbstractDTO {

    /**
     * Identificador único de la información de seguridad.
     */
    @Schema(description = "ID único de la información de seguridad", example = "1")
    private Long securityId;

    /**
     * Indica si la cuenta está habilitada.
     */
    @Schema(description = "Cuenta habilitada", example = "true")
    private boolean enabled = true;

    /**
     * Indica si la cuenta está bloqueada.
     */
    @Schema(description = "Cuenta bloqueada", example = "false")
    private boolean locked = false;

    /**
     * Indica si las credenciales han expirado.
     */
    @Schema(description = "Credenciales expiradas", example = "false")
    private boolean credentialsExpired = false;

    /**
     * Indica si la cuenta ha expirado.
     */
    @Schema(description = "Cuenta expirada", example = "false")
    private boolean accountExpired = false;

    /**
     * Timestamp del último login exitoso.
     */
    @Schema(description = "Último login", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime lastLogin;

    /**
     * Número de intentos fallidos de login.
     */
    @Schema(description = "Intentos fallidos de login", example = "0")
    private int failedAttempts = 0;

    /**
     * Timestamp de creación de la información de seguridad.
     */
    @Schema(description = "Fecha de creación", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización.
     */
    @Schema(description = "Fecha de última actualización", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    /**
     * Constructor por defecto.
     */
    public UserSecurityDTO() {
        super();
        this.enabled = true;
        this.locked = false;
        this.credentialsExpired = false;
        this.accountExpired = false;
        this.failedAttempts = 0;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor con parámetros principales.
     *
     * @param securityId ID de seguridad
     */
    public UserSecurityDTO(Long securityId) {
        super();
        this.securityId = securityId;
        this.enabled = true;
        this.locked = false;
        this.credentialsExpired = false;
        this.accountExpired = false;
        this.failedAttempts = 0;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Método Factory para crear un estado de seguridad básico.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @return UserSecurityDTO configurado
     */
    public static UserSecurityDTO create() {
        UserSecurityDTO dto = new UserSecurityDTO();
        dto.setEnabled(true);
        dto.setLocked(false);
        dto.setCredentialsExpired(false);
        dto.setAccountExpired(false);
        dto.setFailedAttempts(0);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear un estado de cuenta bloqueada.
     *
     * @return UserSecurityDTO con cuenta bloqueada
     */
    public static UserSecurityDTO createLocked() {
        UserSecurityDTO dto = create();
        dto.setLocked(true);
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear un estado de cuenta deshabilitada.
     *
     * @return UserSecurityDTO con cuenta deshabilitada
     */
    public static UserSecurityDTO createDisabled() {
        UserSecurityDTO dto = create();
        dto.setEnabled(false);
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear un UserSecurityDTO vacío.
     * Útil para inicialización o pruebas.
     *
     * @return UserSecurityDTO con valores por defecto
     */
    public static UserSecurityDTO empty() {
        UserSecurityDTO dto = new UserSecurityDTO();
        dto.setEnabled(true);
        dto.setLocked(false);
        dto.setCredentialsExpired(false);
        dto.setAccountExpired(false);
        dto.setFailedAttempts(0);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Verifica si la cuenta está en buen estado (habilitada y no bloqueada).
     *
     * @return true si la cuenta está en buen estado
     */
    public boolean isAccountInGoodStanding() {
        return enabled && !locked && !accountExpired && !credentialsExpired;
    }

    /**
     * Verifica si las credenciales necesitan renovación.
     * Considera que necesitan renovación si tienen más de 90 días.
     *
     * @return true si necesitan renovación
     */
    public boolean credentialsNeedRenewal() {
        if (lastLogin == null) {
            return true; // Si nunca ha hecho login, asumir que necesita renovación
        }

        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        return lastLogin.isBefore(ninetyDaysAgo);
    }

    /**
     * Incrementa el contador de intentos fallidos.
     */
    public void incrementFailedAttempts() {
        this.failedAttempts++;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Reinicia el contador de intentos fallidos.
     */
    public void resetFailedAttempts() {
        this.failedAttempts = 0;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Verifica si la cuenta debe ser bloqueada por demasiados intentos fallidos.
     * Política: 5 intentos fallidos bloquean la cuenta.
     *
     * @return true si debe ser bloqueada
     */
    public boolean shouldBeLocked() {
        return failedAttempts >= 5;
    }

    /**
     * Aplica bloqueo de cuenta.
     */
    public void applyLockout() {
        this.locked = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Registra un login exitoso, reiniciando contadores.
     */
    public void recordSuccessfulLogin() {
        this.lastLogin = LocalDateTime.now();
        this.failedAttempts = 0;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Valida si el DTO tiene información básica completa.
     * Método de validación de negocio.
     *
     * @return true si tiene la información esencial
     */
    @Override
    public boolean isValid() {
        return securityId != null && securityId > 0;
    }

    /**
     * Obtiene una representación resumida del estado de seguridad.
     * Formato: "Seguridad ID [securityId] - Estado: [estado] - Intentos fallidos: [failedAttempts]"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        String status = isAccountInGoodStanding() ? "Activo" :
                       (!enabled ? "Deshabilitado" :
                       (locked ? "Bloqueado" : "Problema"));
        return String.format("Seguridad ID %d - Estado: %s - Intentos fallidos: %d",
                securityId != null ? securityId : 0,
                status,
                failedAttempts);
    }

    // Getters y Setters
    public Long getSecurityId() {
        return securityId;
    }

    public void setSecurityId(Long securityId) {
        this.securityId = securityId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isCredentialsExpired() {
        return credentialsExpired;
    }

    public void setCredentialsExpired(boolean credentialsExpired) {
        this.credentialsExpired = credentialsExpired;
    }

    public boolean isAccountExpired() {
        return accountExpired;
    }

    public void setAccountExpired(boolean accountExpired) {
        this.accountExpired = accountExpired;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
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