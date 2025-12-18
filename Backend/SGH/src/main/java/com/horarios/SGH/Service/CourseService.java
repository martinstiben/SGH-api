package com.horarios.SGH.Service;

import com.horarios.SGH.DTO.CourseDTO;
import com.horarios.SGH.DTO.CourseStudentDTO;
import com.horarios.SGH.Model.courses;
import com.horarios.SGH.Model.teachers;
import com.horarios.SGH.Repository.Icourses;
import com.horarios.SGH.Repository.Iteachers;
import com.horarios.SGH.Repository.IUserRepository;
import com.horarios.SGH.Repository.TeacherSubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de cursos con operaciones CRUD y consulta de estudiantes.
 * 
 * @author Sistema SGH
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final Icourses courseRepo;
    private final Iteachers teacherRepo;
    private final IUserRepository userRepo;
    private final TeacherSubjectRepository teacherSubjectRepo;

    /**
     * Crea un comparador natural para ordenar nombres de cursos numéricamente.
     * 
     * @return comparador para ordenamiento natural
     */
    private static Comparator<CourseDTO> naturalOrderComparator() {
        return Comparator.comparing(dto -> Pattern.compile("(\\d+)").splitAsStream(dto.getCourseName())
                .map(part -> part.matches("\\d+") ? String.format("%010d", Integer.parseInt(part)) : part)
                .collect(Collectors.joining()));
    }

    /**
     * Crea un nuevo curso.
     * 
     * @param dto datos del curso a crear
     * @return DTO del curso creado con ID asignado
     * @throws IllegalArgumentException si los datos son inválidos
     */
    public CourseDTO create(CourseDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos del curso no pueden ser null");
        }
        
        if (dto.getCourseName() == null || dto.getCourseName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del curso es obligatorio");
        }

        courses entity = new courses();
        entity.setCourseName(dto.getCourseName().trim());

        // Solo asignar director de grado si se especifica
        if (dto.getGradeDirectorId() != null) {
            teachers director = teacherRepo.findById(dto.getGradeDirectorId())
                .orElseThrow(() -> new IllegalArgumentException("Director de grado no encontrado"));
            entity.setGradeDirector(director);
        }

        courses saved = courseRepo.save(entity);
        dto.setCourseId(saved.getId());
        return dto;
    }

    /**
     * Obtiene todos los cursos ordenados naturalmente.
     * 
     * @return lista de DTOs de cursos
     */
    public List<CourseDTO> getAll() {
        return courseRepo.findAll().stream().map(c -> {
            CourseDTO dto = new CourseDTO();
            dto.setCourseId(c.getId());
            dto.setCourseName(c.getCourseName());
            dto.setGradeDirectorId(c.getGradeDirector() != null ? c.getGradeDirector().getId() : null);
            return dto;
        }).sorted(naturalOrderComparator()).collect(Collectors.toList());
    }

    /**
     * Obtiene un curso por su ID.
     * 
     * @param id ID del curso
     * @return DTO del curso encontrado o null si no existe
     */
    public CourseDTO getById(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID del curso debe ser un número positivo");
        }
        
        return courseRepo.findById(id).map(c -> {
            CourseDTO dto = new CourseDTO();
            dto.setCourseId(c.getId());
            dto.setCourseName(c.getCourseName());
            dto.setGradeDirectorId(c.getGradeDirector() != null ? c.getGradeDirector().getId() : null);
            return dto;
        }).orElse(null);
    }

    /**
     * Actualiza un curso existente.
     * 
     * @param id ID del curso a actualizar
     * @param dto nuevos datos del curso
     * @return DTO del curso actualizado o null si no existe
     * @throws IllegalArgumentException si los datos son inválidos
     */
    public CourseDTO update(int id, CourseDTO dto) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID del curso debe ser un número positivo");
        }
        
        if (dto == null) {
            throw new IllegalArgumentException("Los datos del curso no pueden ser null");
        }

        courses entity = courseRepo.findById(id).orElse(null);
        if (entity == null) {
            return null;
        }

        entity.setCourseName(dto.getCourseName().trim());

        if (dto.getGradeDirectorId() != null) {
            teachers director = teacherRepo.findById(dto.getGradeDirectorId())
                .orElseThrow(() -> new IllegalArgumentException("Director de grado no encontrado"));
            entity.setGradeDirector(director);
        } else {
            entity.setGradeDirector(null);
        }

        courses updated = courseRepo.save(entity);
        dto.setCourseId(updated.getId());
        return dto;
    }

    /**
     * Elimina un curso por su ID.
     * 
     * @param id ID del curso a eliminar
     * @throws IllegalArgumentException si el ID es inválido
     */
    public void delete(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID del curso debe ser un número positivo");
        }
        courseRepo.deleteById(id);
    }

    /**
     * Obtiene todos los estudiantes de un curso específico con información detallada.
     * 
     * @param courseId ID del curso
     * @return lista de DTOs con información de estudiantes
     * @throws IllegalArgumentException si courseId es inválido
     */
    public List<CourseStudentDTO> getStudentsByCourseId(int courseId) {
        // Validar que el courseId sea válido
        if (courseId <= 0) {
            throw new IllegalArgumentException("El ID del curso debe ser un número positivo");
        }

        // Verificar que el curso existe
        courses course = courseRepo.findById(courseId).orElse(null);
        if (course == null) {
            throw new IllegalArgumentException("El curso con ID " + courseId + " no existe");
        }

        try {
            return userRepo.findByCourseIdWithDetails((long) courseId).stream()
                    .map(user -> {
                        CourseStudentDTO dto = new CourseStudentDTO();
                        dto.setUserId(user.getUserId());
                        dto.setFullName(user.getPerson() != null ? user.getPerson().getFullName() : "N/A");
                        dto.setEmail(user.getPerson() != null ? user.getPerson().getEmail() : "N/A");
                        // CORRECCIÓN: Usar getFirstRole() en lugar de getRole()
                        dto.setRoleName(user.getFirstRole() != null ? user.getFirstRole().getRoleName() : "N/A");
                        dto.setAccountStatus(user.getAccountStatus());
                        dto.setVerified(user.isVerified());
                        return dto;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener estudiantes del curso: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el número total de cursos en el sistema.
     * 
     * @return número total de cursos
     */
    public long getTotalCoursesCount() {
        return courseRepo.count();
    }

    /**
     * Verifica si un curso existe por su ID.
     * 
     * @param courseId ID del curso
     * @return true si el curso existe
     * @throws IllegalArgumentException si courseId es inválido
     */
    public boolean existsById(int courseId) {
        if (courseId <= 0) {
            throw new IllegalArgumentException("El ID del curso debe ser un número positivo");
        }
        return courseRepo.existsById(courseId);
    }
}