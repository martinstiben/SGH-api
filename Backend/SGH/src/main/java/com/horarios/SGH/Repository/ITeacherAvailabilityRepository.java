package com.horarios.SGH.Repository;

import com.horarios.SGH.Model.Days;
import com.horarios.SGH.Model.TeacherAvailability;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio especializado para disponibilidad docente siguiendo principios SOLID.
 * Extiende AbstractRepository para operaciones comunes y añade consultas específicas del dominio de horarios.
 *
 * Implementa el patrón Repository con consultas optimizadas para gestión de disponibilidad docente.
 * Aplica el patrón Factory a través de RepositoryFactory para consultas dinámicas.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única - gestión de disponibilidad docente
 * - OCP: Extensible mediante Specifications para filtros de horarios
 * - LSP: Compatible con JpaRepository y AbstractRepository
 * - ISP: Interface específica para disponibilidad docente
 * - DIP: Depende de abstracciones, no implementaciones concretas
 *
 * Patrón Abstract aplicado: Extiende AbstractRepository para operaciones comunes
 * Patrón Factory aplicado: Usa RepositoryFactory para consultas dinámicas de disponibilidad
 *
 * @author Sistema SGH
 * @version 2.0 - Refactorizado con patrones SOLID
 */
@Repository
public interface ITeacherAvailabilityRepository extends AbstractRepository<TeacherAvailability, Long> {

    // ==================== IMPLEMENTACIÓN DE MÉTODOS ABSTRACTOS ====================

    /**
     * {@inheritDoc}
     * Para disponibilidad docente, considera "activas" todas (no hay eliminación lógica).
     */
    @Override
    @Query("SELECT ta FROM TeacherAvailability ta ORDER BY ta.teacher.teacherName, ta.day")
    Page<TeacherAvailability> findActive(Pageable pageable);

    /**
     * {@inheritDoc}
     * Busca disponibilidades por fecha de creación en un rango.
     */
    @Override
    @Query("SELECT ta FROM TeacherAvailability ta WHERE ta.createdAt BETWEEN :startDate AND :endDate")
    Page<TeacherAvailability> findByCreatedDateBetween(LocalDateTime startDate,
                                                      LocalDateTime endDate,
                                                      Pageable pageable);

    /**
     * {@inheritDoc}
     * Cuenta todas las disponibilidades.
     */
    @Override
    default long countActive() {
        return count();
    }

    /**
     * {@inheritDoc}
     * Las disponibilidades siempre existen si tienen ID.
     */
    @Override
    default boolean existsActiveById(Long id) {
        return existsById(id);
    }

    /**
     * {@inheritDoc}
     * Las disponibilidades siempre están "activas".
     */
    @Override
    default Optional<TeacherAvailability> findActiveById(Long id) {
        return findById(id);
    }

