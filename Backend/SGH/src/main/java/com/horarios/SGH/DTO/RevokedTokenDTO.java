package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO para gestión de tokens revocados del sistema SGH.
 * Implementa validaciones de negocio específicas para tokens JWT
 * y métodos de utilidad para gestión de seguridad de usuarios.
 *
 * Proporciona métodos Factory para crear tokens revocados
 * y validaciones de formato de token con información de usuario.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO para gestión de tokens revocados")
public class RevokedTokenDTO extends AbstractDTO {

    /**
     * Identificador único del token revocado.
     */
    @Schema(description = "ID único del token revocado", example = "1")
    private Long tokenId;

    /**
     * Token JWT que ha sido revocado.
     */
    @NotNull(message = "El token es obligatorio")
    @Size(min = 10, max = 512, message = "El token debe tener entre 10 y 512 caracteres")
    @Schema(description = "Token JWT revocado", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    /**
     * Identificador del usuario propietario del token.
     */
    @NotNull(message = "El ID de usuario es obligatorio")
    @Schema(description = "ID del usuario propietario del token", example = "1")
    private Long userId;

    /**
     * Timestamp cuando el token fue revocado.
     */
    @Schema(description = "Fecha de revocación", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime revokedAt;

    /**
     * Timestamp de expiración original del token.
     */
    @Schema(description = "Fecha de expiración original del token", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime expiresAt;

    /**
     * Indica si el token revocado es un refresh token.
     */
    @Schema(description = "Indica si es un refresh token", example = "false")
    private boolean isRefreshToken = false;

    /**
     * Timestamp de creación del registro.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización.
     */
    private LocalDateTime updatedAt;

    /**
     * Método Factory para crear un token revocado por logout.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param token token JWT a revocar
     * @param userId ID del usuario propietario
     * @return RevokedTokenDTO configurado
     */
    /**
     * Constructor por defecto.
     */
    public RevokedTokenDTO() {
        super();
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor con parámetros principales.
     *
     * @param token token JWT a revocar
     * @param userId ID del usuario propietario
     */
    public RevokedTokenDTO(String token, Long userId) {
        super();
        this.token = token;
        this.userId = userId;
        this.revokedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    public static RevokedTokenDTO createForLogout(String token, Long userId) {
        RevokedTokenDTO dto = new RevokedTokenDTO();
        dto.setToken(token);
        dto.setUserId(userId);
        dto.setRevokedAt(LocalDateTime.now());
        dto.setRefreshToken(false);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear un refresh token revocado.
     *
     * @param token refresh token a revocar
     * @param userId ID del usuario propietario
     * @return RevokedTokenDTO configurado como refresh token
     */
    public static RevokedTokenDTO createRefreshToken(String token, Long userId) {
        RevokedTokenDTO dto = new RevokedTokenDTO();
        dto.setToken(token);
        dto.setUserId(userId);
        dto.setRevokedAt(LocalDateTime.now());
        dto.setRefreshToken(true);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear un token revocado por seguridad.
     *
     * @param token token JWT a revocar
     * @param userId ID del usuario propietario
     * @param reason razón de la revocación
     * @return RevokedTokenDTO configurado
     */
    public static RevokedTokenDTO createForSecurity(String token, Long userId, String reason) {
        RevokedTokenDTO dto = new RevokedTokenDTO();
        dto.setToken(token);
        dto.setUserId(userId);
        dto.setRevokedAt(LocalDateTime.now());
        dto.setCreatedAt(LocalDateTime.now());
        // Nota: el campo reason no existe en este DTO, se podría agregar si es necesario
        return dto;
    }

    /**
     * Método Factory para crear un RevokedTokenDTO vacío.
     * Útil para inicialización o pruebas.
     *
     * @return RevokedTokenDTO con valores por defecto
     */
    public static RevokedTokenDTO empty() {
        RevokedTokenDTO dto = new RevokedTokenDTO();
        dto.setRevokedAt(LocalDateTime.now());
        dto.setRefreshToken(false);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Verifica si el token tiene formato JWT válido.
     * Un token JWT válido tiene exactamente 3 partes separadas por puntos.
     *
     * @return true si el formato es válido
     */
    public boolean hasValidJwtFormat() {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        String[] parts = token.split("\\.");
        return parts.length == 3;
    }

    /**
     * Verifica si el token está expirado.
     *
     * @return true si el token ha expirado
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Verifica si la revocación fue reciente (últimas 24 horas).
     *
     * @return true si fue revocado recientemente
     */
    public boolean isRecentlyRevoked() {
        if (revokedAt == null) {
            return false;
        }

        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        return revokedAt.isAfter(oneDayAgo);
    }

    /**
     * Valida si el DTO tiene todos los campos obligatorios.
     * Método de validación de negocio.
     *
     * @return true si todos los campos obligatorios están presentes y válidos
     */
    @Override
    public boolean isValid() {
        return token != null && !token.trim().isEmpty() &&
               userId != null && userId > 0 &&
               hasValidJwtFormat() &&
               revokedAt != null;
    }

    /**
     * Obtiene una representación segura del token (mascarado).
     * Muestra solo los primeros y últimos caracteres.
     *
     * @return Token mascarado
     */
    public String getMaskedToken() {
        if (token == null || token.length() < 10) {
            return "***";
        }

        String firstPart = token.substring(0, 6);
        String lastPart = token.substring(token.length() - 4);
        return firstPart + "***" + lastPart;
    }

    /**
     * Obtiene una representación resumida del token revocado.
     * Formato: "Token revocado: [maskedToken] - Usuario: [userId] - Refresh: [isRefreshToken]"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        return String.format("Token revocado: %s - Usuario: %d - Refresh: %s",
                getMaskedToken(),
                userId != null ? userId : 0,
                isRefreshToken);
    }

    // Getters y Setters
    public Long getTokenId() {
        return tokenId;
    }

    public void setTokenId(Long tokenId) {
        this.tokenId = tokenId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isRefreshToken() {
        return isRefreshToken;
    }

    public void setRefreshToken(boolean refreshToken) {
        isRefreshToken = refreshToken;
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}