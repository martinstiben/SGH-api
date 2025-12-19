package com.horarios.SGH.Repository;

import com.horarios.SGH.Model.schedule;
import com.horarios.SGH.Model.Days;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio especializado para horarios académicos siguiendo principios SOLID.
 * Extiende AbstractRepository para operaciones comunes y añade consultas específicas del dominio académico.
 *
 * Implementa el patrón Repository con consultas optimizadas usando JOIN FETCH para evitar N+1 queries.
 * Aplica el patrón Factory a través de RepositoryFactory para consultas dinámicas de horarios.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única - gestión de horarios académicos
 * - OCP: Extensible mediante Specifications para filtros académicos
 * - LSP: Compatible con JpaRepository y AbstractRepository
 * - ISP: Interface específica para horarios
 * - DIP: Depende de abstracciones, no implementaciones concretas
 *
 * Patrón Abstract aplicado: Extiende AbstractRepository para operaciones comunes
 * Patrón Factory aplicado: Usa RepositoryFactory para consultas dinámicas de horarios
 *
 * @author Sistema SGH
 * @version 2.0 - Refactorizado con patrones SOLID
 */
@Repository
public interface IScheduleRepository extends AbstractRepository<schedule, Integer> {

    // ==================== IMPLEMENTACIÓN DE MÉTODOS ABSTRACTOS ====================

    /**
     * {@inheritDoc}
     * Para horarios, considera "activos" todos los registros (no hay eliminación lógica).
     */
    @Override
    @Query("SELECT s FROM schedule s ORDER BY s.createdAt DESC")
    Page<schedule> findActive(Pageable pageable);

    /**
     * {@inheritDoc}
     * Busca horarios por fecha de creación en un rango.
     */
    @Override
    @Query("SELECT s FROM schedule s WHERE s.createdAt BETWEEN :startDate AND :endDate")
    Page<schedule> findByCreatedDateBetween(LocalDateTime startDate,
                                           LocalDateTime endDate,
                                           Pageable pageable);

    /**
     * {@inheritDoc}
     * Cuenta todos los horarios (no hay concepto de "inactivos").
     */
    @Override
    default long countActive() {
        return count();
    }

    /**
     * {@inheritDoc}
     * Los horarios siempre existen si tienen ID.
     */
    @Override
    default boolean existsActiveById(Integer id) {
        return existsById(id);
    }

    /**
     * {@inheritDoc}
     * Los horarios siempre están "activos".
     */
    @Override
    default Optional<schedule> findActiveById(Integer id) {
        return findById(id);
    }

    /**
     * {@inheritDoc}
     * Busca horarios por términos de búsqueda en nombre o materia.
     */
    @Override
    @Query("SELECT s FROM schedule s LEFT JOIN s.subjectId sub WHERE " +
           "LOWER(s.scheduleName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(sub.subjectName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<schedule> searchByTerm(String searchTerm, Pageable pageable);

    // ==================== MÉTODOS ESPECÍFICOS DEL DOMINIO ====================

    /**
     * Busca horarios por nombre del horario.
     * Método optimizado para consultas por nombre.
     *
     * @param scheduleName nombre del horario a buscar
     * @return lista de horarios con ese nombre
     */
    List<schedule> findByScheduleName(String scheduleName);

    /**
     * Verifica si existe algún horario para una materia específica.
     * Método de validación para eliminación de materias.
     *
     * @param subjectId ID de la materia
     * @return true si existe al menos un horario para la materia
     */
    boolean existsBySubjectId_Id(Integer subjectId);

    /**
     * Busca horarios completos por curso con JOIN FETCH para evitar N+1 queries.
     * Optimizado para dashboards de estudiantes.
     *
     * @param courseId ID del curso
     * @return lista completa de horarios del curso
     */
    @Query("SELECT s FROM schedule s " +
           "LEFT JOIN FETCH s.teacherId t " +
           "LEFT JOIN FETCH s.subjectId sub " +
           "LEFT JOIN FETCH s.courseId c " +
           "WHERE s.courseId.id = :courseId " +
           "ORDER BY s.day, s.startTime")
    List<schedule> findByCourseId(@Param("courseId") Integer courseId);

    /**
     * Busca horarios completos por profesor con JOIN FETCH para evitar N+1 queries.
     * Optimizado para dashboards de profesores.
     *
     * @param teacherId ID del profesor
     * @return lista completa de horarios del profesor
     */
    @Query("SELECT s FROM schedule s " +
           "LEFT JOIN FETCH s.teacherId t " +
           "LEFT JOIN FETCH s.subjectId sub " +
           "LEFT JOIN FETCH s.courseId c " +
           "WHERE s.teacherId.id = :teacherId " +
           "ORDER BY s.day, s.startTime")
    List<schedule> findByTeacherId(@Param("teacherId") Integer teacherId);

    /**
     * Busca horarios por día de la semana.
     * Método optimizado para consultas por día.
     *
     * @param day día de la semana
     * @return lista de horarios para ese día
     */
    @Query("SELECT s FROM schedule s WHERE s.day = :day ORDER BY s.startTime")
    List<schedule> findByDay(@Param("day") Days day);

    /**
     * Busca horarios en un rango horario específico.
     * Útil para detectar conflictos de horarios.
     *
     * @param day día de la semana
     * @param startTime hora de inicio
     * @param endTime hora de fin
     * @return lista de horarios que se solapan con el rango
     */
    @Query("SELECT s FROM schedule s WHERE s.day = :day AND " +
           "((s.startTime <= :startTime AND s.endTime > :startTime) OR " +
           "(s.startTime < :endTime AND s.endTime >= :endTime) OR " +
           "(s.startTime >= :startTime AND s.endTime <= :endTime))")
    List<schedule> findOverlappingSchedules(@Param("day") Days day,
                                          @Param("startTime") LocalTime startTime,
                                          @Param("endTime") LocalTime endTime);

    /**
     * Elimina todos los horarios de un día específico.
     * Operación transaccional para limpieza masiva.
     *
     * @param day día a eliminar
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM schedule s WHERE s.day = :day")
    void deleteByDay(@Param("day") Days day);

    /**
     * Elimina todos los horarios de un día específico (versión String para compatibilidad).
     * Operación transaccional para limpieza masiva.
     *
     * @param day día a eliminar como String
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM schedule s WHERE s.day = :day")
    void deleteByDayString(@Param("day") String day);

    /**
     * Cuenta horarios por profesor.
     * Método para estadísticas de carga docente.
     *
     * @param teacherId ID del profesor
     * @return número de horarios asignados al profesor
     */
    @Query("SELECT COUNT(s) FROM schedule s WHERE s.teacherId.id = :teacherId")
    long countByTeacherId(@Param("teacherId") Integer teacherId);

    /**
     * Cuenta horarios por curso.
     * Método para estadísticas de carga académica.
     *
     * @param courseId ID del curso
     * @return número de horarios asignados al curso
     */
    @Query("SELECT COUNT(s) FROM schedule s WHERE s.courseId.id = :courseId")
    long countByCourseId(@Param("courseId") Integer courseId);

    /**
     * Busca horarios por materia.
     * Método para consultas de carga por materia.
     *
     * @param subjectId ID de la materia
     * @return lista de horarios para la materia
     */
    @Query("SELECT s FROM schedule s WHERE s.subjectId.id = :subjectId ORDER BY s.day, s.startTime")
    List<schedule> findBySubjectId(@Param("subjectId") Integer subjectId);
}