    /**
     * {@inheritDoc}
     * Busca disponibilidades por términos de búsqueda en profesor o día.
     */
    @Override
    @Query("SELECT ta FROM TeacherAvailability ta WHERE LOWER(ta.teacher.teacherName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(ta.day) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<TeacherAvailability> searchByTerm(String searchTerm, Pageable pageable);

    // ==================== MÉTODOS ESPECÍFICOS DEL DOMINIO ====================

    /**
     * Busca disponibilidad de un profesor en un día específico.
     * Método optimizado para validaciones de horarios.
     *
     * @param teacherId ID del profesor
     * @param day día de la semana
     * @return lista de disponibilidades para ese día
     */
    List<TeacherAvailability> findByTeacher_IdAndDay(Integer teacherId, Days day);

    /**
     * Busca toda la disponibilidad de un profesor.
     * Método para dashboards de profesores.
     *
     * @param teacherId ID del profesor
     * @return lista completa de disponibilidad del profesor
     */
    List<TeacherAvailability> findByTeacher_Id(Integer teacherId);

    /**
     * Busca disponibilidad completa con JOIN FETCH para evitar N+1 queries.
     * Optimizado para listados administrativos.
     *
     * @param pageable configuración de paginación
     * @return página de disponibilidades con datos completos
     */
    @Query("SELECT ta FROM TeacherAvailability ta " +
           "LEFT JOIN FETCH ta.teacher t " +
           "ORDER BY t.teacherName, ta.day")
    Page<TeacherAvailability> findAllWithTeacher(Pageable pageable);

    /**
     * Busca profesores disponibles en un horario específico.
     * Método para asignación automática de horarios.
     *
     * @param day día de la semana
     * @param startTime hora de inicio
     * @param endTime hora de fin
     * @return lista de profesores disponibles en ese horario
     */
    @Query("SELECT DISTINCT ta.teacher FROM TeacherAvailability ta " +
           "WHERE ta.day = :day AND " +
           "((ta.amStart <= :startTime AND ta.amEnd >= :endTime) OR " +
           "(ta.pmStart <= :startTime AND ta.pmEnd >= :endTime))")
    List<com.horarios.SGH.Model.teachers> findAvailableTeachers(@Param("day") Days day,
                                                              @Param("startTime") LocalTime startTime,
                                                              @Param("endTime") LocalTime endTime);

    /**
     * Verifica si un profesor tiene disponibilidad en un horario específico.
     * Método de validación para creación de horarios.
     *
     * @param teacherId ID del profesor
     * @param day día de la semana
     * @param startTime hora de inicio
     * @param endTime hora de fin
     * @return true si el profesor está disponible
     */
    @Query("SELECT COUNT(ta) > 0 FROM TeacherAvailability ta " +
           "WHERE ta.teacher.id = :teacherId AND ta.day = :day AND " +
           "((ta.amStart <= :startTime AND ta.amEnd >= :endTime) OR " +
           "(ta.pmStart <= :startTime AND ta.pmEnd >= :endTime))")
    boolean isTeacherAvailable(@Param("teacherId") Integer teacherId,
                              @Param("day") Days day,
                              @Param("startTime") LocalTime startTime,
                              @Param("endTime") LocalTime endTime);

    /**
     * Busca disponibilidad por día de la semana.
     * Método para estadísticas de cobertura horaria.
     *
     * @param day día de la semana
     * @return lista de disponibilidades para ese día
     */
    @Query("SELECT ta FROM TeacherAvailability ta WHERE ta.day = :day ORDER BY ta.teacher.teacherName")
    List<TeacherAvailability> findByDay(@Param("day") Days day);

    /**
     * Cuenta profesores disponibles por día.
     * Método para métricas de disponibilidad docente.
     *
     * @param day día de la semana
     * @return número de profesores disponibles ese día
     */
    @Query("SELECT COUNT(DISTINCT ta.teacher) FROM TeacherAvailability ta WHERE ta.day = :day")
    long countAvailableTeachersByDay(@Param("day") Days day);

    /**
     * Busca profesores sin disponibilidad registrada.
     * Método para identificar profesores que necesitan configuración.
     *
     * @return lista de profesores sin disponibilidad
     */
    @Query("SELECT t FROM teachers t WHERE t.id NOT IN (SELECT DISTINCT ta.teacher.id FROM TeacherAvailability ta)")
    List<com.horarios.SGH.Model.teachers> findTeachersWithoutAvailability();

    /**
     * Elimina toda la disponibilidad de un profesor.
     * Método para limpieza masiva al eliminar profesores.
     *
     * @param teacherId ID del profesor
     */
    @Query("DELETE FROM TeacherAvailability ta WHERE ta.teacher.id = :teacherId")
    void deleteByTeacherId(@Param("teacherId") Integer teacherId);

    /**
     * Busca disponibilidad que se solapa con un horario específico.
     * Método para detectar conflictos de disponibilidad.
     *
     * @param teacherId ID del profesor
     * @param day día de la semana
     * @param startTime hora de inicio
     * @param endTime hora de fin
     * @return lista de disponibilidades que se solapan
     */
    @Query("SELECT ta FROM TeacherAvailability ta " +
           "WHERE ta.teacher.id = :teacherId AND ta.day = :day AND " +
           "((ta.amStart < :endTime AND ta.amEnd > :startTime) OR " +
           "(ta.pmStart < :endTime AND ta.pmEnd > :startTime))")
    List<TeacherAvailability> findOverlappingAvailability(@Param("teacherId") Integer teacherId,
                                                        @Param("day") Days day,
                                                        @Param("startTime") LocalTime startTime,
                                                        @Param("endTime") LocalTime endTime);
}