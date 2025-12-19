package com.horarios.SGH.Repository;

import com.horarios.SGH.Model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio especializado para roles de seguridad siguiendo principios SOLID.
 * Extiende AbstractRepository para operaciones comunes y añade consultas específicas del dominio de roles.
 *
 * Implementa el patrón Repository con consultas optimizadas para gestión de roles y permisos.
 * Aplica el patrón Factory a través de RepositoryFactory para consultas dinámicas.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única - gestión de roles de seguridad
 * - OCP: Extensible mediante Specifications para filtros de roles
 * - LSP: Compatible con JpaRepository y AbstractRepository
 * - ISP: Interface específica para roles
 * - DIP: Depende de abstracciones, no implementaciones concretas
 *
 * Patrón Abstract aplicado: Extiende AbstractRepository para operaciones comunes
 * Patrón Factory aplicado: Usa RepositoryFactory para consultas dinámicas de roles
 *
 * @author Sistema SGH
 * @version 2.0 - Refactorizado con patrones SOLID
 */
@Repository
public interface IRolesRepository extends AbstractRepository<Role, Long> {

    // ==================== IMPLEMENTACIÓN DE MÉTODOS ABSTRACTOS ====================

    /**
     * {@inheritDoc}
     * Para roles, considera "activos" todos (no hay eliminación lógica).
     */
    @Override
    @Query("SELECT r FROM Role r ORDER BY r.roleName ASC")
    Page<Role> findActive(Pageable pageable);

    /**
     * {@inheritDoc}
     * Busca roles por fecha de creación en un rango.
     */
    @Override
    @Query("SELECT r FROM Role r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    Page<Role> findByCreatedDateBetween(LocalDateTime startDate,
                                       LocalDateTime endDate,
                                       Pageable pageable);

    /**
     * {@inheritDoc}
     * Cuenta todos los roles.
     */
    @Override
    default long countActive() {
        return count();
    }

    /**
     * {@inheritDoc}
     * Los roles siempre existen si tienen ID.
     */
    @Override
    default boolean existsActiveById(Long id) {
        return existsById(id);
    }

    /**
     * {@inheritDoc}
     * Los roles siempre están "activos".
     */
    @Override
    default Optional<Role> findActiveById(Long id) {
        return findById(id);
    }

    /**
     * {@inheritDoc}
     * Busca roles por términos de búsqueda en nombre o descripción.
     */
    @Override
    @Query("SELECT r FROM Role r WHERE LOWER(r.roleName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Role> searchByTerm(String searchTerm, Pageable pageable);

    // ==================== MÉTODOS ESPECÍFICOS DEL DOMINIO ====================

    /**
     * Busca un rol por su nombre exacto.
     * Método optimizado para validaciones de autenticación.
     *
     * @param roleName nombre del rol a buscar
     * @return Optional con el rol encontrado
     */
    Optional<Role> findByRoleName(String roleName);

    /**
     * Busca roles que contienen un término en su nombre o descripción.
     * Método para autocompletado y búsquedas administrativas.
     *
     * @param term término de búsqueda
     * @return lista de roles que coinciden
     */
    @Query("SELECT r FROM Role r WHERE LOWER(r.roleName) LIKE LOWER(CONCAT('%', :term, '%')) OR LOWER(r.description) LIKE LOWER(CONCAT('%', :term, '%')) ORDER BY r.roleName")
    List<Role> findByNameOrDescriptionContaining(@Param("term") String term);

    /**
     * Cuenta el número de usuarios asignados a cada rol.
     * Método para estadísticas de distribución de roles.
     *
     * @return lista de arrays [roleId, roleName, userCount]
     */
    @Query("SELECT r.id, r.roleName, COUNT(ur) FROM Role r LEFT JOIN r.userRoles ur GROUP BY r.id, r.roleName ORDER BY COUNT(ur) DESC")
    List<Object[]> countUsersByRole();

    /**
     * Busca roles que no tienen usuarios asignados.
     * Método para identificar roles sin uso.
     *
     * @return lista de roles sin asignaciones
     */
    @Query("SELECT r FROM Role r WHERE r.id NOT IN (SELECT DISTINCT ur.role.id FROM UserRole ur)")
    List<Role> findRolesWithoutUsers();

    /**
     * Verifica si un rol tiene usuarios asignados.
     * Método de validación para eliminación de roles.
     *
     * @param roleId ID del rol
     * @return true si tiene usuarios asignados
     */
    @Query("SELECT COUNT(ur) > 0 FROM Role r JOIN r.userRoles ur WHERE r.id = :roleId")
    boolean hasAssignedUsers(@Param("roleId") Long roleId);

    /**
     * Busca roles ordenados por número de permisos.
     * Método para análisis de complejidad de roles.
     *
     * @return lista de roles ordenados por permisos
     */
    @Query("SELECT r, COUNT(rp) as permissionCount FROM Role r LEFT JOIN r.rolePermissions rp GROUP BY r ORDER BY COUNT(rp) DESC")
    List<Object[]> findRolesOrderedByPermissions();

    /**
     * Busca roles que tienen un permiso específico.
     * Método para consultas de permisos por rol.
     *
     * @param permissionId ID del permiso
     * @return lista de roles que tienen el permiso
     */
    @Query("SELECT r FROM Role r JOIN r.rolePermissions rp WHERE rp.permission.id = :permissionId ORDER BY r.roleName")
    List<Role> findRolesByPermission(@Param("permissionId") Long permissionId);
}