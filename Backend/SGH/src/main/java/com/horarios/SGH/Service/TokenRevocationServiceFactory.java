package com.horarios.SGH.Service;

/**
 * Factory para crear instancias de TokenRevocationService.
 * Implementa el patrón Abstract Factory para crear diferentes tipos de servicios
 * de revocación de tokens según el almacenamiento requerido.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de creación de servicios
 * - OCP: Abierto para extensión con nuevos tipos de storage
 * - LSP: Todas las implementaciones son sustituibles
 * - ISP: Interface específica para creación
 * - DIP: Depende de abstracciones (TokenStorage)
 *
 * Patrones de diseño utilizados:
 * - Abstract Factory: Para crear familias de objetos relacionados
 * - Factory Method: Para métodos de creación específicos
 *
 * @author Sistema SGH
 * @version 1.0
 */
public class TokenRevocationServiceFactory {

    /**
     * Tipos de almacenamiento disponibles para tokens revocados.
     * Cada tipo define una estrategia diferente de almacenamiento y gestión.
     */
    public enum StorageType {
        /** Almacenamiento en memoria (volátil, para desarrollo/testing) */
        IN_MEMORY,
        /** Almacenamiento persistente en base de datos (producción) */
        DATABASE,
        /** Almacenamiento distribuido en Redis (escalabilidad) */
        REDIS,
        /** Almacenamiento híbrido (memoria + persistencia) */
        HYBRID
    }

    /**
     * Crea una instancia de TokenRevocationService con el tipo de almacenamiento especificado.
     *
     * @param storageType Tipo de almacenamiento a utilizar
     * @return Servicio de revocación configurado según el tipo
     * @throws IllegalArgumentException si el storageType no es válido
     */
    public static AbstractTokenRevocationService createService(StorageType storageType) {
        if (storageType == null) {
            throw new IllegalArgumentException("El tipo de almacenamiento no puede ser null");
        }

        switch (storageType) {
            case IN_MEMORY:
                return createInMemoryService();
            case DATABASE:
                return createDatabaseService();
            case REDIS:
                return createRedisService();
            case HYBRID:
                return createHybridService();
            default:
                throw new IllegalArgumentException("Tipo de almacenamiento no soportado: " + storageType);
        }
    }

    /**
     * Crea una instancia de TokenRevocationService con almacenamiento en memoria.
     * Ideal para desarrollo, testing y casos de uso temporales.
     *
     * @return Servicio de revocación con almacenamiento en memoria
     */
    public static AbstractTokenRevocationService createInMemoryService() {
        TokenStorage storage = new InMemoryTokenStorage();
        return new TokenRevocationService(storage);
    }

    /**
     * Crea una instancia de TokenRevocationService con almacenamiento en base de datos.
     * Ideal para producción cuando se requiere persistencia.
     *
     * @return Servicio de revocación con almacenamiento en base de datos
     * @throws UnsupportedOperationException si la implementación no está disponible
     */
    public static AbstractTokenRevocationService createDatabaseService() {
        // TODO: Implementar DatabaseTokenStorage cuando esté disponible
        throw new UnsupportedOperationException("Implementación de base de datos no disponible aún");
    }

    /**
     * Crea una instancia de TokenRevocationService con almacenamiento en Redis.
     * Ideal para entornos distribuidos que requieren alta escalabilidad.
     *
     * @return Servicio de revocación con almacenamiento en Redis
     * @throws UnsupportedOperationException si la implementación no está disponible
     */
    public static AbstractTokenRevocationService createRedisService() {
        // TODO: Implementar RedisTokenStorage cuando esté disponible
        throw new UnsupportedOperationException("Implementación de Redis no disponible aún");
    }

    /**
     * Crea una instancia de TokenRevocationService con almacenamiento híbrido.
     * Combina memoria para acceso rápido y persistencia para durabilidad.
     *
     * @return Servicio de revocación con almacenamiento híbrido
     * @throws UnsupportedOperationException si la implementación no está disponible
     */
    public static AbstractTokenRevocationService createHybridService() {
        // TODO: Implementar HybridTokenStorage cuando esté disponible
        throw new UnsupportedOperationException("Implementación híbrida no disponible aún");
    }

    /**
     * Crea una instancia personalizada de TokenRevocationService.
     * Permite usar implementaciones personalizadas de TokenStorage.
     *
     * @param storage Implementación personalizada de almacenamiento de tokens
     * @return Servicio de revocación configurado
     * @throws IllegalArgumentException si storage es null
     */
    public static AbstractTokenRevocationService createCustomService(TokenStorage storage) {
        if (storage == null) {
            throw new IllegalArgumentException("El almacenamiento de tokens no puede ser null");
        }
        return new TokenRevocationService(storage);
    }

    /**
     * Obtiene una descripción de los tipos de almacenamiento disponibles.
     *
     * @return Descripción de los tipos de storage y sus casos de uso
     */
    public static String getStorageTypesDescription() {
        return """
            Tipos de almacenamiento disponibles:
            
            IN_MEMORY:
            - Almacenamiento volátil en RAM
            - Acceso ultra-rápido
            - Ideal para: desarrollo, testing, casos temporales
            - Persistencia: No (se pierde al reiniciar)
            
            DATABASE:
            - Almacenamiento persistente en base de datos
            - Acceso confiable y durable
            - Ideal para: producción, datos críticos
            - Persistencia: Sí (survive reinicios)
            
            REDIS:
            - Almacenamiento distribuido en Redis
            - Escalabilidad horizontal
            - Ideal para: microservicios, alta concurrencia
            - Persistencia: Configurable (RDB/AOF)
            
            HYBRID:
            - Combinación memoria + persistencia
            - Balance óptimo performance/durabilidad
            - Ideal para: producción con alta demanda
            - Persistencia: Dual (memoria + disco)
            """;
    }
}