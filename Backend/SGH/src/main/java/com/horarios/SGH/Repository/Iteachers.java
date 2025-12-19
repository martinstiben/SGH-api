package com.horarios.SGH.Repository;

import com.horarios.SGH.Model.teachers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio especializado para docentes siguiendo principios SOLID.
 * Extiende AbstractRepository para operaciones comunes y añade consultas específicas del dominio docente.
 *
 * Implementa el patrón Repository con consultas optimizadas para gestión de profesores.
 * Aplica el patrón Factory a través de RepositoryFactory para consultas dinámicas.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única - gestión de docentes
 * - OCP: Extensible mediante Specifications para filtros docentes
 * - LSP: Compatible con JpaRepository y AbstractRepository
 * - ISP: Interface específica para profesores
 * - DIP: Depende de abstracciones, no implementaciones concretas
 *
 * Patrón Abstract aplicado: Extiende AbstractRepository para operaciones comunes
 * Patrón Factory aplicado: Usa RepositoryFactory para consultas dinámicas de profesores
 *
 * @author Sistema SGH
 * @version 2.0 - Refactorizado con patrones SOLID
 */
@Repository
public interface Iteachers extends AbstractRepository<teachers, Integer> {

    // ==================== IMPLEMENTACIÓN DE MÉTODOS ABSTRACTOS ====================

    /**
     * {@inheritDoc}
     * Para profesores, considera "activos" todos (no hay eliminación lógica).
     */
    @Override
    @Query("SELECT t FROM teachers t ORDER BY t.teacherName ASC")
    Page<teachers> findActive(Pageable pageable);

    /**
     * {@inheritDoc}
     * Busca profesores por fecha de creación en un rango.
     */
    @Override
    @Query("SELECT t FROM teachers t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    Page<teachers> findByCreatedDateBetween(LocalDateTime startDate,
                                           LocalDateTime endDate,
                                           Pageable pageable);

    /**
     * {@inheritDoc}
     * Cuenta todos los profesores.
     */
    @Override
    default long countActive() {
        return count();
    }

    /**
     * {@inheritDoc}
     * Los profesores siempre existen si tienen ID.
     */
    @Override
    default boolean existsActiveById(Integer id) {
        return existsById(id);
    }

    /**
     * {@inheritDoc}
     * Los profesores siempre están "activos".
     */
    @Override
    default Optional<teachers> findActiveById(Integer id) {
        return findById(id);
    }

    /**
     * {@inheritDoc}
     * Busca profesores por términos de búsqueda en nombre.
     */
    @Override
    @Query("SELECT t FROM teachers t WHERE LOWER(t.teacherName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<teachers> searchByTerm(String searchTerm, Pageable pageable);

    // ==================== MÉTODOS ESPECÍFICOS DEL DOMINIO ====================

    /**
     * Busca profesores que contienen un término en su nombre.
     * Método para autocompletado y búsquedas.
     *
     * @param name término de búsqueda
     * @return lista de profesores que coinciden
     */
    @Query("SELECT t FROM teachers t WHERE LOWER(t.teacherName) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY t.teacherName")
    List<teachers> findByTeacherNameContaining(@Param("name") String name);

    /**
     * Busca profesores con foto de perfil.
     * Método para dashboard de profesores con avatar.
     *
     * @return lista de profesores que tienen foto
     */
    @Query("SELECT t FROM teachers t WHERE t.photoData IS NOT NULL ORDER BY t.teacherName")
    List<teachers> findTeachersWithPhoto();

    /**
     * Cuenta el número de materias asignadas a cada profesor.
     * Método para estadísticas de carga docente.
     *
     * @return lista de arrays [teacherId, teacherName, subjectCount]
     */
    @Query("SELECT t.id, t.teacherName, COUNT(ts) FROM teachers t LEFT JOIN t.teacherSubjects ts GROUP BY t.id, t.teacherName ORDER BY COUNT(ts) DESC")
    List<Object[]> countSubjectsByTeacher();

    /**
     * Busca profesores que no tienen materias asignadas.
     * Método para identificar profesores disponibles.
     *
     * @return lista de profesores sin asignaciones
     */
    @Query("SELECT t FROM teachers t WHERE t.id NOT IN (SELECT DISTINCT ts.teacher.id FROM TeacherSubject ts)")
    List<teachers> findTeachersWithoutSubjects();

    /**
     * Verifica si un profesor tiene horarios asignados.
     * Método de validación para eliminación.
     *
     * @param teacherId ID del profesor
     * @return true si tiene horarios asignados
     */
    @Query("SELECT COUNT(sch) > 0 FROM schedule sch WHERE sch.teacherId.id = :teacherId")
    boolean hasAssignedSchedules(@Param("teacherId") Integer teacherId);

    /**
     * Verifica si un profesor es director de algún curso.
     * Método de validación para eliminación.
     *
     * @param teacherId ID del profesor
     * @return true si es director de grado
     */
    @Query("SELECT COUNT(c) > 0 FROM courses c WHERE c.gradeDirector.id = :teacherId")
    boolean isGradeDirector(@Param("teacherId") Integer teacherId);

    /**
     * Busca profesores ordenados por carga de trabajo (número de horarios).
     * Método para distribución equitativa de carga docente.
     *
     * @return lista de profesores ordenados por carga
     */
    @Query("SELECT t, COUNT(sch) as scheduleCount FROM teachers t LEFT JOIN t.schedules sch GROUP BY t ORDER BY COUNT(sch) ASC")
    List<Object[]> findTeachersOrderedByWorkload();

    /**
     * Busca profesores disponibles en un día y horario específico.
     * Método para asignación automática de horarios.
     *
     * @param day día de la semana
     * @param startTime hora de inicio
     * @param endTime hora de fin
     * @return lista de profesores disponibles
     */
    @Query("SELECT DISTINCT t FROM teachers t " +
           "JOIN t.teacherAvailabilities ta " +
           "WHERE ta.day = :day AND " +
           "((ta.amStart <= :startTime AND ta.amEnd >= :endTime) OR " +
           "(ta.pmStart <= :startTime AND ta.pmEnd >= :endTime))")
    List<teachers> findAvailableTeachers(@Param("day") com.horarios.SGH.Model.Days day,
                                       @Param("startTime") java.time.LocalTime startTime,
                                       @Param("endTime") java.time.LocalTime endTime);
}