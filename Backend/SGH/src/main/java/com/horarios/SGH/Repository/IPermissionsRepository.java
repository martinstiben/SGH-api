package com.horarios.SGH.Repository;

import com.horarios.SGH.Model.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio especializado para permisos de seguridad siguiendo principios SOLID.
 * Extiende AbstractRepository para operaciones comunes y añade consultas específicas del dominio de permisos.
 *
 * Implementa el patrón Repository con consultas optimizadas para gestión de permisos.
 * Aplica el patrón Factory a través de RepositoryFactory para consultas dinámicas.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única - gestión de permisos de seguridad
 * - OCP: Extensible mediante Specifications para filtros de permisos
 * - LSP: Compatible con JpaRepository y AbstractRepository
 * - ISP: Interface específica para permisos
 * - DIP: Depende de abstracciones, no implementaciones concretas
 *
 * Patrón Abstract aplicado: Extiende AbstractRepository para operaciones comunes
 * Patrón Factory aplicado: Usa RepositoryFactory para consultas dinámicas de permisos
 *
 * @author Sistema SGH
 * @version 2.0 - Refactorizado con patrones SOLID
 */
@Repository
public interface IPermissionsRepository extends AbstractRepository<Permission, Long> {

    // ==================== IMPLEMENTACIÓN DE MÉTODOS ABSTRACTOS ====================

    /**
     * {@inheritDoc}
     * Para permisos, considera "activos" todos (no hay eliminación lógica).
     */
    @Override
    @Query("SELECT p FROM Permission p ORDER BY p.permissionName ASC")
    Page<Permission> findActive(Pageable pageable);

    /**
     * {@inheritDoc}
     * Busca permisos por fecha de creación en un rango.
     */
    @Override
    @Query("SELECT p FROM Permission p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    Page<Permission> findByCreatedDateBetween(LocalDateTime startDate,
                                             LocalDateTime endDate,
                                             Pageable pageable);

    /**
     * {@inheritDoc}
     * Cuenta todos los permisos.
     */
    @Override
    default long countActive() {
        return count();
    }

    /**
     * {@inheritDoc}
     * Los permisos siempre existen si tienen ID.
     */
    @Override
    default boolean existsActiveById(Long id) {
        return existsById(id);
    }

    /**
     * {@inheritDoc}
     * Los permisos siempre están "activos".
     */
    @Override
    default Optional<Permission> findActiveById(Long id) {
        return findById(id);
    }

    /**
     * {@inheritDoc}
     * Busca permisos por términos de búsqueda en nombre o descripción.
     */
    @Override
    @Query("SELECT p FROM Permission p WHERE LOWER(p.permissionName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Permission> searchByTerm(String searchTerm, Pageable pageable);

    // ==================== MÉTODOS ESPECÍFICOS DEL DOMINIO ====================

    /**
     * Busca un permiso por su nombre exacto.
     * Método optimizado para validaciones de autorización.
     *
     * @param permissionName nombre del permiso a buscar
     * @return Optional con el permiso encontrado
     */
    Optional<Permission> findByPermissionName(String permissionName);

    /**
     * Busca permisos que contienen un término en su nombre o descripción.
     * Método para autocompletado y búsquedas administrativas.
     *
     * @param term término de búsqueda
     * @return lista de permisos que coinciden
     */
    @Query("SELECT p FROM Permission p WHERE LOWER(p.permissionName) LIKE LOWER(CONCAT('%', :term, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :term, '%')) ORDER BY p.permissionName")
    List<Permission> findByNameOrDescriptionContaining(@Param("term") String term);

    /**
     * Cuenta el número de roles asignados a cada permiso.
     * Método para estadísticas de distribución de permisos.
     *
     * @return lista de arrays [permissionId, permissionName, roleCount]
     */
    @Query("SELECT p.id, p.permissionName, COUNT(rp) FROM Permission p LEFT JOIN p.rolePermissions rp GROUP BY p.id, p.permissionName ORDER BY COUNT(rp) DESC")
    List<Object[]> countRolesByPermission();

    /**
     * Busca permisos que no están asignados a ningún rol.
     * Método para identificar permisos sin uso.
     *
     * @return lista de permisos sin asignaciones
     */
    @Query("SELECT p FROM Permission p WHERE p.id NOT IN (SELECT DISTINCT rp.permission.id FROM RolePermission rp)")
    List<Permission> findPermissionsWithoutRoles();

    /**
     * Verifica si un permiso está asignado a algún rol.
     * Método de validación para eliminación de permisos.
     *
     * @param permissionId ID del permiso
     * @return true si está asignado a roles
     */
    @Query("SELECT COUNT(rp) > 0 FROM Permission p JOIN p.rolePermissions rp WHERE p.id = :permissionId")
    boolean hasAssignedRoles(@Param("permissionId") Long permissionId);

    /**
     * Busca permisos ordenados por número de roles.
     * Método para análisis de permisos más utilizados.
     *
     * @return lista de permisos ordenados por roles
     */
    @Query("SELECT p, COUNT(rp) as roleCount FROM Permission p LEFT JOIN p.rolePermissions rp GROUP BY p ORDER BY COUNT(rp) DESC")
    List<Object[]> findPermissionsOrderedByRoles();

    /**
     * Busca permisos asignados a un rol específico.
     * Método para consultas de permisos por rol.
     *
     * @param roleId ID del rol
     * @return lista de permisos del rol
     */
    @Query("SELECT p FROM Permission p JOIN p.rolePermissions rp WHERE rp.role.id = :roleId ORDER BY p.permissionName")
    List<Permission> findPermissionsByRole(@Param("roleId") Long roleId);
}
