package com.horarios.SGH.Service;

import com.horarios.SGH.DTO.ScheduleHistoryDTO;
import com.horarios.SGH.DTO.CourseWithoutAvailabilityDTO;
import com.horarios.SGH.DTO.ScheduleDTO;
import com.horarios.SGH.DTO.ScheduleGenerationDiagnosticDTO;
import com.horarios.SGH.Model.*;
import com.horarios.SGH.Model.schedule;
import com.horarios.SGH.Repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleGenerationService.class);
    
    // Duración de clase en minutos (60 minutos = 1 hora)
    private static final int CLASS_DURATION_MINUTES = 60;
    
    // Número mínimo de cursos que debe haber para activar la generación automática
    private static final int MIN_COURSES_FOR_AUTO_GENERATION = 0;

    private final IScheduleHistory historyRepository;
    private final Icourses courseRepo;
    private final Iteachers teacherRepo;
    private final ITeacherAvailabilityRepository availabilityRepo;
    private final IScheduleRepository scheduleRepo;
    private final Isubjects subjectRepo;
    private final TeacherSubjectRepository teacherSubjectRepo;
    private final ScheduleService scheduleService;

    @Transactional
    public ScheduleHistoryDTO generate(ScheduleHistoryDTO request, String executedBy) {
        logger.info("=== INICIANDO GENERACIÓN DE HORARIOS ===");
        logger.info("Usuario ejecutor: {}", executedBy);
        logger.info("Periodo: {} a {}", request.getPeriodStart(), request.getPeriodEnd());
        logger.info("Modo simulación (dryRun): {}", request.isDryRun());
        logger.info("Parámetros: {}", request.getParams());

        validate(request);

        schedule_history history = new schedule_history();
        history.setExecutedBy(executedBy);
        history.setExecutedAt(LocalDateTime.now());
        history.setStatus("RUNNING");
        history.setMessage("Iniciando generación");
        history.setTotalGenerated(0);

        history.setPeriodStart(request.getPeriodStart());
        history.setPeriodEnd(request.getPeriodEnd());
        history.setDryRun(request.isDryRun());
        history.setForce(request.isForce());
        history.setParams(request.getParams());

        history = historyRepository.save(history);

        try {
            long days = ChronoUnit.DAYS.between(request.getPeriodStart(), request.getPeriodEnd()) + 1;
            logger.info("Número de días en el período: {}", days);
            
            if (days < 0) throw new IllegalArgumentException("El rango de fechas es inválido");

            int totalGenerated = 0;
            List<CourseWithoutAvailabilityDTO> coursesWithoutAvailability = new ArrayList<>();

            if (!request.isDryRun()) {
                logger.info("=== MODO GENERACIÓN REAL ===");
                // Real schedule generation for courses
                GenerationResult result = generateSchedulesForPeriod(request.getPeriodStart(), request.getPeriodEnd(), request.getParams());
                totalGenerated = result.getTotalGenerated();
                coursesWithoutAvailability = result.getCoursesWithoutAvailability();
                logger.info("Generación completada - Total generado: {}, Cursos sin disponibilidad: {}", 
                    totalGenerated, coursesWithoutAvailability.size());
            } else {
                logger.info("=== MODO SIMULACIÓN ===");
                // Simulation: count courses without assigned schedule
                List<courses> coursesWithoutSchedule = getCoursesWithoutSchedule();
                totalGenerated = coursesWithoutSchedule.size();
                logger.info("Cursos encontrados sin horario asignado: {}", totalGenerated);

                // En modo simulación, también detectar cursos sin disponibilidad
                SimulationResult simulationResult = analyzeCoursesWithoutAvailability(request.getPeriodStart(), request.getPeriodEnd());
                coursesWithoutAvailability = simulationResult.getCoursesWithoutAvailability();
                logger.info("Cursos sin disponibilidad detectados: {}", coursesWithoutAvailability.size());
            }

            history.setStatus("SUCCESS");
            history.setTotalGenerated(totalGenerated);
            history.setMessage(buildSuccessMessage(totalGenerated, coursesWithoutAvailability.size()));
            history.setExecutedAt(LocalDateTime.now());
            historyRepository.save(history);

            // Retornar DTO con información de cursos sin disponibilidad
            ScheduleHistoryDTO response = toDTO(history);
            response.setCoursesWithoutAvailability(coursesWithoutAvailability);
            response.setTotalCoursesWithoutAvailability(coursesWithoutAvailability.size());

            logger.info("=== GENERACIÓN COMPLETADA EXITOSAMENTE ===");
            return response;
        } catch (Exception ex) {
            logger.error("Error durante la generación de horarios: {}", ex.getMessage(), ex);
            history.setStatus("FAILED");
            history.setMessage(ex.getMessage() != null ? ex.getMessage() : "Error en la generación");
            history.setExecutedAt(LocalDateTime.now());
            historyRepository.save(history);

            ScheduleHistoryDTO response = toDTO(history);
            response.setCoursesWithoutAvailability(new ArrayList<>());
            response.setTotalCoursesWithoutAvailability(0);

            logger.error("=== GENERACIÓN FALLIDA ===");
            return response;
        }
    }

    private void validate(ScheduleHistoryDTO r) {
        if (r.getPeriodStart() == null || r.getPeriodEnd() == null) {
            throw new IllegalArgumentException("periodStart y periodEnd son obligatorios");
        }
        if (r.getPeriodEnd().isBefore(r.getPeriodStart())) {
            throw new IllegalArgumentException("periodEnd no puede ser anterior a periodStart");
        }
        long days = ChronoUnit.DAYS.between(r.getPeriodStart(), r.getPeriodEnd()) + 1;
        if (days > 366) {
            throw new IllegalArgumentException("El rango máximo permitido es 366 días");
        }

        // Validación más flexible: solo verificar que haya cursos sin horario
        // No requerir profesores con disponibilidad ya que esto se detectará durante la generación
        List<courses> coursesWithoutSchedule = getCoursesWithoutSchedule();
        if (coursesWithoutSchedule.isEmpty()) {
            logger.info("No hay cursos sin horario asignado. La generación se ejecutará pero no creará nuevos horarios.");
        }

        // Log de advertencia si no hay profesores con disponibilidad, pero no fallar
        try {
            List<teachers> teachersWithAvailability = teacherRepo.findAll().stream()
                .filter(t -> !availabilityRepo.findByTeacher_Id(t.getId()).isEmpty())
                .collect(Collectors.toList());

            if (teachersWithAvailability.isEmpty()) {
                logger.warn("ADVERTENCIA: No hay profesores con disponibilidad definida. " +
                    "La generación puede no asignar cursos si no se configuran disponibilidades.");
            }
        } catch (Exception e) {
            // En caso de error al verificar (ej. en pruebas), continuar sin validar
            logger.debug("No se pudo verificar profesores con disponibilidad: {}", e.getMessage());
        }
    }

    /**
     * Resultado de la generación de horarios
     */
    private static class GenerationResult {
        private int totalGenerated;
        private List<CourseWithoutAvailabilityDTO> coursesWithoutAvailability;

        public GenerationResult(int totalGenerated, List<CourseWithoutAvailabilityDTO> coursesWithoutAvailability) {
            this.totalGenerated = totalGenerated;
            this.coursesWithoutAvailability = coursesWithoutAvailability;
        }

        public int getTotalGenerated() { return totalGenerated; }
        public List<CourseWithoutAvailabilityDTO> getCoursesWithoutAvailability() { return coursesWithoutAvailability; }
    }

    /**
     * Resultado de la simulación
     */
    private static class SimulationResult {
        private List<CourseWithoutAvailabilityDTO> coursesWithoutAvailability;

        public SimulationResult(List<CourseWithoutAvailabilityDTO> coursesWithoutAvailability) {
            this.coursesWithoutAvailability = coursesWithoutAvailability;
        }

        public List<CourseWithoutAvailabilityDTO> getCoursesWithoutAvailability() { return coursesWithoutAvailability; }
    }

    /**
     * Genera horarios automáticamente para cursos sin asignación y detecta cursos sin disponibilidad
     */
    private GenerationResult generateSchedulesForPeriod(LocalDate startDate, LocalDate endDate, String params) {
        logger.info("=== INICIANDO GENERACIÓN DE HORARIOS PARA PERÍODO ===");
        logger.info("Fecha inicio: {}, Fecha fin: {}", startDate, endDate);
        
        List<courses> coursesWithoutSchedule = getCoursesWithoutSchedule();
        logger.info("Cursos sin horario asignado encontrados: {}", coursesWithoutSchedule.size());
        
        List<schedule> generatedSchedules = new ArrayList<>();
        List<CourseWithoutAvailabilityDTO> coursesWithoutAvailability = new ArrayList<>();
        int totalGenerated = 0;

        List<String> daysInPeriod = getUniqueDaysInPeriod(startDate, endDate);
        logger.info("Días únicos en el período: {}", daysInPeriod);

        // Crear una lista para intentar la reasignación de cursos fallidos
        List<courses> coursesToRetry = new ArrayList<>();

        for (int i = 0; i < coursesWithoutSchedule.size(); i++) {
            courses course = coursesWithoutSchedule.get(i);
            logger.info("--- Procesando curso {}/{}: {} ---", i + 1, coursesWithoutSchedule.size(), course.getCourseName());

            if (course.getTeacherSubject() == null) {
                // Curso sin profesor/materia asignada
                logger.warn("Curso {} no tiene profesor/materia asignada", course.getCourseName());
                coursesWithoutAvailability.add(new CourseWithoutAvailabilityDTO(
                    course.getId(),
                    course.getCourseName(),
                    null,
                    "Sin profesor asignado",
                    "NO_TEACHER_ASSIGNED",
                    "El curso " + course.getCourseName() + " no tiene un profesor y materia asignados"
                ));
                continue;
            }

            teachers assignedTeacher = course.getTeacherSubject().getTeacher();
            subjects subject = course.getTeacherSubject().getSubject();
            
            logger.info("Profesor asignado: {} ({})", assignedTeacher.getTeacherName(), assignedTeacher.getId());
            logger.info("Materia: {} ({})", subject.getSubjectName(), subject.getId());

            // VALIDACIÓN CRÍTICA: Un profesor solo puede estar asociado a UNA materia
            List<TeacherSubject> teacherAssociations = teacherSubjectRepo.findByTeacher_Id(assignedTeacher.getId());
            logger.info("El profesor {} está asociado a {} materias", assignedTeacher.getTeacherName(), teacherAssociations.size());
            
            if (teacherAssociations.size() > 1) {
                String errorMsg = "ERROR DE CONFIGURACIÓN: El profesor " + assignedTeacher.getTeacherName() +
                    " está asociado a múltiples materias (" + teacherAssociations.size() + "). " +
                    "Cada profesor debe estar asociado únicamente a UNA materia.";
                logger.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            boolean courseAssigned = false;

            // Try to assign the course ONLY to the assigned teacher (no alternatives)
            for (String dayName : daysInPeriod) {
                if (courseAssigned) break;

                logger.info("Intentando asignar curso {} al profesor {} el día {}", 
                    course.getCourseName(), assignedTeacher.getTeacherName(), dayName);

                if (tryAssignCourseToTeacher(course, assignedTeacher, dayName, generatedSchedules)) {
                    courseAssigned = true;
                    totalGenerated++;
                    logger.info("✓ Curso {} asignado exitosamente el día {}", course.getCourseName(), dayName);
                    break;
                } else {
                    logger.debug("No se pudo asignar curso {} el día {}", course.getCourseName(), dayName);
                }
            }

            // Si el curso no pudo ser asignado, agregar a la lista para reintentos
            if (!courseAssigned) {
                logger.warn("✗ Curso {} no pudo ser asignado a ningún día", course.getCourseName());
                coursesToRetry.add(course);
            }
        }

        // PASO 2: Intentar reasignar cursos fallidos con una estrategia diferente
        // Buscar profesores alternativos que puedan enseñar la misma materia
        if (!coursesToRetry.isEmpty()) {
            logger.info("=== INICIANDO REASIGNACIÓN DE CURSOS FALLIDOS ===");
            logger.info("Intentando reasignar {} cursos fallidos", coursesToRetry.size());
            
            // Intentar reasignar cada curso con un enfoque diferente
            for (courses course : coursesToRetry) {
                if (course.getTeacherSubject() == null) {
                    continue; // Saltar cursos sin profesor
                }
                
                subjects subject = course.getTeacherSubject().getSubject();
                logger.info("Buscando profesor alternativo para {} - {}", course.getCourseName(), subject.getSubjectName());
                
                // Buscar otros profesores que puedan enseñar la misma materia
                List<TeacherSubject> alternativeTeachers = teacherSubjectRepo.findBySubject_Id(subject.getId());
                logger.info("Se encontraron {} profesores que pueden enseñar {}", 
                    alternativeTeachers.size(), subject.getSubjectName());
                
                boolean reassigned = false;
                
                for (TeacherSubject teacherSubject : alternativeTeachers) {
                    if (reassigned) break;
                    
                    teachers teacher = teacherSubject.getTeacher();
                    
                    // Evitar el profesor originalmente asignado
                    if (teacher.getId() == course.getTeacherSubject().getTeacher().getId()) {
                        continue;
                    }
                    
                    logger.info("Probando profesor alternativo: {} ({})", teacher.getTeacherName(), teacher.getId());
                    
                    // Intentar asignar al profesor alternativo en cualquier día
                    for (String dayName : daysInPeriod) {
                        if (tryAssignCourseToTeacher(course, teacher, dayName, generatedSchedules)) {
                            reassigned = true;
                            totalGenerated++;
                            logger.info("✓ Curso {} reasignado exitosamente al profesor {} el día {}", 
                                course.getCourseName(), teacher.getTeacherName(), dayName);
                            break;
                        }
                    }
                }
                
                // Si no se pudo reasignar, analizar el motivo
                if (!reassigned) {
                    teachers originalTeacher = course.getTeacherSubject().getTeacher();
                    CourseWithoutAvailabilityDTO unavailabilityInfo = analyzeCourseUnavailability(course, originalTeacher, daysInPeriod);
                    if (unavailabilityInfo != null) {
                        logger.info("Motivo de no asignación: {} - {}", unavailabilityInfo.getReason(), unavailabilityInfo.getDescription());
                        coursesWithoutAvailability.add(unavailabilityInfo);
                    }
                }
            }
        }

        logger.info("Resumiendo generación:");
        logger.info("- Total generado: {}", totalGenerated);
        logger.info("- Cursos sin disponibilidad: {}", coursesWithoutAvailability.size());
        logger.info("- Horarios generados: {}", generatedSchedules.size());

        if (!generatedSchedules.isEmpty()) {
            logger.info("Guardando {} horarios en la base de datos...", generatedSchedules.size());
            scheduleRepo.saveAll(generatedSchedules);
            logger.info("Horarios guardados exitosamente");
        } else {
            logger.info("No se generaron horarios para guardar");
        }

        return new GenerationResult(totalGenerated, coursesWithoutAvailability);
    }

    /**
     * Analiza la disponibilidad de cursos en modo simulación
     */
    private SimulationResult analyzeCoursesWithoutAvailability(LocalDate startDate, LocalDate endDate) {
        List<courses> coursesWithoutSchedule = getCoursesWithoutSchedule();
        List<CourseWithoutAvailabilityDTO> coursesWithoutAvailability = new ArrayList<>();
        List<String> daysInPeriod = getUniqueDaysInPeriod(startDate, endDate);

        for (courses course : coursesWithoutSchedule) {
            if (course.getTeacherSubject() == null) {
                coursesWithoutAvailability.add(new CourseWithoutAvailabilityDTO(
                    course.getId(),
                    course.getCourseName(),
                    null,
                    "Sin profesor asignado",
                    "NO_TEACHER_ASSIGNED",
                    "El curso " + course.getCourseName() + " no tiene un profesor y materia asignados"
                ));
                continue;
            }

            teachers assignedTeacher = course.getTeacherSubject().getTeacher();
            CourseWithoutAvailabilityDTO unavailabilityInfo = analyzeCourseUnavailability(course, assignedTeacher, daysInPeriod);
            if (unavailabilityInfo != null) {
                coursesWithoutAvailability.add(unavailabilityInfo);
            }
        }

        return new SimulationResult(coursesWithoutAvailability);
    }

    /**
     * Analiza por qué un curso no tiene disponibilidad
     */
    private CourseWithoutAvailabilityDTO analyzeCourseUnavailability(courses course, teachers teacher, List<String> daysInPeriod) {
        logger.info("Analizando indisponibilidad del curso {} para profesor {}", course.getCourseName(), teacher.getTeacherName());
        
        // Verificar si el profesor tiene disponibilidad definida para algún día
        boolean hasAnyAvailability = false;
        List<String> daysWithoutAvailability = new ArrayList<>();
        List<String> daysWithAvailability = new ArrayList<>();

        for (String dayName : daysInPeriod) {
            List<TeacherAvailability> availabilities = availabilityRepo.findByTeacher_IdAndDay(teacher.getId(), Days.valueOf(dayName));
            if (availabilities.isEmpty() || !availabilities.stream().anyMatch(TeacherAvailability::hasValidSchedule)) {
                daysWithoutAvailability.add(dayName);
                logger.debug("Día {} sin disponibilidad válida para profesor {}", dayName, teacher.getTeacherName());
            } else {
                hasAnyAvailability = true;
                daysWithAvailability.add(dayName);
                logger.debug("Día {} con disponibilidad válida para profesor {}", dayName, teacher.getTeacherName());
            }
        }

        if (!hasAnyAvailability) {
            String message = "El profesor " + teacher.getTeacherName() + " no tiene disponibilidad configurada para ningún día: " + 
                String.join(", ", daysWithoutAvailability);
            logger.info("MOTIVO DE INDISPONIBILIDAD: {}", message);
            return new CourseWithoutAvailabilityDTO(
                course.getId(),
                course.getCourseName(),
                teacher.getId(),
                teacher.getTeacherName(),
                "NO_AVAILABILITY_DEFINED",
                message
            );
        }

        // Verificar conflictos con horarios existentes
        for (String dayName : daysInPeriod) {
            List<schedule> existingSchedules = scheduleRepo.findByTeacherId(teacher.getId());
            List<schedule> daySchedules = existingSchedules.stream()
                .filter(s -> dayName.equals(s.getDay()))
                .collect(Collectors.toList());

            if (!daySchedules.isEmpty()) {
                String conflictDescription = daySchedules.stream()
                    .map(s -> String.format("%s - %s", s.getStartTime(), s.getEndTime()))
                    .collect(Collectors.joining(", "));
                String message = "El profesor " + teacher.getTeacherName() + " tiene conflictos de horario existentes el día " + 
                    dayName + " (" + conflictDescription + ")";
                logger.info("MOTIVO DE INDISPONIBILIDAD: {}", message);
                return new CourseWithoutAvailabilityDTO(
                    course.getId(),
                    course.getCourseName(),
                    teacher.getId(),
                    teacher.getTeacherName(),
                    "CONFLICTS_WITH_EXISTING",
                    message
                );
            }
        }

        // Si llegamos aquí, no se encontró disponibilidad por otros motivos
        String message = "No se encontraron espacios de tiempo disponibles para el profesor " + teacher.getTeacherName() + 
            " en los días: " + String.join(", ", daysInPeriod) + 
            ". Días con disponibilidad: " + String.join(", ", daysWithAvailability);
        logger.info("MOTIVO DE INDISPONIBILIDAD: {}", message);
        return new CourseWithoutAvailabilityDTO(
            course.getId(),
            course.getCourseName(),
            teacher.getId(),
            teacher.getTeacherName(),
            "NO_TIME_SLOTS_AVAILABLE",
            message
        );
    }

    private List<courses> getCoursesWithoutSchedule() {
        List<courses> allCourses = courseRepo.findAll();
        logger.debug("Total de cursos en el sistema: {}", allCourses.size());
        
        if (allCourses.isEmpty()) {
            logger.warn("No hay cursos registrados en el sistema");
            return Collections.emptyList();
        }
        
        // Obtener IDs de cursos que ya tienen horarios para optimizar la consulta
        List<Integer> coursesWithSchedules = scheduleRepo.findAll().stream()
            .map(s -> s.getCourseId().getId())
            .distinct()
            .collect(Collectors.toList());
        
        logger.debug("Cursos con horarios existentes: {}", coursesWithSchedules.size());
        
        // Filtrar cursos sin horarios
        List<courses> coursesWithoutSchedule = allCourses.stream()
            .filter(course -> !coursesWithSchedules.contains(course.getId()))
            .collect(Collectors.toList());
            
        logger.debug("Cursos sin horario asignado: {}", coursesWithoutSchedule.size());
        
        // Log de cursos sin horarios
        if (!coursesWithoutSchedule.isEmpty()) {
            logger.info("Cursos a procesar para generación de horarios:");
            coursesWithoutSchedule.forEach(course -> 
                logger.info("  - {} (ID: {})", course.getCourseName(), course.getId()));
        }
        
        return coursesWithoutSchedule;
    }

    private boolean tryAssignCourseToTeacher(courses course, teachers teacher, String dayName, List<schedule> generatedSchedules) {
        logger.debug("Intentando asignar curso {} al profesor {} el día {}", 
            course.getCourseName(), teacher.getTeacherName(), dayName);

        // Verificar disponibilidad del profesor para este día
        Days dayEnum = Days.valueOf(dayName);
        List<TeacherAvailability> availabilities = availabilityRepo.findByTeacher_IdAndDay(teacher.getId(), dayEnum);
        
        logger.debug("Disponibilidades encontradas para el profesor {} el {}: {}", 
            teacher.getTeacherName(), dayName, availabilities.size());

        // Ordenar las disponibilidades para priorizar horarios de mañana antes que tarde
        availabilities.sort(Comparator.comparing(TeacherAvailability::getAmStart, 
            Comparator.nullsLast(Comparator.naturalOrder())));

        for (int i = 0; i < availabilities.size(); i++) {
            TeacherAvailability availability = availabilities.get(i);
            logger.debug("Analizando disponibilidad {}/{} del profesor {} el {}", 
                i + 1, availabilities.size(), teacher.getTeacherName(), dayName);

            if (availability.hasValidSchedule()) {
                logger.debug("Disponibilidad válida encontrada:");
                if (availability.getAmStart() != null && availability.getAmEnd() != null) {
                    logger.debug("  - Mañana: {} - {}", availability.getAmStart(), availability.getAmEnd());
                }
                if (availability.getPmStart() != null && availability.getPmEnd() != null) {
                    logger.debug("  - Tarde: {} - {}", availability.getPmStart(), availability.getPmEnd());
                }

                // Generar slots disponibles para este profesor en este día
                List<schedule> availableSlots = generateAvailableTimeSlotsForCourse(course, teacher, availability, dayName);
                logger.debug("Slots generados: {}", availableSlots.size());

                if (!availableSlots.isEmpty()) {
                    for (int j = 0; j < availableSlots.size(); j++) {
                        schedule slot = availableSlots.get(j);
                        logger.debug("Slot {}/{}: {} - {}", j + 1, availableSlots.size(), slot.getStartTime(), slot.getEndTime());
                    }
                }

                // Filtrar slots que no tengan conflictos
                List<schedule> conflictFreeSlots = filterSlotsWithoutConflicts(availableSlots, teacher.getId(), dayName);
                logger.debug("Slots sin conflictos: {}", conflictFreeSlots.size());

                if (!conflictFreeSlots.isEmpty()) {
                    // Asignar el primer slot disponible (priorizar mañana)
                    schedule assignedSlot = conflictFreeSlots.get(0);
                    logger.info("✓ Slot asignado: {} - {}", assignedSlot.getStartTime(), assignedSlot.getEndTime());
                    generatedSchedules.add(assignedSlot);
                    return true;
                } else {
                    logger.debug("Todos los slots tienen conflictos o no son válidos");
                }
            } else {
                logger.debug("Disponibilidad inválida (no tiene horarios válidos)");
            }
        }
        
        if (availabilities.isEmpty()) {
            logger.debug("No se encontraron disponibilidades para el profesor {} el día {}", teacher.getTeacherName(), dayName);
        }
        
        return false;
    }

    private String getDayNameFromDate(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "Lunes";
            case TUESDAY -> "Martes";
            case WEDNESDAY -> "Miércoles";
            case THURSDAY -> "Jueves";
            case FRIDAY -> "Viernes";
            // Opcional: incluir fines de semana
            // case SATURDAY -> "Sábado";
            // case SUNDAY -> "Domingo";
            default -> null; // No generar para fines de semana por defecto
        };
    }

    private List<String> getUniqueDaysInPeriod(LocalDate startDate, LocalDate endDate) {
        List<String> days = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            String dayName = getDayNameFromDate(currentDate);
            if (dayName != null && !days.contains(dayName)) {
                days.add(dayName);
            }
            currentDate = currentDate.plusDays(1);
        }

        // Ordenar los días según el orden de la semana
        List<String> orderedDays = new ArrayList<>();
        String[] dayOrder = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};
        
        for (String day : dayOrder) {
            if (days.contains(day)) {
                orderedDays.add(day);
            }
        }
        
        logger.debug("Días únicos en el período (ordenados): {}", orderedDays);
        return orderedDays;
    }

    private List<schedule> generateAvailableTimeSlotsForCourse(courses course, teachers teacher, TeacherAvailability availability, String dayName) {
        List<schedule> slots = new ArrayList<>();
        subjects subject = course.getTeacherSubject().getSubject();
        String scheduleName = course.getCourseName() + " - " + subject.getSubjectName();

        logger.debug("Generando slots para curso {}, profesor {}, día {}", 
            course.getCourseName(), teacher.getTeacherName(), dayName);

        // Generar slots para mañana
        if (availability.getAmStart() != null && availability.getAmEnd() != null) {
            logger.debug("Generando slots de mañana: {} - {}", availability.getAmStart(), availability.getAmEnd());
            List<schedule> morningSlots = generateSlotsInPeriodForCourse(course, teacher, subject, availability.getAmStart(), availability.getAmEnd(),
                                                      dayName, scheduleName);
            slots.addAll(morningSlots);
            logger.debug("Slots de mañana generados: {}", morningSlots.size());
        } else {
            logger.debug("No hay disponibilidad de mañana");
        }

        // Generar slots para tarde
        if (availability.getPmStart() != null && availability.getPmEnd() != null) {
            logger.debug("Generando slots de tarde: {} - {}", availability.getPmStart(), availability.getPmEnd());
            List<schedule> afternoonSlots = generateSlotsInPeriodForCourse(course, teacher, subject, availability.getPmStart(), availability.getPmEnd(),
                                                      dayName, scheduleName);
            slots.addAll(afternoonSlots);
            logger.debug("Slots de tarde generados: {}", afternoonSlots.size());
        } else {
            logger.debug("No hay disponibilidad de tarde");
        }

        logger.debug("Total de slots generados: {}", slots.size());
        return slots;
    }

    private List<schedule> generateSlotsInPeriodForCourse(courses course, teachers teacher, subjects subject, LocalTime start, LocalTime end,
                                                        String day, String scheduleName) {
        List<schedule> slots = new ArrayList<>();
        LocalTime currentTime = start;
        
        // Calcular duración de clase en horas y minutos
        final int durationHours = CLASS_DURATION_MINUTES / 60;
        final int durationMinutes = CLASS_DURATION_MINUTES % 60;
        final LocalTime CLASS_DURATION = LocalTime.of(durationHours, durationMinutes);

        logger.debug("Generando slots de tiempo para período: {} - {} (duración: {} minutos)", 
            start, end, CLASS_DURATION_MINUTES);

        // Generar slots de tiempo de duración fija según configuración
        while (currentTime.plusHours(durationHours).plusMinutes(durationMinutes).isBefore(end) || 
               currentTime.plusHours(durationHours).plusMinutes(durationMinutes).equals(end)) {
            
            LocalTime slotEnd = currentTime.plusHours(durationHours).plusMinutes(durationMinutes);
            
            if (slotEnd.isAfter(end)) {
                logger.debug("Slot {} - {} excede el límite de {} - {}, no se genera", currentTime, slotEnd, start, end);
                break;
            }

            schedule slot = new schedule();
            slot.setCourseId(course);
            slot.setTeacherId(teacher);
            slot.setSubjectId(subject);
            slot.setDay(day);
            slot.setStartTime(currentTime);
            slot.setEndTime(slotEnd);
            slot.setScheduleName(scheduleName);

            slots.add(slot);
            logger.debug("Slot generado: {} - {}", currentTime, slotEnd);
            
            currentTime = slotEnd;
        }

        // Ordenar los slots por hora de inicio para facilitar la visualización
        slots.sort(Comparator.comparing(schedule::getStartTime));

        logger.debug("Total de slots generados en el período: {}", slots.size());
        return slots;
    }

    private List<schedule> filterSlotsWithoutConflicts(List<schedule> slots, Integer teacherId, String dayName) {
        return slots.stream()
            .filter(slot -> !hasConflict(slot, teacherId, dayName))
            .collect(Collectors.toList());
    }

    private boolean hasConflict(schedule slot, Integer teacherId, String dayName) {
        // Buscar todos los horarios existentes para este profesor
        List<schedule> existingSchedules = scheduleRepo.findByTeacherId(teacherId);
        
        logger.debug("Verificando conflictos para slot {} - {} del profesor {} el día {}", 
            slot.getStartTime(), slot.getEndTime(), teacherId, dayName);
        logger.debug("Horarios existentes del profesor {}: {}", teacherId, existingSchedules.size());

        // Filtrar por día y verificar conflictos de horario
        for (schedule existing : existingSchedules) {
            if (dayName.equals(existing.getDay())) {
                logger.debug("Comparando con horario existente: {} - {}", existing.getStartTime(), existing.getEndTime());
                
                boolean hasConflict = (slot.getStartTime().isBefore(existing.getEndTime()) &&
                                     slot.getEndTime().isAfter(existing.getStartTime()));
                
                if (hasConflict) {
                    logger.debug("✓ CONFLICTO DETECTADO: El slot {} - {} se solapa con {} - {}", 
                        slot.getStartTime(), slot.getEndTime(), existing.getStartTime(), existing.getEndTime());
                    return true;
                } else {
                    logger.debug("No hay conflicto con este horario existente");
                }
            } else {
                logger.debug("Horario de día diferente ({} vs {}), ignorando", dayName, existing.getDay());
            }
        }
        
        logger.debug("✓ No se detectaron conflictos para el slot {} - {}", slot.getStartTime(), slot.getEndTime());
        return false;
    }

    private String buildSuccessMessage(int totalGenerated, int totalWithoutAvailability) {
        if (totalWithoutAvailability == 0) {
            return "Generación completada exitosamente. " + totalGenerated + " horarios generados.";
        } else {
            return String.format("Generación completada. %d horarios generados, %d cursos sin disponibilidad de profesores.", 
                totalGenerated, totalWithoutAvailability);
        }
    }

    /**
     * Genera horarios automáticamente con parámetros por defecto (semana actual)
     */
    @Transactional
    public ScheduleHistoryDTO autoGenerate(String executedBy) {
        logger.info("=== INICIANDO GENERACIÓN AUTOMÁTICA DE HORARIOS ===");
        logger.info("Usuario ejecutor: {}", executedBy);
        
        ScheduleHistoryDTO request = new ScheduleHistoryDTO();
        LocalDate today = LocalDate.now();

        // Calcular lunes y viernes de la semana actual
        LocalDate monday = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate friday = today.with(java.time.DayOfWeek.FRIDAY);
        
        logger.info("Generando horarios para la semana actual: {} a {}", monday, friday);

        request.setPeriodStart(monday);
        request.setPeriodEnd(friday);
        request.setDryRun(false);
        request.setForce(false);
        request.setParams("Generación automática desde interfaz");

        ScheduleHistoryDTO result = generate(request, executedBy);
        
        logger.info("Generación automática completada: {} horarios generados, {} cursos sin disponibilidad", 
            result.getTotalGenerated(), result.getTotalCoursesWithoutAvailability());
        
        return result;
    }

    /**
     * Regenera todo el horario: borra todos los horarios existentes y genera nuevos automáticamente
     */
    @Transactional
    public ScheduleHistoryDTO regenerate(String executedBy) {
        logger.info("=== INICIANDO REGENERACIÓN COMPLETA DE HORARIOS ===");
        logger.info("Usuario ejecutor: {}", executedBy);
        
        // Borrar todos los horarios existentes
        logger.info("Eliminando todos los horarios existentes...");
        scheduleService.deleteAllSchedules();
        logger.info("Todos los horarios existentes han sido eliminados");

        // Generar nuevos horarios automáticamente
        logger.info("Generando nuevos horarios automáticamente...");
        ScheduleHistoryDTO result = autoGenerate(executedBy);
        
        logger.info("Regeneración completa finalizada");
        return result;
    }

    private ScheduleHistoryDTO toDTO(schedule_history h) {
        ScheduleHistoryDTO dto = new ScheduleHistoryDTO();
        dto.setId(h.getId());
        dto.setExecutedBy(h.getExecutedBy());
        dto.setExecutedAt(h.getExecutedAt());
        dto.setStatus(h.getStatus());
        dto.setTotalGenerated(h.getTotalGenerated());
        dto.setMessage(h.getMessage());
        dto.setPeriodStart(h.getPeriodStart());
        dto.setPeriodEnd(h.getPeriodEnd());
        dto.setDryRun(h.isDryRun());
        dto.setForce(h.isForce());
        dto.setParams(h.getParams());
        return dto;
    }

    /**
     * Método para limpiar todos los horarios (solo para testing)
     */
    public void clearAllSchedulesForTesting() {
        logger.warn("=== LIMPIANDO TODOS LOS HORARIOS PARA TESTING ===");
        scheduleService.deleteAllSchedules();
        logger.warn("Todos los horarios han sido eliminados");
    }

    /**
     * Método para resetear completamente la base de datos (solo para testing)
     */
    public void resetDatabaseForTesting() {
        logger.warn("=== RESETEANDO BASE DE DATOS COMPLETA PARA TESTING ===");

        // Limpiar en orden para evitar restricciones de clave foránea
        scheduleRepo.deleteAll();
        courseRepo.deleteAll();
        teacherSubjectRepo.deleteAll();
        availabilityRepo.deleteAll();
        teacherRepo.deleteAll();
        subjectRepo.deleteAll();

        logger.warn("Base de datos limpiada completamente. Reinicia la aplicación para recargar datos iniciales.");
    }

    /**
     * Método de debug para verificar el estado de cursos y horarios
     */
    public String debugCoursesStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== DEBUG: ESTADO DE CURSOS ===\n");

        List<courses> allCourses = courseRepo.findAll();
        sb.append("Total cursos en BD: ").append(allCourses.size()).append("\n");

        List<schedule> allSchedules = scheduleRepo.findAll();
        sb.append("Total horarios en BD: ").append(allSchedules.size()).append("\n\n");

        sb.append("CURSOS DETALLADOS:\n");
        for (courses course : allCourses) {
            List<schedule> courseSchedules = scheduleRepo.findByCourseId(course.getId());
            String teacherInfo = course.getTeacherSubject() != null ?
                course.getTeacherSubject().getTeacher().getTeacherName() : "SIN PROFESOR";

            sb.append(String.format("- %s (ID:%d) | Profesor: %s | Horarios: %d\n",
                course.getCourseName(), course.getId(), teacherInfo, courseSchedules.size()));
        }

        sb.append("\nCURSOS SIN HORARIO ASIGNADO:\n");
        List<courses> coursesWithoutSchedule = getCoursesWithoutSchedule();
        if (coursesWithoutSchedule.isEmpty()) {
            sb.append("NINGUNO - Todos los cursos tienen horario asignado\n");
        } else {
            for (courses course : coursesWithoutSchedule) {
                String teacherInfo = course.getTeacherSubject() != null ?
                    course.getTeacherSubject().getTeacher().getTeacherName() : "SIN PROFESOR";
                sb.append(String.format("- %s (ID:%d) | Profesor: %s\n",
                    course.getCourseName(), course.getId(), teacherInfo));
            }
        }

        return sb.toString();
    }

    /**
     * Genera un diagnóstico completo del sistema de generación de horarios
     * Útil para debugging y identificar problemas antes de generar horarios
     */
    public ScheduleGenerationDiagnosticDTO generateDiagnostic() {
        logger.info("=== GENERANDO DIAGNÓSTICO DEL SISTEMA DE HORARIOS ===");
        
        try {
            // Obtener datos básicos
            List<courses> allCourses = courseRepo.findAll();
            List<teachers> allTeachers = teacherRepo.findAll();
            List<TeacherAvailability> allAvailabilities = availabilityRepo.findAll();
            List<schedule> allSchedules = scheduleRepo.findAll();
            List<TeacherSubject> allTeacherSubjects = teacherSubjectRepo.findAll();
            
            // Analizar cursos
            List<ScheduleGenerationDiagnosticDTO.CourseDiagnosticDTO> courseDiagnostics = allCourses.stream()
                .map(course -> {
                    Integer teacherId = null;
                    String teacherName = "Sin asignar";
                    Integer subjectId = null;
                    String subjectName = "Sin asignar";
                    boolean hasSchedule = false;
                    String status;
                    
                    if (course.getTeacherSubject() != null) {
                        teacherId = course.getTeacherSubject().getTeacher().getId();
                        teacherName = course.getTeacherSubject().getTeacher().getTeacherName();
                        subjectId = course.getTeacherSubject().getSubject().getId();
                        subjectName = course.getTeacherSubject().getSubject().getSubjectName();
                    }
                    
                    List<schedule> courseSchedules = scheduleRepo.findByCourseId(course.getId());
                    hasSchedule = !courseSchedules.isEmpty();
                    
                    if (!hasSchedule) {
                        if (teacherId == null) {
                            status = "SIN_PROFESOR";
                        } else {
                            status = "SIN_HORARIO";
                        }
                    } else {
                        status = "CON_HORARIO";
                    }
                    
                    return new ScheduleGenerationDiagnosticDTO.CourseDiagnosticDTO(
                        course.getId(), course.getCourseName(), teacherId, teacherName,
                        subjectId, subjectName, hasSchedule, status
                    );
                })
                .collect(Collectors.toList());
            
            // Analizar profesores
            List<ScheduleGenerationDiagnosticDTO.TeacherDiagnosticDTO> teacherDiagnostics = allTeachers.stream()
                .map(teacher -> {
                    long subjectCount = allTeacherSubjects.stream()
                        .filter(ts -> ts.getTeacher().getId() == teacher.getId())
                        .count();
                    
                    long availabilityCount = allAvailabilities.stream()
                        .filter(av -> av.getTeacher().getId() == teacher.getId())
                        .count();
                    
                    boolean canTeachMultipleSubjects = subjectCount > 1;
                    
                    return new ScheduleGenerationDiagnosticDTO.TeacherDiagnosticDTO(
                        teacher.getId(), teacher.getTeacherName(), (int)subjectCount, 
                        (int)availabilityCount, canTeachMultipleSubjects
                    );
                })
                .collect(Collectors.toList());
            
            // Obtener horarios existentes convertidos a DTO
            List<ScheduleDTO> scheduleDTOs = allSchedules.stream()
                .map(s -> {
                    ScheduleDTO dto = new ScheduleDTO();
                    dto.setId(s.getId());
                    dto.setCourseId(s.getCourseId().getId());
                    dto.setTeacherId(s.getTeacherId().getId());
                    dto.setSubjectId(s.getSubjectId().getId());
                    dto.setDay(s.getDay());
                    dto.setStartTimeFromLocalTime(s.getStartTime());
                    dto.setEndTimeFromLocalTime(s.getEndTime());
                    dto.setScheduleName(s.getScheduleName());
                    dto.setTeacherName(s.getTeacherId().getTeacherName());
                    dto.setSubjectName(s.getSubjectId().getSubjectName());
                    return dto;
                })
                .collect(Collectors.toList());
            
            // Identificar cursos potencialmente problemáticos
            List<CourseWithoutAvailabilityDTO> problematicCourses = new ArrayList<>();
            for (ScheduleGenerationDiagnosticDTO.CourseDiagnosticDTO course : courseDiagnostics) {
                if ("SIN_HORARIO".equals(course.getStatus()) && course.getTeacherId() != null) {
                    List<String> daysInPeriod = getUniqueDaysInPeriod(LocalDate.now(), LocalDate.now().plusDays(4));
                    teachers teacher = teacherRepo.findById(course.getTeacherId()).orElse(null);
                    if (teacher != null) {
                        CourseWithoutAvailabilityDTO problemInfo = analyzeCourseUnavailability(
                            courseRepo.findById(course.getId()).orElseThrow(), 
                            teacher, daysInPeriod
                        );
                        if (problemInfo != null) {
                            problematicCourses.add(problemInfo);
                        }
                    }
                }
            }
            
            // Calcular estadísticas
            long coursesWithSchedule = courseDiagnostics.stream().filter(ScheduleGenerationDiagnosticDTO.CourseDiagnosticDTO::isHasScheduleAssigned).count();
            long teachersWithAvailability = teacherDiagnostics.stream()
                .filter(teacher -> teacher.getAvailabilityCount() > 0).count();
            
            ScheduleGenerationDiagnosticDTO.SystemStatistics statistics = 
                new ScheduleGenerationDiagnosticDTO.SystemStatistics(
                    allCourses.size(),
                    coursesWithSchedule,
                    courseDiagnostics.stream().filter(c -> !c.isHasScheduleAssigned()).count(),
                    allTeachers.size(),
                    teachersWithAvailability,
                    teacherDiagnostics.stream().filter(teacher -> teacher.getAvailabilityCount() == 0).count(),
                    allSchedules.size(),
                    allTeacherSubjects.size()
                );
            
            ScheduleGenerationDiagnosticDTO diagnostic = new ScheduleGenerationDiagnosticDTO(
                courseDiagnostics,
                teacherDiagnostics,
                allAvailabilities,
                scheduleDTOs,
                problematicCourses,
                statistics
            );
            
            logger.info("Diagnóstico completado:");
            logger.info("- Total cursos: {}", statistics.getTotalCourses());
            logger.info("- Cursos sin horario: {}", statistics.getCoursesWithoutSchedule());
            logger.info("- Total profesores: {}", statistics.getTotalTeachers());
            logger.info("- Profesores sin disponibilidad: {}", statistics.getTeachersWithoutAvailability());
            logger.info("- Cursos problemáticos: {}", problematicCourses.size());
            
            return diagnostic;
            
        } catch (Exception e) {
            logger.error("Error generando diagnóstico del sistema: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar diagnóstico del sistema: " + e.getMessage());
        }
    }
}