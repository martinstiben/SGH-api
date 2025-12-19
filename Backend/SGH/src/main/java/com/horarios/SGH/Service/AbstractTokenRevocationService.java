package com.horarios.SGH.Service;

/**
 * Clase abstracta para servicios de revocación de tokens.
 * Implementa patrón Template Method para el flujo de revocación.
 *
 * @author Sistema SGH
 * @version 1.0
 */
public abstract class AbstractTokenRevocationService {

    protected final TokenStorage tokenStorage;

    protected AbstractTokenRevocationService(TokenStorage tokenStorage) {
        this.tokenStorage = tokenStorage;
    }

    /**
     * Revoca un token. Método template.
     *
     * @param token Token a revocar
     */
    public final void revokeToken(String token) {
        validateToken(token);
        performRevocation(token);
        onTokenRevoked(token);
    }

    /**
     * Verifica si un token está revocado.
     *
     * @param token Token a verificar
     * @return true si revocado
     */
    public boolean isTokenRevoked(String token) {
        return tokenStorage.containsToken(token);
    }

    /**
     * Obtiene conteo de tokens revocados.
     *
     * @return número de tokens
     */
    public int getRevokedTokensCount() {
        return tokenStorage.size();
    }

    /**
     * Método hook para validación.
     *
     * @param token Token a validar
     */
    protected void validateToken(String token) {
        // Implementación por defecto
    }

    /**
     * Método abstracto para realizar la revocación.
     *
     * @param token Token a revocar
     */
    protected abstract void performRevocation(String token);

    /**
     * Método hook después de revocar.
     *
     * @param token Token revocado
     */
    protected void onTokenRevoked(String token) {
        // Implementación por defecto
    }

    /**
     * Revoca todos los tokens de un usuario (método abstracto para extensiones futuras).
     *
     * @param username Usuario
     */
    public abstract void revokeAllTokensForUser(String username);

    /**
     * Limpia tokens expirados.
     */
    public void cleanupExpiredTokens() {
        tokenStorage.cleanupExpiredTokens();
    }
}