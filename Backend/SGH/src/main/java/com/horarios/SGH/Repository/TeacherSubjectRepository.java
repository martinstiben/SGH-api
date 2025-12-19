package com.horarios.SGH.Repository;

import com.horarios.SGH.Model.TeacherSubject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio especializado para relaciones profesor-materia siguiendo principios SOLID.
 * Extiende AbstractRepository para operaciones comunes y añade consultas específicas del dominio académico.
 *
 * Implementa el patrón Repository con consultas optimizadas para gestión de asignaciones docente-materia.
 * Aplica el patrón Factory a través de RepositoryFactory para consultas dinámicas.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única - gestión de relaciones profesor-materia
 * - OCP: Extensible mediante Specifications para filtros académicos
 * - LSP: Compatible con JpaRepository y AbstractRepository
 * - ISP: Interface específica para asignaciones académicas
 * - DIP: Depende de abstracciones, no implementaciones concretas
 *
 * Patrón Abstract aplicado: Extiende AbstractRepository para operaciones comunes
 * Patrón Factory aplicado: Usa RepositoryFactory para consultas dinámicas de asignaciones
 *
 * @author Sistema SGH
 * @version 2.0 - Refactorizado con patrones SOLID
 */
@Repository
public interface TeacherSubjectRepository extends AbstractRepository<TeacherSubject, Integer> {

    // ==================== IMPLEMENTACIÓN DE MÉTODOS ABSTRACTOS ====================

    /**
     * {@inheritDoc}
     * Para asignaciones profesor-materia, considera "activas" todas (no hay eliminación lógica).
     */
    @Override
    @Query("SELECT ts FROM TeacherSubject ts ORDER BY ts.teacher.teacherName, ts.subject.subjectName")
    Page<TeacherSubject> findActive(Pageable pageable);

    /**
     * {@inheritDoc}
     * Busca asignaciones por fecha de creación en un rango.
     */
    @Override
    @Query("SELECT ts FROM TeacherSubject ts WHERE ts.createdAt BETWEEN :startDate AND :endDate")
    Page<TeacherSubject> findByCreatedDateBetween(LocalDateTime startDate,
                                                 LocalDateTime endDate,
                                                 Pageable pageable);

    /**
     * {@inheritDoc}
     * Cuenta todas las asignaciones.
     */
    @Override
    default long countActive() {
        return count();
    }

    /**
     * {@inheritDoc}
     * Las asignaciones siempre existen si tienen ID.
     */
    @Override
    default boolean existsActiveById(Integer id) {
        return existsById(id);
    }

    /**
     * {@inheritDoc}
     * Las asignaciones siempre están "activas".
     */
    @Override
    default Optional<TeacherSubject> findActiveById(Integer id) {
        return findById(id);
    }

    /**
     * {@inheritDoc}
     * Busca asignaciones por términos de búsqueda en profesor o materia.
     */
    @Override
    @Query("SELECT ts FROM TeacherSubject ts WHERE LOWER(ts.teacher.teacherName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(ts.subject.subjectName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<TeacherSubject> searchByTerm(String searchTerm, Pageable pageable);

    // ==================== MÉTODOS ESPECÍFICOS DEL DOMINIO ====================

    /**
     * Busca todas las asignaciones para una materia específica.
     * Método optimizado para consultas de profesores por materia.
     *
     * @param subjectId ID de la materia
     * @return lista de asignaciones de la materia
     */
    List<TeacherSubject> findBySubject_Id(Integer subjectId);

    /**
     * Busca todas las asignaciones para un profesor específico.
     * Método optimizado para consultas de materias por profesor.
     *
     * @param teacherId ID del profesor
     * @return lista de asignaciones del profesor
     */
    List<TeacherSubject> findByTeacher_Id(Integer teacherId);

    /**
     * Busca una asignación específica entre profesor y materia.
     * Método para consultas puntuales de asignaciones.
     *
     * @param teacherId ID del profesor
     * @param subjectId ID de la materia
     * @return Optional con la asignación encontrada
     */
    Optional<TeacherSubject> findByTeacher_IdAndSubject_Id(Integer teacherId, Integer subjectId);

    /**
     * Verifica si existe una asignación específica entre profesor y materia.
     * Método de validación para asignaciones duplicadas.
     *
     * @param teacherId ID del profesor
     * @param subjectId ID de la materia
     * @return true si existe la asignación
     */
    boolean existsByTeacher_IdAndSubject_Id(Integer teacherId, Integer subjectId);

    /**
     * Busca asignaciones con información completa (JOIN FETCH).
     * Método optimizado para evitar N+1 queries en listados.
     *
     * @param pageable configuración de paginación
     * @return página de asignaciones con datos completos
     */
    @Query("SELECT ts FROM TeacherSubject ts " +
           "LEFT JOIN FETCH ts.teacher t " +
           "LEFT JOIN FETCH ts.subject s " +
           "ORDER BY t.teacherName, s.subjectName")
    Page<TeacherSubject> findAllWithDetails(Pageable pageable);

    /**
     * Cuenta asignaciones por profesor.
     * Método para estadísticas de carga docente.
     *
     * @param teacherId ID del profesor
     * @return número de materias asignadas al profesor
     */
    @Query("SELECT COUNT(ts) FROM TeacherSubject ts WHERE ts.teacher.id = :teacherId")
    long countSubjectsByTeacher(@Param("teacherId") Integer teacherId);

    /**
     * Cuenta asignaciones por materia.
     * Método para estadísticas de cobertura docente.
     *
     * @param subjectId ID de la materia
     * @return número de profesores asignados a la materia
     */
    @Query("SELECT COUNT(ts) FROM TeacherSubject ts WHERE ts.subject.id = :subjectId")
    long countTeachersBySubject(@Param("subjectId") Integer subjectId);

    /**
     * Busca profesores disponibles para una materia específica.
     * Método para asignación automática de profesores.
     *
     * @param subjectId ID de la materia
     * @return lista de profesores disponibles para la materia
     */
    @Query("SELECT DISTINCT t FROM teachers t WHERE t.id NOT IN " +
           "(SELECT ts.teacher.id FROM TeacherSubject ts WHERE ts.subject.id = :subjectId) " +
           "ORDER BY t.teacherName")
    List<com.horarios.SGH.Model.teachers> findAvailableTeachersForSubject(@Param("subjectId") Integer subjectId);

    /**
     * Busca materias disponibles para un profesor específico.
     * Método para asignación automática de materias.
     *
     * @param teacherId ID del profesor
     * @return lista de materias disponibles para el profesor
     */
    @Query("SELECT DISTINCT s FROM subjects s WHERE s.id NOT IN " +
           "(SELECT ts.subject.id FROM TeacherSubject ts WHERE ts.teacher.id = :teacherId) " +
           "ORDER BY s.subjectName")
    List<com.horarios.SGH.Model.subjects> findAvailableSubjectsForTeacher(@Param("teacherId") Integer teacherId);

    /**
     * Elimina todas las asignaciones de un profesor.
     * Método para limpieza masiva al eliminar profesores.
     *
     * @param teacherId ID del profesor
     */
    @Query("DELETE FROM TeacherSubject ts WHERE ts.teacher.id = :teacherId")
    void deleteByTeacherId(@Param("teacherId") Integer teacherId);

    /**
     * Elimina todas las asignaciones de una materia.
     * Método para limpieza masiva al eliminar materias.
     *
     * @param subjectId ID de la materia
     */
    @Query("DELETE FROM TeacherSubject ts WHERE ts.subject.id = :subjectId")
    void deleteBySubjectId(@Param("subjectId") Integer subjectId);
}