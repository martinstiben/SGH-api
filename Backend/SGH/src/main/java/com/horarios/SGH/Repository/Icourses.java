package com.horarios.SGH.Repository;

import com.horarios.SGH.Model.courses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio especializado para cursos académicos siguiendo principios SOLID.
 * Extiende AbstractRepository para operaciones comunes y añade consultas específicas del dominio de cursos.
 *
 * Implementa el patrón Repository con consultas optimizadas para gestión de cursos.
 * Aplica el patrón Factory a través de RepositoryFactory para consultas dinámicas.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única - gestión de cursos académicos
 * - OCP: Extensible mediante Specifications para filtros de cursos
 * - LSP: Compatible con JpaRepository y AbstractRepository
 * - ISP: Interface específica para cursos
 * - DIP: Depende de abstracciones, no implementaciones concretas
 *
 * Patrón Abstract aplicado: Extiende AbstractRepository para operaciones comunes
 * Patrón Factory aplicado: Usa RepositoryFactory para consultas dinámicas de cursos
 *
 * @author Sistema SGH
 * @version 2.0 - Refactorizado con patrones SOLID
 */
@Repository
public interface Icourses extends AbstractRepository<courses, Integer> {

    // ==================== IMPLEMENTACIÓN DE MÉTODOS ABSTRACTOS ====================

    /**
     * {@inheritDoc}
     * Para cursos, considera "activos" todos (no hay eliminación lógica).
     */
    @Override
    @Query("SELECT c FROM courses c ORDER BY c.courseName ASC")
    Page<courses> findActive(Pageable pageable);

    /**
     * {@inheritDoc}
     * Busca cursos por fecha de creación en un rango.
     */
    @Override
    @Query("SELECT c FROM courses c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    Page<courses> findByCreatedDateBetween(LocalDateTime startDate,
                                          LocalDateTime endDate,
                                          Pageable pageable);

    /**
     * {@inheritDoc}
     * Cuenta todos los cursos.
     */
    @Override
    default long countActive() {
        return count();
    }

    /**
     * {@inheritDoc}
     * Los cursos siempre existen si tienen ID.
     */
    @Override
    default boolean existsActiveById(Integer id) {
        return existsById(id);
    }

    /**
     * {@inheritDoc}
     * Los cursos siempre están "activos".
     */
    @Override
    default Optional<courses> findActiveById(Integer id) {
        return findById(id);
    }

    /**
     * {@inheritDoc}
     * Busca cursos por términos de búsqueda en nombre.
     */
    @Override
    @Query("SELECT c FROM courses c WHERE LOWER(c.courseName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<courses> searchByTerm(String searchTerm, Pageable pageable);

    // ==================== MÉTODOS ESPECÍFICOS DEL DOMINIO ====================

    /**
     * Busca todos los cursos donde el docente especificado es director de grado.
     * Método optimizado para consultas de dirección académica.
     *
     * @param teacherId ID del docente director de grado
     * @return lista de cursos dirigidos por el docente
     */
    List<courses> findByGradeDirector_Id(Integer teacherId);

    /**
     * Busca cursos que contienen un término en su nombre.
     * Método para autocompletado y búsquedas.
     *
     * @param name término de búsqueda
     * @return lista de cursos que coinciden
     */
    @Query("SELECT c FROM courses c WHERE LOWER(c.courseName) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY c.courseName")
    List<courses> findByCourseNameContaining(@Param("name") String name);

    /**
     * Busca cursos con profesor asignado.
     * Método para identificar cursos con docente titular.
     *
     * @return lista de cursos que tienen profesor asignado
     */
    @Query("SELECT c FROM courses c WHERE c.teacherSubject IS NOT NULL ORDER BY c.courseName")
    List<courses> findCoursesWithAssignedTeacher();

    /**
     * Busca cursos sin profesor asignado.
     * Método para identificar cursos que necesitan asignación docente.
     *
     * @return lista de cursos sin profesor
     */
    @Query("SELECT c FROM courses c WHERE c.teacherSubject IS NULL ORDER BY c.courseName")
    List<courses> findCoursesWithoutTeacher();

    /**
     * Cuenta el número de estudiantes por curso.
     * Método para estadísticas de matrícula.
     *
     * @return lista de arrays [courseId, courseName, studentCount]
     */
    @Query("SELECT c.id, c.courseName, COUNT(u) FROM courses c LEFT JOIN c.users u GROUP BY c.id, c.courseName ORDER BY COUNT(u) DESC")
    List<Object[]> countStudentsByCourse();

    /**
     * Verifica si un curso tiene estudiantes asignados.
     * Método de validación para eliminación.
     *
     * @param courseId ID del curso
     * @return true si tiene estudiantes asignados
     */
    @Query("SELECT COUNT(u) > 0 FROM courses c JOIN c.users u WHERE c.id = :courseId")
    boolean hasAssignedStudents(@Param("courseId") Integer courseId);

    /**
     * Verifica si un curso tiene horarios asignados.
     * Método de validación para eliminación.
     *
     * @param courseId ID del curso
     * @return true si tiene horarios asignados
     */
    @Query("SELECT COUNT(sch) > 0 FROM schedule sch WHERE sch.courseId.id = :courseId")
    boolean hasAssignedSchedules(@Param("courseId") Integer courseId);

    /**
     * Busca cursos ordenados por carga académica (número de horarios).
     * Método para distribución equitativa de carga académica.
     *
     * @return lista de cursos ordenados por carga
     */
    @Query("SELECT c, COUNT(sch) as scheduleCount FROM courses c LEFT JOIN c.schedules sch GROUP BY c ORDER BY COUNT(sch) ASC")
    List<Object[]> findCoursesOrderedByWorkload();

    /**
     * Busca cursos por profesor asignado.
     * Método para consultas de carga docente por curso.
     *
     * @param teacherId ID del profesor
     * @return lista de cursos asignados al profesor
     */
    @Query("SELECT c FROM courses c WHERE c.teacherSubject.teacher.id = :teacherId ORDER BY c.courseName")
    List<courses> findCoursesByTeacher(@Param("teacherId") Integer teacherId);

    /**
     * Busca cursos por materia.
     * Método para consultas de oferta académica por materia.
     *
     * @param subjectId ID de la materia
     * @return lista de cursos que imparten la materia
     */
    @Query("SELECT c FROM courses c WHERE c.teacherSubject.subject.id = :subjectId ORDER BY c.courseName")
    List<courses> findCoursesBySubject(@Param("subjectId") Integer subjectId);
}