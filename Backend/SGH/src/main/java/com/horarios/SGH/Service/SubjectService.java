package com.horarios.SGH.Service;

import com.horarios.SGH.DTO.SubjectDTO;
import com.horarios.SGH.Model.subjects;
import com.horarios.SGH.Repository.Isubjects;
import com.horarios.SGH.Repository.IScheduleRepository;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de materias académicas.
 * Proporciona operaciones CRUD con validación de integridad referencial.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de gestionar materias
 * - DIP: Depende de abstracciones (repositorios)
 *
 * Funcionalidades:
 * - Creación, lectura, actualización y eliminación de materias
 * - Validación de integridad (no eliminar materias en uso)
 * - Conversión entre entidades y DTOs
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Service
public class SubjectService {

    private final Isubjects repo;
    private final IScheduleRepository scheduleRepo;
    
    /**
     * Logger para registro de eventos del servicio de asignaturas.
     */
    private static final Logger logger = Logger.getLogger(SubjectService.class.getName());
    
    /**
     * Constructor manual para inyección de dependencias.
     * Mantiene compatibilidad con Spring y permite testing.
     *
     * @param repo Repositorio de asignaturas
     * @param scheduleRepo Repositorio de horarios
     */
    public SubjectService(Isubjects repo, IScheduleRepository scheduleRepo) {
        this.repo = repo;
        this.scheduleRepo = scheduleRepo;
    }

    /**
     * Crea una nueva materia en el sistema.
     *
     * @param dto DTO con los datos de la materia a crear
     * @return DTO de la materia creada con ID asignado
     */
    public SubjectDTO create(SubjectDTO dto) {
        subjects entity = new subjects();
        entity.setSubjectName(dto.getSubjectName());
        subjects saved = repo.save(entity);
        dto.setSubjectId(saved.getId());
        return dto;
    }

    /**
     * Obtiene todas las materias del sistema.
     *
     * @return Lista de DTOs con todas las materias
     */
    public List<SubjectDTO> getAll() {
        return repo.findAll().stream().map(s -> {
            SubjectDTO dto = new SubjectDTO();
            dto.setSubjectId(s.getId());
            dto.setSubjectName(s.getSubjectName());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * Obtiene una materia específica por su ID.
     *
     * @param id ID de la materia a buscar
     * @return DTO de la materia encontrada o null si no existe
     */
    public SubjectDTO getById(int id) {
        return repo.findById(id).map(s -> {
            SubjectDTO dto = new SubjectDTO();
            dto.setSubjectId(s.getId());
            dto.setSubjectName(s.getSubjectName());
            return dto;
        }).orElse(null);
    }

    /**
     * Actualiza una materia existente.
     *
     * @param id ID de la materia a actualizar
     * @param dto DTO con los nuevos datos de la materia
     * @return DTO de la materia actualizada o null si no existe
     */
    public SubjectDTO update(int id, SubjectDTO dto) {
        subjects entity = repo.findById(id).orElse(null);
        if (entity == null) return null;
        entity.setSubjectName(dto.getSubjectName());
        subjects updated = repo.save(entity);
        dto.setSubjectId(updated.getId());
        return dto;
    }

    /**
     * Elimina una materia del sistema.
     * Verifica que la materia no esté siendo utilizada en horarios antes de eliminarla.
     *
     * @param id ID de la materia a eliminar
     * @throws RuntimeException si la materia está siendo utilizada en horarios
     */
    public void delete(int id) {
        // Verificar si la materia está siendo utilizada en horarios
        if (scheduleRepo.existsBySubjectId_Id(id)) {
            throw new RuntimeException("No se puede eliminar la materia porque está siendo utilizada en horarios");
        }
        repo.deleteById(id);
    }
}