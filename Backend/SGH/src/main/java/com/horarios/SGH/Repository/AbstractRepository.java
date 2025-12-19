package com.horarios.SGH.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║                    ABSTRACT REPOSITORY - Patrón Template Method           ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║  Interfaz base abstracta que define contratos comunes para todos los     ║
 * ║  repositorios del sistema SGH, implementando el patrón Template Method.   ║
 * ║                                                                          ║
 * ║  PROPÓSITO:                                                             ║
 * ║  • Centralizar operaciones CRUD comunes                                 ║
 * ║  • Establecer contratos para consultas especializadas                   ║
 * ║  • Proporcionar métodos template para comportamientos estándar          ║
 * ║  • Garantizar consistencia en la capa de acceso a datos                 ║
 * ║                                                                          ║
 * ║  PATRÓN TEMPLATE METHOD aplicado:                                       ║
 * ║  • findActive() - Método template para entidades "activas"              ║
 * ║  • searchByTerm() - Método template para búsquedas genéricas           ║
 * ║  • countActive() - Método template para conteos                         ║
 * ║  • getBasicStats() - Método template con implementación por defecto     ║
 * ║                                                                          ║
 * ║  JERARQUÍA DE HERENCIA:                                                 ║
 * ║  ┌─────────────────────────────────────────────────────────────────────┐ ║
 * ║  │                AbstractRepository<T, ID>                           │ ║
 * ║  │  (Esta interfaz - contratos y comportamientos base)               │ ║
 * ║  └─────────────────────────────────────────────────────────────────────┘ ║
 * ║                                   │                                      ║
 * ║                                   ▼                                      ║
 * ║  ┌─────────────────────────────────────────────────────────────────────┐ ║
 * ║  │            Repositorios Concretos (Domain-specific)               │ ║
 * ║  │  • IUserRepository, IScheduleRepository, etc.                     │ ║
 * ║  │  • Implementan métodos abstractos                                  │ ║
 * ║  │  • Añaden consultas específicas del dominio                        │ ║
 * ║  └─────────────────────────────────────────────────────────────────────┘ ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 *
 * MÉTODOS TEMPLATE (Deben ser implementados por repositorios concretos):
 * ✅ findActive(Pageable) - Busca entidades "activas" (sin eliminación lógica)
 * ✅ searchByTerm(String, Pageable) - Búsqueda full-text genérica
 * ✅ countActive() - Cuenta entidades activas
 * ✅ existsActiveById(ID) - Verifica existencia de entidad activa
 * ✅ findActiveById(ID) - Busca entidad activa por ID
 * ✅ findByCreatedDateBetween(LocalDateTime, LocalDateTime, Pageable) - Búsqueda por rango de fechas
 *
 * MÉTODOS CON IMPLEMENTACIÓN POR DEFECTO:
 * 🔄 getBasicStats() - Estadísticas básicas (total, activos, inactivos)
 * 🔄 softDeleteById(ID) - Soft delete (puede ser sobrescrito)
 * 🔄 restoreById(ID) - Restauración de soft delete (puede ser sobrescrito)
 *
 * @param <T>  Tipo de la entidad JPA
 * @param <ID> Tipo del identificador de la entidad (debe ser Serializable)
 *
 * @author Sistema SGH
 * @version 2.0 - Documentado como interfaz base con patrón Template Method
 */
@NoRepositoryBean
public interface AbstractRepository<T, ID extends Serializable>
        extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    /**
     * Busca entidades por criterios usando Specifications.
     * Implementa el patrón Specification para consultas dinámicas.
     *
     * @param spec     especificación de criterios de búsqueda
     * @param pageable configuración de paginación
     * @return página de resultados
     */
    Page<T> findAll(Specification<T> spec, Pageable pageable);

    /**
     * Busca entidades activas (no eliminadas lógicamente) con paginación.
     * Método template que debe ser implementado por repositorios concretos.
     *
     * @param pageable configuración de paginación
     * @return página de entidades activas
     */
    Page<T> findActive(Pageable pageable);

    /**
     * Busca entidades por fecha de creación en un rango.
     *
     * @param startDate fecha de inicio (inclusive)
     * @param endDate   fecha de fin (inclusive)
     * @param pageable  configuración de paginación
     * @return página de resultados
     */
    Page<T> findByCreatedDateBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Cuenta entidades activas en el sistema.
     *
     * @return número de entidades activas
     */
    long countActive();

    /**
     * Verifica si existe una entidad con el ID especificado y está activa.
     *
     * @param id identificador de la entidad
     * @return true si existe y está activa
     */
    boolean existsActiveById(ID id);

    /**
     * Busca una entidad activa por su ID.
     *
     * @param id identificador de la entidad
     * @return Optional con la entidad si existe y está activa
     */
    Optional<T> findActiveById(ID id);

    /**
     * Busca entidades por términos de búsqueda genéricos.
     * Implementa búsqueda full-text básica.
     *
     * @param searchTerm término de búsqueda
     * @param pageable   configuración de paginación
     * @return página de resultados que coinciden con el término
     */
    Page<T> searchByTerm(String searchTerm, Pageable pageable);

    /**
     * Obtiene estadísticas básicas de la entidad.
     * Método template para métricas comunes.
     *
     * @return mapa con estadísticas (total, activos, etc.)
     */
    default java.util.Map<String, Long> getBasicStats() {
        long total = count();
        long active = countActive();
        long inactive = total - active;

        return java.util.Map.of(
            "total", total,
            "active", active,
            "inactive", inactive
        );
    }

    /**
     * Operación de soft delete si la entidad lo soporta.
     * Método template que puede ser sobrescrito.
     *
     * @param id identificador de la entidad
     * @return true si se realizó el soft delete
     */
    default boolean softDeleteById(ID id) {
        // Implementación por defecto - no hace nada
        // Los repositorios concretos pueden sobrescribir si soportan soft delete
        return false;
    }

    /**
     * Restaura una entidad soft-deleted si existe.
     *
     * @param id identificador de la entidad
     * @return Optional con la entidad restaurada
     */
    default Optional<T> restoreById(ID id) {
        // Implementación por defecto - no hace nada
        return Optional.empty();
    }
}