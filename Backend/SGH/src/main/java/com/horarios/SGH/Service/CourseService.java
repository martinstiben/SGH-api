package com.horarios.SGH.Service;

import com.horarios.SGH.DTO.CourseDTO;
import com.horarios.SGH.DTO.CourseStudentDTO;
import com.horarios.SGH.Model.courses;
import com.horarios.SGH.Model.teachers;
import com.horarios.SGH.Repository.Icourses;
import com.horarios.SGH.Repository.Iteachers;
import com.horarios.SGH.Repository.IUserRepository;
import com.horarios.SGH.Repository.TeacherSubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.logging.Logger;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de cursos académicos del sistema SGH.
 * Implementa el patrón Strategy para diferentes estrategias de ordenamiento,
 * Factory Method para creación de DTOs, y Template Method para operaciones CRUD.
 *
 * Proporciona operaciones completas de gestión de cursos con validaciones
 * de negocio, manejo robusto de excepciones y logging detallado.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Service
@Transactional(readOnly = true)
public class CourseService {

    private final Icourses courseRepo;
    private final Iteachers teacherRepo;
    private final IUserRepository userRepo;
    private final TeacherSubjectRepository teacherSubjectRepo;
    
    /**
     * Logger para registro de eventos del servicio de cursos.
     */
    private static final Logger logger = Logger.getLogger(CourseService.class.getName());
    
    /**
     * Logger estático para compatibilidad con código existente.
     */
    private static final Logger log = logger;
    
    /**
     * Constructor manual para inyección de dependencias.
     * Mantiene compatibilidad con Spring y permite testing.
     *
     * @param courseRepo Repositorio de cursos
     * @param teacherRepo Repositorio de docentes
     * @param userRepo Repositorio de usuarios
     * @param teacherSubjectRepo Repositorio de relación docente-materia
     */
    public CourseService(Icourses courseRepo, Iteachers teacherRepo, 
                        IUserRepository userRepo, TeacherSubjectRepository teacherSubjectRepo) {
        this.courseRepo = courseRepo;
        this.teacherRepo = teacherRepo;
        this.userRepo = userRepo;
        this.teacherSubjectRepo = teacherSubjectRepo;
    }

    /**
     * Estrategia de ordenamiento para cursos.
     * Implementa patrón Strategy.
     */
    @FunctionalInterface
    public interface CourseSortingStrategy {
        Comparator<CourseDTO> getComparator();
    }

    /**
     * Estrategia de ordenamiento natural (por número de curso).
     */
    public static final CourseSortingStrategy NATURAL_ORDER = () ->
        Comparator.comparing(dto -> Pattern.compile("(\\d+)")
            .splitAsStream(dto.getCourseName())
            .map(part -> part.matches("\\d+") ? String.format("%010d", Integer.parseInt(part)) : part)
            .collect(Collectors.joining()));

    /**
     * Estrategia de ordenamiento alfabético.
     */
    public static final CourseSortingStrategy ALPHABETICAL_ORDER = () ->
        Comparator.comparing(CourseDTO::getCourseName);

    /**
     * Factory para crear CourseDTOs.
     * Implementa patrón Factory Method.
     */
    public static class CourseDTOFactory {
        public static CourseDTO createFromEntity(courses course) {
            if (course == null) {
                return null;
            }

            CourseDTO dto = new CourseDTO();
            dto.setCourseId(course.getId());
            dto.setCourseName(course.getCourseName());
            dto.setGradeDirectorId(course.getGradeDirector() != null ? course.getGradeDirector().getId() : null);
            return dto;
        }

        public static CourseDTO createBasic(int courseId, String courseName) {
            CourseDTO dto = new CourseDTO();
            dto.setCourseId(courseId);
            dto.setCourseName(courseName);
            return dto;
        }

        public static CourseDTO createWithDirector(int courseId, String courseName, Integer gradeDirectorId) {
            CourseDTO dto = new CourseDTO();
            dto.setCourseId(courseId);
            dto.setCourseName(courseName);
            dto.setGradeDirectorId(gradeDirectorId);
            return dto;
        }
    }

    /**
     * Método Template para operaciones CRUD de cursos.
     * Implementa patrón Template Method.
     */
    protected abstract class CourseOperationTemplate<T> {
        protected abstract void validateInput(T input);
        protected abstract courses executeOperation(T input);
        protected abstract CourseDTO createResult(courses entity);

        public final CourseDTO execute(T input) {
            validateInput(input);
            try {
                courses result = executeOperation(input);
                CourseDTO dto = createResult(result);
                logger.info("Operación de curso ejecutada exitosamente");
                return dto;
            } catch (Exception e) {
                logger.severe("Error en operación de curso: " + e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Crea un nuevo curso aplicando validaciones y patrón Template Method.
     *
     * @param dto datos del curso a crear
     * @return DTO del curso creado con ID asignado
     * @throws IllegalArgumentException si los datos son inválidos
     */
    @Transactional
    public CourseDTO create(CourseDTO dto) {
        return new CourseOperationTemplate<CourseDTO>() {
            @Override
            protected void validateInput(CourseDTO input) {
                if (input == null) {
                    throw new IllegalArgumentException("Los datos del curso no pueden ser null");
                }

                if (input.getCourseName() == null || input.getCourseName().trim().isEmpty()) {
                    throw new IllegalArgumentException("El nombre del curso es obligatorio");
                }

                // Validar formato del nombre del curso
                if (!input.getCourseName().matches("^[a-zA-ZÀ-ÿ0-9\\s]+$")) {
                    throw new IllegalArgumentException("El nombre del curso solo puede contener letras, números y espacios");
                }
            }

            @Override
            protected courses executeOperation(CourseDTO input) {
                courses entity = new courses();
                entity.setCourseName(input.getCourseName().trim());

                // Solo asignar director de grado si se especifica
                if (input.getGradeDirectorId() != null) {
                    teachers director = teacherRepo.findById(input.getGradeDirectorId())
                        .orElseThrow(() -> new IllegalArgumentException("Director de grado no encontrado"));
                    entity.setGradeDirector(director);
                }

                return courseRepo.save(entity);
            }

            @Override
            protected CourseDTO createResult(courses entity) {
                return CourseDTOFactory.createFromEntity(entity);
            }
        }.execute(dto);
    }

    /**
     * Obtiene todos los cursos ordenados naturalmente.
     * Implementa patrón Strategy para ordenamiento configurable.
     *
     * @return lista de DTOs de cursos ordenados
     */
    public List<CourseDTO> getAll() {
        return getAll(NATURAL_ORDER);
    }

    /**
     * Obtiene todos los cursos con estrategia de ordenamiento específica.
     *
     * @param sortingStrategy estrategia de ordenamiento a aplicar
     * @return lista de DTOs de cursos ordenados
     */
    public List<CourseDTO> getAll(CourseSortingStrategy sortingStrategy) {
        try {
            List<CourseDTO> courses = courseRepo.findAll().stream()
                .map(CourseDTOFactory::createFromEntity)
                .collect(Collectors.toList());

            if (sortingStrategy != null) {
                courses.sort(sortingStrategy.getComparator());
            }

            logger.info("Obtenidos " + courses.size() + " cursos ordenados");
            return courses;
        } catch (Exception e) {
            logger.severe("Error obteniendo todos los cursos: " + e.getMessage());
            throw new RuntimeException("Error al obtener cursos", e);
        }
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