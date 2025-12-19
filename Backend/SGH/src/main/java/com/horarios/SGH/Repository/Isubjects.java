package com.horarios.SGH.Repository;

import com.horarios.SGH.Model.subjects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio especializado para materias académicas siguiendo principios SOLID.
 * Extiende AbstractRepository para operaciones comunes y añade consultas específicas del dominio académico.
 *
 * Implementa el patrón Repository con consultas optimizadas para gestión de materias.
 * Aplica el patrón Factory a través de RepositoryFactory para consultas dinámicas.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única - gestión de materias académicas
 * - OCP: Extensible mediante Specifications para filtros académicos
 * - LSP: Compatible con JpaRepository y AbstractRepository
 * - ISP: Interface específica para materias
 * - DIP: Depende de abstracciones, no implementaciones concretas
 *
 * Patrón Abstract aplicado: Extiende AbstractRepository para operaciones comunes
 * Patrón Factory aplicado: Usa RepositoryFactory para consultas dinámicas de materias
 *
 * @author Sistema SGH
 * @version 2.0 - Refactorizado con patrones SOLID
 */
@Repository
public interface Isubjects extends AbstractRepository<subjects, Integer> {

    // ==================== IMPLEMENTACIÓN DE MÉTODOS ABSTRACTOS ====================

    /**
     * {@inheritDoc}
     * Para materias, considera "activas" todas (no hay eliminación lógica).
     */
    @Override
    @Query("SELECT s FROM subjects s ORDER BY s.subjectName ASC")
    Page<subjects> findActive(Pageable pageable);

    /**
     * {@inheritDoc}
     * Busca materias por fecha de creación en un rango.
     */
    @Override
    @Query("SELECT s FROM subjects s WHERE s.createdAt BETWEEN :startDate AND :endDate")
    Page<subjects> findByCreatedDateBetween(LocalDateTime startDate,
                                           LocalDateTime endDate,
                                           Pageable pageable);

    /**
     * {@inheritDoc}
     * Cuenta todas las materias.
     */
    @Override
    default long countActive() {
        return count();
    }

    /**
     * {@inheritDoc}
     * Las materias siempre existen si tienen ID.
     */
    @Override
    default boolean existsActiveById(Integer id) {
        return existsById(id);
    }

    /**
     * {@inheritDoc}
     * Las materias siempre están "activas".
     */
    @Override
    default Optional<subjects> findActiveById(Integer id) {
        return findById(id);
    }

    /**
     * {@inheritDoc}
     * Busca materias por términos de búsqueda en nombre.
     */
    @Override
    @Query("SELECT s FROM subjects s WHERE LOWER(s.subjectName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<subjects> searchByTerm(String searchTerm, Pageable pageable);

    // ==================== MÉTODOS ESPECÍFICOS DEL DOMINIO ====================

    /**
     * Busca una materia por su ID.
     * Método de compatibilidad con versiones anteriores.
     *
     * @param id ID de la materia
     * @return Optional con la materia encontrada
     */
    Optional<subjects> findById(int id);

    /**
     * Busca una materia por su nombre exacto.
     * Método optimizado para validaciones de unicidad.
     *
     * @param subjectName nombre de la materia
     * @return materia encontrada o null
     */
    subjects findBySubjectName(String subjectName);

    /**
     * Busca materias que contienen un término en su nombre.
     * Método para autocompletado y búsquedas.
     *
     * @param name término de búsqueda
     * @return lista de materias que coinciden
     */
    @Query("SELECT s FROM subjects s WHERE LOWER(s.subjectName) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY s.subjectName")
    List<subjects> findBySubjectNameContaining(@Param("name") String name);

    /**
     * Cuenta el número de profesores asociados a cada materia.
     * Método para estadísticas académicas.
     *
     * @return lista de arrays [subjectId, subjectName, teacherCount]
     */
    @Query("SELECT s.id, s.subjectName, COUNT(ts) FROM subjects s LEFT JOIN s.teacherSubjects ts GROUP BY s.id, s.subjectName ORDER BY COUNT(ts) DESC")
    List<Object[]> countTeachersBySubject();

    /**
     * Verifica si una materia tiene profesores asignados.
     * Método de validación para eliminación.
     *
     * @param subjectId ID de la materia
     * @return true si tiene profesores asignados
     */
    @Query("SELECT COUNT(ts) > 0 FROM subjects s JOIN s.teacherSubjects ts WHERE s.id = :subjectId")
    boolean hasAssignedTeachers(@Param("subjectId") Integer subjectId);

    /**
     * Verifica si una materia está siendo utilizada en horarios.
     * Método de validación para eliminación.
     *
     * @param subjectId ID de la materia
     * @return true si está en uso en horarios
     */
    @Query("SELECT COUNT(sch) > 0 FROM schedule sch WHERE sch.subjectId.id = :subjectId")
    boolean isUsedInSchedules(@Param("subjectId") Integer subjectId);
}