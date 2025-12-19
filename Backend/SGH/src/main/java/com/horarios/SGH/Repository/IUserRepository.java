package com.horarios.SGH.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.horarios.SGH.Model.AccountStatus;
import com.horarios.SGH.Model.Role;
import com.horarios.SGH.Model.User;

/**
 * Repositorio especializado para la entidad User siguiendo los principios SOLID.
 * Extiende AbstractRepository para operaciones comunes y añade consultas específicas del dominio de usuarios.
 *
 * Implementa el patrón Repository con consultas optimizadas usando JOIN FETCH para evitar N+1 queries.
 * Aplica el patrón Specification a través de RepositoryFactory para consultas dinámicas.
 *
 * Principios SOLID aplicados:
 * - SRP: Una sola responsabilidad - gestión de usuarios
 * - OCP: Extensible mediante Specifications
 * - LSP: Compatible con JpaRepository
 * - ISP: Interface específica para usuarios
 * - DIP: Depende de abstracciones, no implementaciones
 *
 * @author Sistema SGH
 * @version 2.0 - Refactorizado con patrones SOLID
 */
@Repository
public interface IUserRepository extends AbstractRepository<User, Long> {

    // ==================== IMPLEMENTACIÓN DE MÉTODOS ABSTRACTOS ====================

    /**
     * {@inheritDoc}
     * Para usuarios, considera "activos" aquellos con estado ACTIVE.
     */
    @Override
    @Query("SELECT u FROM User u WHERE u.accountStatus = 'ACTIVE'")
    Page<User> findActive(Pageable pageable);

    /**
     * {@inheritDoc}
     * Busca usuarios por fecha de creación en un rango.
     */
    @Override
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :startDate AND :endDate")
    Page<User> findByCreatedDateBetween(java.time.LocalDateTime startDate,
                                       java.time.LocalDateTime endDate,
                                       Pageable pageable);

    /**
     * {@inheritDoc}
     * Cuenta usuarios activos (estado ACTIVE).
     */
    @Override
    @Query("SELECT COUNT(u) FROM User u WHERE u.accountStatus = 'ACTIVE'")
    long countActive();

    /**
     * {@inheritDoc}
     * Verifica si existe un usuario activo con el ID especificado.
     */
    @Override
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.userId = :id AND u.accountStatus = 'ACTIVE'")
    boolean existsActiveById(Long id);

    /**
     * {@inheritDoc}
     * Busca un usuario activo por su ID.
     */
    @Override
    @Query("SELECT u FROM User u WHERE u.userId = :id AND u.accountStatus = 'ACTIVE'")
    Optional<User> findActiveById(Long id);

    /**
     * {@inheritDoc}
     * Busca usuarios por términos de búsqueda en nombre completo o email.
     */
    @Override
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.person p " +
           "WHERE LOWER(p.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(p.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<User> searchByTerm(String searchTerm, Pageable pageable);

    // ==================== MÉTODOS ESPECÍFICOS DEL DOMINIO ====================

    /**
     * Encuentra un usuario por email de la persona asociada.
     * Método optimizado para autenticación.
     *
     * @param email email a buscar
     * @return Optional con el usuario encontrado
     */
    Optional<User> findByPerson_Email(String email);

    /**
     * Verifica si existe un usuario con el email especificado.
     * 
     * @param email email a verificar
     * @return true si existe un usuario con ese email
     */
    boolean existsByPerson_Email(String email);

    /**
     * Encuentra usuarios por rol específico usando una relación Many-to-Many.
     * 
     * @param role rol a buscar
     * @return lista de usuarios con el rol especificado
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r = :role")
    List<User> findByRole(@Param("role") Role role);

    /**
     * Encuentra usuarios por estado de cuenta con información detallada.
     * Optimizado para evitar consultas N+1.
     * 
     * @param status estado de cuenta a buscar
     * @return lista de usuarios con el estado especificado
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.person p " +
           "LEFT JOIN FETCH u.roles r " +
           "WHERE u.accountStatus = :status")
    List<User> findByAccountStatusWithDetails(@Param("status") AccountStatus status);

    /**
     * Encuentra usuarios por nombre de rol con información detallada.
     * Optimizado para evitar consultas N+1.
     * 
     * @param roleName nombre del rol a buscar
     * @return lista de usuarios con el rol especificado
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN u.roles r " +
           "LEFT JOIN FETCH u.person p " +
           "WHERE r.roleName = :roleName")
    List<User> findByRoleNameWithDetails(@Param("roleName") String roleName);

    /**
     * Encuentra usuarios por curso con información detallada.
     * Optimizado para evitar consultas N+1.
     * 
     * @param courseId ID del curso a buscar
     * @return lista de usuarios del curso especificado
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.person p " +
           "LEFT JOIN FETCH u.roles r " +
           "WHERE u.course.id = :courseId")
    List<User> findByCourseIdWithDetails(@Param("courseId") Long courseId);

    /**
     * Encuentra usuarios verificados con información completa.
     * 
     * @return lista de usuarios verificados
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.person p " +
           "LEFT JOIN FETCH u.roles r " +
           "WHERE u.isVerified = true")
    List<User> findVerifiedUsersWithDetails();

    /**
     * Encuentra usuarios activos con información completa.
     * 
     * @return lista de usuarios activos
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.person p " +
           "LEFT JOIN FETCH u.roles r " +
           "WHERE u.accountStatus = 'ACTIVE'")
    List<User> findActiveUsersWithDetails();

    /**
     * Busca usuarios por nombre completo o email.
     * 
     * @param searchTerm término de búsqueda
     * @return lista de usuarios que coinciden con la búsqueda
     */
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.person p " +
           "WHERE LOWER(p.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(p.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchUsers(@Param("searchTerm") String searchTerm);

    /**
     * Cuenta usuarios por rol específico.
     * 
     * @param roleName nombre del rol
     * @return número de usuarios con el rol especificado
     */
    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.roleName = :roleName")
    long countByRoleName(@Param("roleName") String roleName);

    /**
     * Encuentra usuarios sin curso asignado.
     * 
     * @return lista de usuarios sin curso
     */
    @Query("SELECT u FROM User u WHERE u.course IS NULL")
    List<User> findUsersWithoutCourse();

    /**
     * Para compatibilidad con autenticación - busca por nombre de usuario.
     * En este sistema, el username se basa en el email de la persona.
     * 
     * @param userName nombre de usuario
     * @return Optional con el usuario encontrado
     */
    default Optional<User> findByUserName(String userName) {
        return findByPerson_Email(userName);
    }

    /**
     * Para compatibilidad con autenticación - verifica existencia por nombre de usuario.
     * 
     * @param userName nombre de usuario
     * @return true si existe el usuario
     */
    default boolean existsByUserName(String userName) {
        return existsByPerson_Email(userName);
    }
}