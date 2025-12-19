package com.horarios.SGH.Service;

/**
 * Interfaz para almacenamiento de tokens revocados.
 * Implementa patrón Strategy para diferentes estrategias de almacenamiento.
 *
 * @author Sistema SGH
 * @version 1.0
 */
public interface TokenStorage {
    /**
     * Agrega un token a la lista de revocados.
     *
     * @param token Token a agregar
     */
    void addToken(String token);

    /**
     * Verifica si un token está revocado.
     *
     * @param token Token a verificar
     * @return true si está revocado
     */
    boolean containsToken(String token);

    /**
     * Obtiene el número de tokens revocados.
     *
     * @return número de tokens
     */
    int size();

    /**
     * Limpia tokens expirados (implementación opcional).
     */
    default void cleanupExpiredTokens() {
        // Implementación por defecto vacía
    }
}