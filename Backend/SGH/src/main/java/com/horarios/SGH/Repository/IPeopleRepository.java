package com.horarios.SGH.Repository;

import com.horarios.SGH.Model.People;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio especializado para personas siguiendo principios SOLID.
 * Extiende AbstractRepository para operaciones comunes y añade consultas específicas del dominio de personas.
 *
 * Implementa el patrón Repository con consultas optimizadas para gestión de datos personales.
 * Aplica el patrón Factory a través de RepositoryFactory para consultas dinámicas.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única - gestión de datos personales
 * - OCP: Extensible mediante Specifications para filtros de personas
 * - LSP: Compatible con JpaRepository y AbstractRepository
 * - ISP: Interface específica para personas
 * - DIP: Depende de abstracciones, no implementaciones concretas
 *
 * Patrón Abstract aplicado: Extiende AbstractRepository para operaciones comunes
 * Patrón Factory aplicado: Usa RepositoryFactory para consultas dinámicas de personas
 *
 * @author Sistema SGH
 * @version 2.0 - Refactorizado con patrones SOLID
 */
@Repository
public interface IPeopleRepository extends AbstractRepository<People, Integer> {

    // ==================== IMPLEMENTACIÓN DE MÉTODOS ABSTRACTOS ====================

    /**
     * {@inheritDoc}
     * Para personas, considera "activas" todas (no hay eliminación lógica).
     */
    @Override
    @Query("SELECT p FROM People p ORDER BY p.fullName ASC")
    Page<People> findActive(Pageable pageable);

    /**
     * {@inheritDoc}
     * Busca personas por fecha de creación en un rango.
     */
    @Override
    @Query("SELECT p FROM People p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    Page<People> findByCreatedDateBetween(LocalDateTime startDate,
                                         LocalDateTime endDate,
                                         Pageable pageable);

    /**
     * {@inheritDoc}
     * Cuenta todas las personas.
     */
    @Override
    default long countActive() {
        return count();
    }

    /**
     * {@inheritDoc}
     * Las personas siempre existen si tienen ID.
     */
    @Override
    default boolean existsActiveById(Integer id) {
        return existsById(id);
    }

    /**
     * {@inheritDoc}
     * Las personas siempre están "activas".
     */
    @Override
    default Optional<People> findActiveById(Integer id) {
        return findById(id);
    }

    /**
     * {@inheritDoc}
     * Busca personas por términos de búsqueda en nombre completo o email.
     */
    @Override
    @Query("SELECT p FROM People p WHERE LOWER(p.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(p.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<People> searchByTerm(String searchTerm, Pageable pageable);

    // ==================== MÉTODOS ESPECÍFICOS DEL DOMINIO ====================

    /**
     * Busca una persona por su email exacto.
     * Método optimizado para validaciones de unicidad y autenticación.
     *
     * @param email email a buscar
     * @return Optional con la persona encontrada
     */
    Optional<People> findByEmail(String email);

    /**
     * Busca personas que contienen un término en su nombre completo o email.
     * Método para autocompletado y búsquedas.
     *
     * @param term término de búsqueda
     * @return lista de personas que coinciden
     */
    @Query("SELECT p FROM People p WHERE LOWER(p.fullName) LIKE LOWER(CONCAT('%', :term, '%')) OR LOWER(p.email) LIKE LOWER(CONCAT('%', :term, '%')) ORDER BY p.fullName")
    List<People> findByNameOrEmailContaining(@Param("term") String term);

    /**
     * Busca personas con foto de perfil.
     * Método para dashboard de usuarios con avatar.
     *
     * @return lista de personas que tienen foto
     */
    @Query("SELECT p FROM People p WHERE p.photoData IS NOT NULL ORDER BY p.fullName")
    List<People> findPeopleWithPhoto();

    /**
     * Verifica si un email está disponible (no usado por otra persona).
     * Método de validación para registro de usuarios.
     *
     * @param email email a verificar
     * @param excludeId ID de persona a excluir (para actualizaciones)
     * @return true si el email está disponible
     */
    @Query("SELECT COUNT(p) = 0 FROM People p WHERE p.email = :email AND (:excludeId IS NULL OR p.id != :excludeId)")
    boolean isEmailAvailable(@Param("email") String email, @Param("excludeId") Integer excludeId);

    /**
     * Busca personas ordenadas por fecha de creación (más recientes primero).
     * Método para dashboard administrativo.
     *
     * @param pageable configuración de paginación
     * @return página de personas recientes
     */
    @Query("SELECT p FROM People p ORDER BY p.createdAt DESC")
    Page<People> findRecentPeople(Pageable pageable);

    /**
     * Cuenta personas creadas en un período específico.
     * Método para estadísticas de crecimiento.
     *
     * @param startDate fecha de inicio
     * @param endDate fecha de fin
     * @return número de personas creadas en el período
     */
    @Query("SELECT COUNT(p) FROM People p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    long countPeopleCreatedBetween(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);
}