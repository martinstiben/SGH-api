package com.horarios.SGH.Service;

import org.springframework.stereotype.Service;

/**
 * Servicio para gestión de revocación de tokens JWT.
 * Implementa AbstractTokenRevocationService con almacenamiento en memoria.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de gestionar revocación de tokens
 * - OCP: Abierto para extensión mediante diferentes storages
 * - LSP: Sustituye a AbstractTokenRevocationService
 * - ISP: Implementa solo métodos necesarios
 * - DIP: Depende de TokenStorage abstracción
 *
 * Patrones de diseño utilizados:
 * - Template Method: En AbstractTokenRevocationService
 * - Strategy: TokenStorage intercambiable
 * - Factory: TokenRevocationServiceFactory
 *
 * @author Sistema SGH
 * @version 2.0 - Refactorizado con patrones
 */
@Service
public class TokenRevocationService extends AbstractTokenRevocationService {

    /**
     * Constructor con inyección de TokenStorage.
     * Usa Factory para crear instancia por defecto.
     *
     * @param tokenStorage Almacenamiento de tokens
     */
    public TokenRevocationService(TokenStorage tokenStorage) {
        super(tokenStorage);
    }

    /**
     * Constructor por defecto usando almacenamiento en memoria.
     */
    public TokenRevocationService() {
        this(new InMemoryTokenStorage());
    }

    @Override
    protected void performRevocation(String token) {
        tokenStorage.addToken(token);
    }

    @Override
    public void revokeAllTokensForUser(String username) {
        // En una implementación completa, mantendrías una relación usuario-token
        // Por ahora, este método está preparado para futuras implementaciones
    }

    @Override
    protected void onTokenRevoked(String token) {
        // Logging o notificaciones adicionales podrían ir aquí
    }
}