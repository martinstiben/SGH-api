package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Entidad que representa un token JWT revocado en el sistema SGH.
 * Los tokens revocados se almacenan para prevenir su reutilización,
 * implementando una lista negra de tokens inválidos.
 *
 * Esta entidad es crucial para la seguridad del sistema, permitiendo
 * invalidar tokens de manera permanente (logout forzado, cambio de contraseña, etc.).
 *
 * Extiende AbstractEntity para funcionalidades comunes como timestamps,
 * validación y operaciones estándar de entidades.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de representar tokens revocados
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
@Entity(name = "revoked_tokens")
public class RevokedToken extends AbstractEntity {

    /**
     * Identificador único del token revocado.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    /**
     * Token JWT revocado (hash o token completo).
     */
    @Column(name = "token", nullable = false, unique = true, length = 512)
    @NotNull(message = "El token es obligatorio")
    @Size(min = 10, max = 512, message = "El token debe tener entre 10 y 512 caracteres")
    private String token;

    /**
     * ID del usuario propietario del token revocado.
     */
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "El ID de usuario es obligatorio")
    private Long userId;

    /**
     * Timestamp de cuando el token fue revocado.
     */
    @Column(name = "revoked_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime revokedAt;

    /**
     * Timestamp de expiración original del token.
     */
    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime expiresAt;

    /**
     * Indica si el token revocado era un refresh token.
     */
    @Column(name = "is_refresh_token", nullable = false)
    private boolean isRefreshToken = false;

    /**
     * Constructor vacío requerido por JPA.
     * Inicializa los timestamps heredados de AbstractEntity.
     */
    public RevokedToken() {
        super();
        this.revokedAt = LocalDateTime.now();
    }

    /**
     * Constructor completo para crear un token revocado.
     * Inicializa los timestamps heredados de AbstractEntity.
     *
     * @param token token JWT revocado
     * @param userId ID del usuario propietario
     * @param expiresAt fecha de expiración original del token
     * @param isRefreshToken true si es un refresh token
     */
    public RevokedToken(String token, Long userId, LocalDateTime expiresAt, boolean isRefreshToken) {
        super();
        this.token = token;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.isRefreshToken = isRefreshToken;
        this.revokedAt = LocalDateTime.now();
    }

    /**
     * Obtiene el identificador único del token revocado.
     *
     * @return ID del token revocado
     */
    public Long getTokenId() {
        return tokenId;
    }

    /**
     * Establece el identificador único del token revocado.
     *
     * @param tokenId ID del token revocado
     */
    public void setTokenId(Long tokenId) {
        this.tokenId = tokenId;
    }

    /**
     * Obtiene el token JWT revocado.
     *
     * @return token revocado
     */
    public String getToken() {
        return token;
    }

    /**
     * Establece el token JWT revocado.
     *
     * @param token token revocado
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * Obtiene el ID del usuario propietario del token.
     *
     * @return ID del usuario
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Establece el ID del usuario propietario del token.
     *
     * @param userId ID del usuario
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * Obtiene la fecha y hora de revocación del token.
     *
     * @return fecha de revocación
     */
    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    /**
     * Establece la fecha y hora de revocación del token.
     *
     * @param revokedAt fecha de revocación
     */
    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    /**
     * Obtiene la fecha de expiración original del token.
     *
     * @return fecha de expiración
     */
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    /**
     * Establece la fecha de expiración original del token.
     *
     * @param expiresAt fecha de expiración
     */
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * Verifica si el token revocado era un refresh token.
     *
     * @return true si es refresh token
     */
    public boolean isRefreshToken() {
        return isRefreshToken;
    }

    /**
     * Establece si el token revocado era un refresh token.
     *
     * @param refreshToken true si es refresh token
     */
    public void setRefreshToken(boolean refreshToken) {
        isRefreshToken = refreshToken;
    }

    /**
     * Verifica si el token ya ha expirado completamente.
     *
     * @return true si el token ha expirado
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Valida la entidad antes de persistirla.
     * Verifica que los campos obligatorios del token revocado sean válidos.
     *
     * @throws IllegalArgumentException si la validación falla
     */
    @Override
    public void validate() {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("El token es obligatorio");
        }
        if (token.length() < 10 || token.length() > 512) {
            throw new IllegalArgumentException("El token debe tener entre 10 y 512 caracteres");
        }
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("El ID de usuario es obligatorio");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("La fecha de expiración es obligatoria");
        }
    }

    /**
     * Obtiene una representación resumida del token revocado.
     *
     * @return resumen como String
     */
    @Override
    public String getSummary() {
        String maskedToken = token != null && token.length() > 10 ?
                token.substring(0, 6) + "..." + token.substring(token.length() - 4) : "***";
        return "Token revocado: " + maskedToken + " - Usuario: " + userId + 
               (isRefreshToken ? " (Refresh)" : " (Access)");
    }

    /**
     * Verifica si la entidad es nueva (no persistida).
     *
     * @return true si es nueva
     */
    @Override
    public boolean isNew() {
        return tokenId == null;
    }

    /**
     * Método de utilidad para logging y debugging.
     *
     * @return representación en string del token revocado
     */
    @Override
    public String toString() {
        String maskedToken = token != null && token.length() > 10 ?
                token.substring(0, 6) + "..." + token.substring(token.length() - 4) : "***";
        return "RevokedToken{" +
                "tokenId=" + tokenId +
                ", maskedToken='" + maskedToken + '\'' +
                ", userId=" + userId +
                ", revokedAt=" + revokedAt +
                ", expiresAt=" + expiresAt +
                ", isRefreshToken=" + isRefreshToken +
                ", createdAt=" + getCreatedAt() +
                ", updatedAt=" + getUpdatedAt() +
                '}';
    }

    /**
     * Compara dos tokens revocados por su igualdad.
     *
     * @param o objeto a comparar
     * @return true si son iguales
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RevokedToken that = (RevokedToken) o;

        if (tokenId != null ? !tokenId.equals(that.tokenId) : that.tokenId != null) return false;
        return token != null ? token.equals(that.token) : that.token == null;
    }

    /**
     * Genera el código hash del token revocado.
     *
     * @return código hash
     */
    @Override
    public int hashCode() {
        int result = tokenId != null ? tokenId.hashCode() : 0;
        result = 31 * result + (token != null ? token.hashCode() : 0);
        return result;
    }
}