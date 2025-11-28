package com.horarios.SGH.Service;

import com.horarios.SGH.DTO.ScheduleHistoryDTO;
import com.horarios.SGH.Model.*;
import com.horarios.SGH.Repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para asignar profesores automáticamente a cursos sin profesor asignado
 */
@Service
@RequiredArgsConstructor
public class AutoAssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(AutoAssignmentService.class);
    
    private final Icourses courseRepo;
    private final Iteachers teacherRepo;
    private final Isubjects subjectRepo;
    private final TeacherSubjectRepository teacherSubjectRepo;
    private final ScheduleGenerationService scheduleGenerationService;

    /**
     * Asigna profesores automáticamente a cursos sin profesor
     */
    @Transactional
    public String autoAssignTeachers() {
        logger.info("=== INICIANDO ASIGNACIÓN AUTOMÁTICA DE PROFESORES ===");
        
        StringBuilder result = new StringBuilder();
        result.append("=== ASIGNACIÓN AUTOMÁTICA DE PROFESORES ===\n\n");
        
        // Obtener cursos sin profesor
        List<courses> coursesWithoutTeacher = courseRepo.findAll().stream()
            .filter(course -> course.getTeacherSubject() == null)
            .collect(Collectors.toList());
        
        result.append("Cursos sin profesor encontrado: ").append(coursesWithoutTeacher.size()).append("\n\n");
        
        // Obtener profesores disponibles (que no estén sobrecargados)
        List<teachers> availableTeachers = teacherRepo.findAll().stream()
            .filter(this::isTeacherAvailableForNewCourse)
            .collect(Collectors.toList());
        
        result.append("Profesores disponibles para asignar: ").append(availableTeachers.size()).append("\n\n");
        
        if (coursesWithoutTeacher.isEmpty()) {
            result.append("✅ Todos los cursos ya tienen profesor asignado.\n");
            return result.toString();
        }
        
        if (availableTeachers.isEmpty()) {
            result.append("❌ No hay profesores disponibles para asignar.\n");
            return result.toString();
        }
        
        // Obtener todas las materias disponibles
        List<subjects> availableSubjects = subjectRepo.findAll();
        
        int assignments = 0;
        int teacherIndex = 0;
        
        // Asignar profesores a cursos
        for (courses course : coursesWithoutTeacher) {
            if (teacherIndex >= availableTeachers.size()) {
                teacherIndex = 0; // Reiniciar ciclo si se agotan los profesores
            }
            
            teachers teacher = availableTeachers.get(teacherIndex);
            
            // Buscar si el profesor ya tiene una materia asignada
            List<TeacherSubject> existingAssignments = teacherSubjectRepo.findByTeacher_Id(teacher.getId());
            subjects subjectToAssign;
            
            if (!existingAssignments.isEmpty()) {
                // El profesor ya tiene una materia, asignar la misma
                subjectToAssign = existingAssignments.get(0).getSubject();
                result.append(String.format("Asignando %s a %s (materia: %s)\n", 
                    course.getCourseName(), teacher.getTeacherName(), subjectToAssign.getSubjectName()));
            } else {
                // Buscar una materia disponible para este profesor
                subjectToAssign = findSubjectForTeacher(teacher, availableSubjects);
                if (subjectToAssign != null) {
                    // Crear nueva asociación profesor-materia
                    TeacherSubject newAssignment = new TeacherSubject();
                    newAssignment.setTeacher(teacher);
                    newAssignment.setSubject(subjectToAssign);
                    teacherSubjectRepo.save(newAssignment);
                    
                    result.append(String.format("Creando nueva asociación: %s -> %s\n", 
                        teacher.getTeacherName(), subjectToAssign.getSubjectName()));
                } else {
                    result.append(String.format("❌ No se encontró materia para %s\n", teacher.getTeacherName()));
                    continue;
                }
            }
            
            if (subjectToAssign != null) {
                // Asignar profesor y materia al curso
                TeacherSubject finalAssignment = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher.getId(), subjectToAssign.getId())
                    .orElseGet(() -> {
                        // Si no existe, crear nueva asociación
                        TeacherSubject newAssignment = new TeacherSubject();
                        newAssignment.setTeacher(teacher);
                        newAssignment.setSubject(subjectToAssign);
                        return teacherSubjectRepo.save(newAssignment);
                    });
                course.setTeacherSubject(finalAssignment);
                courseRepo.save(course);
                assignments++;
                teacherIndex++;
                
                result.append(String.format("✅ Asignado: %s -> %s (%s)\n\n", 
                    course.getCourseName(), teacher.getTeacherName(), subjectToAssign.getSubjectName()));
            }
        }
        
        result.append("=== RESUMEN ===\n");
        result.append(String.format("Asignaciones realizadas: %d\n", assignments));
        result.append(String.format("Cursos sin profesor después de la asignación: %d\n", 
            courseRepo.findAll().stream().filter(c -> c.getTeacherSubject() == null).count()));
        
        logger.info("Asignación automática completada: {} asignaciones realizadas", assignments);
        
        return result.toString();
    }
    
    /**
     * Verifica si un profesor está disponible para asignar un nuevo curso
     * Un profesor puede tener máximo 3 cursos asignados
     */
    private boolean isTeacherAvailableForNewCourse(teachers teacher) {
        try {
            List<courses> teacherCourses = courseRepo.findAll().stream()
                .filter(course -> course.getTeacherSubject() != null &&
                    course.getTeacherSubject().getTeacher().getId() == teacher.getId())
                .collect(Collectors.toList());
            
            // Verificar que el profesor no esté sobrecargado (máximo 3 cursos)
            boolean isAvailable = teacherCourses.size() < 3;
            
            logger.debug("Profesor {}: {} cursos asignados, disponible: {}", 
                teacher.getTeacherName(), teacherCourses.size(), isAvailable);
            
            return isAvailable;
        } catch (Exception e) {
            logger.warn("Error verificando disponibilidad del profesor {}: {}", 
                teacher.getTeacherName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Busca una materia apropiada para un profesor
     */
    private subjects findSubjectForTeacher(teachers teacher, List<subjects> availableSubjects) {
        // Primero verificar si el profesor ya tiene alguna materia asignada
        List<TeacherSubject> existingAssignments = teacherSubjectRepo.findByTeacher_Id(teacher.getId());
        if (!existingAssignments.isEmpty()) {
            return existingAssignments.get(0).getSubject();
        }
        
        // Buscar una materia que no esté ya asignada a otros profesores
        for (subjects subject : availableSubjects) {
            // Verificar si la materia ya está asignada a alguien
            List<TeacherSubject> subjectAssignments = teacherSubjectRepo.findBySubject_Id(subject.getId());
            if (subjectAssignments.isEmpty()) {
                return subject;
            }
        }
        
        // Si todas las materias están asignadas, devolver la primera disponible
        // (en este caso se permitirán múltiples profesores por materia)
        return availableSubjects.isEmpty() ? null : availableSubjects.get(0);
    }
    
    /**
     * Ejecuta asignación automática y luego genera horarios
     */
    @Transactional
    public String autoAssignAndGenerate() {
        StringBuilder result = new StringBuilder();
        
        // Paso 1: Asignar profesores
        result.append(autoAssignTeachers());
        result.append("\n");
        
        // Paso 2: Generar horarios
        try {
            logger.info("Iniciando generación de horarios después de asignación automática");
            ScheduleHistoryDTO generationResult = scheduleGenerationService.autoGenerate("AUTO_ASSIGNMENT");
            
            result.append("=== GENERACIÓN DE HORARIOS ===\n");
            result.append(String.format("Status: %s\n", generationResult.getStatus()));
            result.append(String.format("Horarios generados: %d\n", generationResult.getTotalGenerated()));
            result.append(String.format("Cursos sin disponibilidad: %d\n", generationResult.getTotalCoursesWithoutAvailability()));
            result.append(String.format("Mensaje: %s\n", generationResult.getMessage()));
            
        } catch (Exception e) {
            logger.error("Error en generación de horarios: {}", e.getMessage());
            result.append(String.format("❌ Error en generación de horarios: %s\n", e.getMessage()));
        }
        
        return result.toString();
    }
}