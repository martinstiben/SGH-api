package com.horarios.SGH.Repository;

import com.horarios.SGH.Model.RolePermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio especializado para relaciones rol-permiso siguiendo principios SOLID.
 * Extiende AbstractRepository para operaciones comunes y añade consultas específicas del dominio de relaciones.
 *
 * Implementa el patrón Repository con consultas optimizadas para gestión de relaciones muchos-a-muchos.
 * Aplica el patrón Factory a través de RepositoryFactory para consultas dinámicas.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única - gestión de relaciones rol-permiso
 * - OCP: Extensible mediante Specifications para filtros de relaciones
 * - LSP: Compatible con JpaRepository y AbstractRepository
 * - ISP: Interface específica para relaciones
 * - DIP: Depende de abstracciones, no implementaciones concretas
 *
 * Patrón Abstract aplicado: Extiende AbstractRepository para operaciones comunes
 * Patrón Factory aplicado: Usa RepositoryFactory para consultas dinámicas de relaciones
 *
 * @author Sistema SGH
 * @version 2.0 - Refactorizado con patrones SOLID
 */
@Repository
public interface IPermissionsRolesRepository extends AbstractRepository<RolePermission, Long> {

    // ==================== IMPLEMENTACIÓN DE MÉTODOS ABSTRACTOS ====================

    /**
     * {@inheritDoc}
     * Para relaciones rol-permiso, considera "activas" todas (no hay eliminación lógica).
     */
    @Override
    @Query("SELECT rp FROM RolePermission rp ORDER BY rp.role.roleName, rp.permission.permissionName")
    Page<RolePermission> findActive(Pageable pageable);

    /**
     * {@inheritDoc}
     * Busca relaciones por fecha de creación en un rango.
     */
    @Override
    @Query("SELECT rp FROM RolePermission rp WHERE rp.createdAt BETWEEN :startDate AND :endDate")
    Page<RolePermission> findByCreatedDateBetween(LocalDateTime startDate,
                                                 LocalDateTime endDate,
                                                 Pageable pageable);

    /**
     * {@inheritDoc}
     * Cuenta todas las relaciones.
     */
    @Override
    default long countActive() {
        return count();
    }

    /**
     * {@inheritDoc}
     * Las relaciones siempre existen si tienen ID.
     */
    @Override
    default boolean existsActiveById(Long id) {
        return existsById(id);
    }

    /**
     * {@inheritDoc}
     * Las relaciones siempre están "activas".
     */
    @Override
    default Optional<RolePermission> findActiveById(Long id) {
        return findById(id);
    }

    /**
     * {@inheritDoc}
     * Busca relaciones por términos de búsqueda en rol o permiso.
     */
    @Override
    @Query("SELECT rp FROM RolePermission rp WHERE LOWER(rp.role.roleName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(rp.permission.permissionName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<RolePermission> searchByTerm(String searchTerm, Pageable pageable);

    // ==================== MÉTODOS ESPECÍFICOS DEL DOMINIO ====================

    /**
     * Busca todas las relaciones de permisos para un rol específico.
     * Método optimizado para consultas de permisos por rol.
     *
     * @param roleId ID del rol
     * @return lista de relaciones de permisos del rol
     */
    List<RolePermission> findByRole_RoleId(Long roleId);

    /**
     * Busca todas las relaciones de roles para un permiso específico.
     * Método optimizado para consultas de roles por permiso.
     *
     * @param permissionId ID del permiso
     * @return lista de relaciones de roles del permiso
     */
    List<RolePermission> findByPermission_PermissionId(Long permissionId);

    /**
     * Verifica si existe una relación específica entre rol y permiso.
     * Método de validación para asignaciones duplicadas.
     *
     * @param roleId ID del rol
     * @param permissionId ID del permiso
     * @return true si existe la relación
     */
    @Query("SELECT COUNT(rp) > 0 FROM RolePermission rp WHERE rp.role.id = :roleId AND rp.permission.id = :permissionId")
    boolean existsByRoleIdAndPermissionId(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    /**
     * Busca una relación específica entre rol y permiso.
     * Método para consultas puntuales.
     *
     * @param roleId ID del rol
     * @param permissionId ID del permiso
     * @return Optional con la relación encontrada
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.role.id = :roleId AND rp.permission.id = :permissionId")
    Optional<RolePermission> findByRoleIdAndPermissionId(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

    /**
     * Cuenta permisos asignados a un rol.
     * Método para estadísticas de permisos por rol.
     *
     * @param roleId ID del rol
     * @return número de permisos asignados
     */
    @Query("SELECT COUNT(rp) FROM RolePermission rp WHERE rp.role.id = :roleId")
    long countPermissionsByRole(@Param("roleId") Long roleId);

    /**
     * Cuenta roles que tienen un permiso específico.
     * Método para estadísticas de roles por permiso.
     *
     * @param permissionId ID del permiso
     * @return número de roles que tienen el permiso
     */
    @Query("SELECT COUNT(rp) FROM RolePermission rp WHERE rp.permission.id = :permissionId")
    long countRolesByPermission(@Param("permissionId") Long permissionId);

    /**
     * Busca relaciones creadas en un rango de fechas.
     * Método para auditoría de asignaciones.
     *
     * @param startDate fecha de inicio
     * @param endDate fecha de fin
     * @return lista de relaciones creadas en el período
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.createdAt BETWEEN :startDate AND :endDate ORDER BY rp.createdAt DESC")
    List<RolePermission> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * Elimina todas las relaciones de permisos para un rol específico.
     * Método para limpieza masiva al eliminar roles.
     *
     * @param roleId ID del rol
     */
    @Query("DELETE FROM RolePermission rp WHERE rp.role.id = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * Elimina todas las relaciones de roles para un permiso específico.
     * Método para limpieza masiva al eliminar permisos.
     *
     * @param permissionId ID del permiso
     */
    @Query("DELETE FROM RolePermission rp WHERE rp.permission.id = :permissionId")
    void deleteByPermissionId(@Param("permissionId") Long permissionId);
}