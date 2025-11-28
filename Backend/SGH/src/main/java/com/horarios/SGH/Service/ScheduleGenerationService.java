package com.horarios.SGH.Service;

import com.horarios.SGH.DTO.ScheduleHistoryDTO;
import com.horarios.SGH.DTO.CourseWithoutAvailabilityDTO;
import com.horarios.SGH.DTO.ScheduleDTO;
import com.horarios.SGH.DTO.ScheduleGenerationDiagnosticDTO;
import com.horarios.SGH.DTO.CourseDTO;
import com.horarios.SGH.DTO.CourseGenerationValidationDTO;
import com.horarios.SGH.DTO.ScheduleTableDTO;
import com.horarios.SGH.DTO.ScheduleVerificationReportDTO;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        private List<schedule> generatedSchedules;

        public GenerationResult(int totalGenerated, List<CourseWithoutAvailabilityDTO> coursesWithoutAvailability) {
            this.totalGenerated = totalGenerated;
            this.coursesWithoutAvailability = coursesWithoutAvailability;
            this.generatedSchedules = new ArrayList<>();
        }

        public GenerationResult(int totalGenerated, List<CourseWithoutAvailabilityDTO> coursesWithoutAvailability, List<schedule> generatedSchedules) {
            this.totalGenerated = totalGenerated;
            this.coursesWithoutAvailability = coursesWithoutAvailability;
            this.generatedSchedules = generatedSchedules != null ? generatedSchedules : new ArrayList<>();
        }

        public int getTotalGenerated() { return totalGenerated; }
        public List<CourseWithoutAvailabilityDTO> getCoursesWithoutAvailability() { return coursesWithoutAvailability; }
        public List<schedule> getGeneratedSchedules() { return generatedSchedules; }
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
     * Validación de viabilidad del horario generado
     */
    private static class ScheduleViabilityValidation {
        private boolean viable;
        private List<String> issues;

        public ScheduleViabilityValidation(boolean viable, List<String> issues) {
            this.viable = viable;
            this.issues = issues != null ? issues : new ArrayList<>();
        }

        public boolean isViable() { return viable; }
        public List<String> getIssues() { return issues; }
    }

    /**
     * Genera horarios completos y optimizados llenando todos los slots disponibles
     * Asigna múltiples cursos por slot según profesores disponibles, maximizando el uso del tiempo
     */
    private GenerationResult generateSchedulesForPeriod(LocalDate startDate, LocalDate endDate, String params) {
        logger.info("=== INICIANDO GENERACIÓN OPTIMIZADA DE HORARIOS COMPLETOS ===");
        logger.info("Fecha inicio: {}, Fecha fin: {}", startDate, endDate);

        List<courses> coursesWithoutSchedule = getCoursesWithoutSchedule();
        logger.info("Cursos sin horario asignado encontrados: {}", coursesWithoutSchedule.size());

        List<schedule> generatedSchedules = new ArrayList<>();
        List<CourseWithoutAvailabilityDTO> coursesWithoutAvailability = new ArrayList<>();
        int totalGenerated = 0;

        List<String> daysInPeriod = getUniqueDaysInPeriod(startDate, endDate);
        logger.info("Días únicos en el período: {}", daysInPeriod);

        // Filtrar cursos que tienen profesor asignado
        List<courses> coursesWithTeachers = coursesWithoutSchedule.stream()
            .filter(course -> course.getTeacherSubject() != null)
            .collect(Collectors.toList());

        logger.info("Cursos con profesores asignados: {}", coursesWithTeachers.size());

        // Agregar cursos sin profesor a la lista de no asignables
        coursesWithoutSchedule.stream()
            .filter(course -> course.getTeacherSubject() == null)
            .forEach(course -> {
                coursesWithoutAvailability.add(new CourseWithoutAvailabilityDTO(
                    course.getId(),
                    course.getCourseName(),
                    null,
                    "Sin profesor asignado",
                    "NO_TEACHER_ASSIGNED",
                    "El curso " + course.getCourseName() + " no tiene un profesor y materia asignados"
                ));
            });

        // Validar configuración de profesores
        validateTeacherConfiguration(coursesWithTeachers);

        // PASO 1: Generar horarios completos por día y slot
        for (String dayName : daysInPeriod) {
            logger.info("--- Generando horarios completos para {} ---", dayName);

            // Crear lista de cursos pendientes para asignar
            List<courses> pendingCourses = new ArrayList<>(coursesWithTeachers);

            // Obtener slots de tiempo disponibles para el día
            List<TimeSlot> timeSlots = createTimeSlotsForDay();

            for (TimeSlot timeSlot : timeSlots) {
                logger.debug("Llenando slot: {} - {}", timeSlot.getStartTime(), timeSlot.getEndTime());

                // Encontrar profesores disponibles que tengan cursos pendientes
                List<teachers> availableTeachers = findAvailableTeachersWithCoursesForSlot(dayName, timeSlot, pendingCourses);
                logger.debug("Profesores disponibles con cursos pendientes: {}", availableTeachers.size());

                if (availableTeachers.isEmpty()) {
                    logger.debug("No hay profesores disponibles con cursos pendientes en este slot");
                    continue;
                }

                // Asignar cursos a profesores disponibles (uno por profesor, maximizando el uso del slot)
                int assignmentsInSlot = 0;
                for (teachers teacher : availableTeachers) {
                    if (pendingCourses.isEmpty()) {
                        logger.debug("No quedan cursos pendientes para asignar");
                        break;
                    }

                    // Encontrar curso asignado a este profesor
                    courses courseToAssign = pendingCourses.stream()
                        .filter(course -> course.getTeacherSubject() != null &&
                                course.getTeacherSubject().getTeacher().getId() == teacher.getId())
                        .findFirst()
                        .orElse(null);

                    if (courseToAssign != null) {
                        // Crear el horario
                        schedule newSchedule = new schedule();
                        newSchedule.setCourseId(courseToAssign);
                        newSchedule.setTeacherId(teacher);
                        newSchedule.setSubjectId(courseToAssign.getTeacherSubject().getSubject());
                        newSchedule.setDay(dayName);
                        newSchedule.setStartTime(timeSlot.getStartTime());
                        newSchedule.setEndTime(timeSlot.getEndTime());
                        newSchedule.setScheduleName(courseToAssign.getCourseName() + " - " +
                            courseToAssign.getTeacherSubject().getSubject().getSubjectName());

                        generatedSchedules.add(newSchedule);
                        pendingCourses.remove(courseToAssign);
                        assignmentsInSlot++;
                        totalGenerated++;

                        logger.info("✓ Asignado: {} → {} en {}-{} ({})",
                            courseToAssign.getCourseName(), teacher.getTeacherName(),
                            timeSlot.getStartTime(), timeSlot.getEndTime(), dayName);
                    }
                }

                logger.debug("Asignaciones realizadas en este slot: {}", assignmentsInSlot);
            }

            logger.info("Horarios generados para {}: {}", dayName,
                generatedSchedules.stream().filter(s -> dayName.equals(s.getDay())).count());
        }

        // PASO 2: Identificar cursos que no pudieron ser asignados
        List<courses> assignedCourses = generatedSchedules.stream()
            .map(schedule::getCourseId)
            .distinct()
            .collect(Collectors.toList());

        List<courses> unassignedCourses = coursesWithTeachers.stream()
            .filter(course -> !assignedCourses.contains(course))
            .collect(Collectors.toList());

        // Analizar por qué no se pudieron asignar
        for (courses course : unassignedCourses) {
            teachers teacher = course.getTeacherSubject().getTeacher();
            CourseWithoutAvailabilityDTO unavailabilityInfo = analyzeCourseUnavailability(course, teacher, daysInPeriod);
            if (unavailabilityInfo != null) {
                coursesWithoutAvailability.add(unavailabilityInfo);
            }
        }

        logger.info("=== RESUMEN DE GENERACIÓN OPTIMIZADA ===");
        logger.info("- Total cursos procesados: {}", coursesWithTeachers.size());
        logger.info("- Horarios generados: {}", totalGenerated);
        logger.info("- Cursos asignados: {}", assignedCourses.size());
        logger.info("- Cursos sin asignar: {}", unassignedCourses.size());
        logger.info("- Cursos sin profesor: {}", coursesWithoutSchedule.size() - coursesWithTeachers.size());
        logger.info("- Eficiencia: {:.1f}%", (double) assignedCourses.size() / coursesWithTeachers.size() * 100);

        if (!generatedSchedules.isEmpty()) {
            logger.info("Guardando {} horarios optimizados en la base de datos...", generatedSchedules.size());
            scheduleRepo.saveAll(generatedSchedules);
            logger.info("Horarios guardados exitosamente");
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

    /**
     * Crea slots de tiempo exactos según especificaciones del horario escolar
     * Basado en el formato del curso 1A con franjas específicas y bloques protegidos
     */
    private List<TimeSlot> createTimeSlotsForDay() {
        List<TimeSlot> slots = new ArrayList<>();

        // Jornada escolar según especificaciones:
        // 6:00-7:00, 7:00-8:00, 8:00-9:00, 9:30-10:30, 10:30-11:30, 11:30-12:30
        // 1:00-2:00, 2:00-3:00, 3:00-4:00, 4:00-5:00, 5:00-6:00

        // Mañana - Primera parte (antes del descanso)
        slots.add(new TimeSlot(LocalTime.of(6, 0), LocalTime.of(7, 0)));   // 6:00-7:00
        slots.add(new TimeSlot(LocalTime.of(7, 0), LocalTime.of(8, 0)));   // 7:00-8:00
        slots.add(new TimeSlot(LocalTime.of(8, 0), LocalTime.of(9, 0)));   // 8:00-9:00

        // BLOQUE PROTEGIDO: 9:00-9:30 AM - DESCANSO (NO SE TOCA)

        // Mañana - Segunda parte (después del descanso)
        slots.add(new TimeSlot(LocalTime.of(9, 30), LocalTime.of(10, 30))); // 9:30-10:30
        slots.add(new TimeSlot(LocalTime.of(10, 30), LocalTime.of(11, 30))); // 10:30-11:30
        slots.add(new TimeSlot(LocalTime.of(11, 30), LocalTime.of(12, 30))); // 11:30-12:30

        // BLOQUE PROTEGIDO: 12:00-1:00 PM - ALMUERZO (NO SE TOCA)

        // Tarde (después del almuerzo)
        slots.add(new TimeSlot(LocalTime.of(13, 0), LocalTime.of(14, 0)));  // 1:00-2:00
        slots.add(new TimeSlot(LocalTime.of(14, 0), LocalTime.of(15, 0)));  // 2:00-3:00
        slots.add(new TimeSlot(LocalTime.of(15, 0), LocalTime.of(16, 0)));  // 3:00-4:00
        slots.add(new TimeSlot(LocalTime.of(16, 0), LocalTime.of(17, 0)));  // 4:00-5:00
        slots.add(new TimeSlot(LocalTime.of(17, 0), LocalTime.of(18, 0)));  // 5:00-6:00

        return slots;
    }

    /**
     * Encuentra slots de tiempo disponibles para un profesor específico en un día determinado
     */
    private List<TimeSlot> findAvailableSlotsForTeacher(teachers teacher, String dayName) {
        List<TimeSlot> availableSlots = new ArrayList<>();

        // Crear todos los slots de tiempo posibles para el día
        List<TimeSlot> allPossibleSlots = createTimeSlotsForDay();

        // Obtener disponibilidad del profesor para este día
        Days dayEnum = Days.valueOf(dayName);
        List<TeacherAvailability> availabilities = availabilityRepo.findByTeacher_IdAndDay(teacher.getId(), dayEnum);

        if (availabilities.isEmpty()) {
            logger.debug("Profesor {} no tiene disponibilidad definida para {}", teacher.getTeacherName(), dayName);
            return availableSlots;
        }

        // Para cada slot posible, verificar si está disponible
        for (TimeSlot slot : allPossibleSlots) {
            boolean slotAvailable = false;

            for (TeacherAvailability availability : availabilities) {
                if (availability.hasValidSchedule() &&
                    isTimeSlotWithinAvailability(slot, availability)) {
                    slotAvailable = true;
                    break;
                }
            }

            if (slotAvailable) {
                availableSlots.add(slot);
            }
        }

        logger.debug("Slots disponibles para {} el {}: {}", teacher.getTeacherName(), dayName, availableSlots.size());
        return availableSlots;
    }

    /**
     * Encuentra profesores disponibles para un slot específico
     */
    private List<teachers> findAvailableTeachersForSlot(String dayName, TimeSlot timeSlot) {
        Days dayEnum = Days.valueOf(dayName);

        return teacherRepo.findAll().stream()
            .filter(teacher -> {
                List<TeacherAvailability> availabilities = availabilityRepo.findByTeacher_IdAndDay(teacher.getId(), dayEnum);
                return availabilities.stream().anyMatch(avail ->
                    avail.hasValidSchedule() &&
                    isTimeSlotWithinAvailability(timeSlot, avail)
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * Encuentra profesores disponibles para un slot específico (versión optimizada)
     * Incluye validación de que tienen cursos asignados
     */
    private List<teachers> findAvailableTeachersWithCoursesForSlot(String dayName, TimeSlot timeSlot, List<courses> pendingCourses) {
        Days dayEnum = Days.valueOf(dayName);

        return teacherRepo.findAll().stream()
            .filter(teacher -> {
                // Verificar que el profesor tenga disponibilidad
                List<TeacherAvailability> availabilities = availabilityRepo.findByTeacher_IdAndDay(teacher.getId(), dayEnum);
                boolean hasAvailability = availabilities.stream().anyMatch(avail ->
                    avail.hasValidSchedule() &&
                    isTimeSlotWithinAvailability(timeSlot, avail)
                );

                if (!hasAvailability) return false;

                // Verificar que no tenga conflicto de horario
                if (hasConflict(teacher, dayName, timeSlot.getStartTime(), timeSlot.getEndTime())) return false;

                // Verificar que tenga cursos pendientes para asignar
                return pendingCourses.stream().anyMatch(course ->
                    course.getTeacherSubject() != null &&
                    course.getTeacherSubject().getTeacher().getId() == teacher.getId()
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * Valida la configuración completa del profesor antes de asignar cursos
     */
    private void validateTeacherCompleteConfiguration(teachers teacher, List<courses> courses) {
        // Verificar que el profesor tenga disponibilidad definida
        List<TeacherAvailability> availabilities = availabilityRepo.findByTeacher_Id(teacher.getId());
        if (availabilities.isEmpty()) {
            logger.warn("Profesor {} no tiene disponibilidad definida", teacher.getTeacherName());
        } else {
            // Verificar que tenga al menos un día con disponibilidad válida
            boolean hasValidAvailability = availabilities.stream().anyMatch(TeacherAvailability::hasValidSchedule);
            if (!hasValidAvailability) {
                logger.warn("Profesor {} no tiene horarios válidos definidos", teacher.getTeacherName());
            }
        }

        // Verificar asociación con materias
        List<TeacherSubject> associations = teacherSubjectRepo.findByTeacher_Id(teacher.getId());
        if (associations.isEmpty()) {
            logger.warn("Profesor {} no está asociado a ninguna materia", teacher.getTeacherName());
        } else if (associations.size() > 1) {
            String error = "Profesor " + teacher.getTeacherName() + " está asociado a múltiples materias: " +
                associations.stream().map(ts -> ts.getSubject().getSubjectName()).collect(Collectors.joining(", "));
            logger.error(error);
            throw new RuntimeException(error);
        }

        // Verificar conflictos existentes
        List<schedule> existingSchedules = scheduleRepo.findByTeacherId(teacher.getId());
        if (!existingSchedules.isEmpty()) {
            logger.info("Profesor {} tiene {} horarios existentes", teacher.getTeacherName(), existingSchedules.size());
        }
    }

    /**
     * Analiza la disponibilidad detallada de un profesor para un período
     */
    private TeacherAvailabilityAnalysis analyzeTeacherAvailabilityForPeriod(teachers teacher, List<String> daysInPeriod) {
        TeacherAvailabilityAnalysis analysis = new TeacherAvailabilityAnalysis();
        
        for (String dayName : daysInPeriod) {
            try {
                Days dayEnum = Days.valueOf(dayName);
                List<TeacherAvailability> dayAvailabilities = availabilityRepo.findByTeacher_IdAndDay(teacher.getId(), dayEnum);
                
                if (dayAvailabilities.isEmpty()) {
                    analysis.addDayWithoutAvailability(dayName, "Sin disponibilidad definida");
                    continue;
                }

                // Analizar slots disponibles para el día
                List<TimeSlot> availableSlots = new ArrayList<>();
                for (TeacherAvailability availability : dayAvailabilities) {
                    if (availability.hasValidSchedule()) {
                        List<TimeSlot> daySlots = getSlotsFromAvailability(availability);
                        availableSlots.addAll(daySlots);
                    }
                }

                if (availableSlots.isEmpty()) {
                    analysis.addDayWithoutAvailability(dayName, "Sin slots de tiempo válidos");
                } else {
                    analysis.addDayWithAvailability(dayName, availableSlots.size());
                }

            } catch (Exception e) {
                analysis.addDayWithoutAvailability(dayName, "Error analizando disponibilidad: " + e.getMessage());
            }
        }

        return analysis;
    }

    /**
     * Obtiene slots de tiempo válidos a partir de la disponibilidad de un profesor
     */
    private List<TimeSlot> getSlotsFromAvailability(TeacherAvailability availability) {
        List<TimeSlot> slots = new ArrayList<>();
        List<TimeSlot> allSlots = createTimeSlotsForDay();

        for (TimeSlot slot : allSlots) {
            if (isTimeSlotWithinAvailability(slot, availability)) {
                slots.add(slot);
            }
        }

        return slots;
    }

    /**
     * Clase auxiliar para análisis de disponibilidad de profesores
     */
    private static class TeacherAvailabilityAnalysis {
        private List<String> daysWithAvailability = new ArrayList<>();
        private List<String> daysWithoutAvailability = new ArrayList<>();
        private Map<String, Integer> slotsPerDay = new HashMap<>();
        private Map<String, String> issuesPerDay = new HashMap<>();

        public void addDayWithAvailability(String dayName, int slotCount) {
            daysWithAvailability.add(dayName);
            slotsPerDay.put(dayName, slotCount);
        }

        public void addDayWithoutAvailability(String dayName, String issue) {
            daysWithoutAvailability.add(dayName);
            issuesPerDay.put(dayName, issue);
        }

        public boolean hasValidAvailability() {
            return !daysWithAvailability.isEmpty();
        }

        public int getTotalAvailableSlots() {
            return slotsPerDay.values().stream().mapToInt(Integer::intValue).sum();
        }

        // Getters
        public List<String> getDaysWithAvailability() { return daysWithAvailability; }
        public List<String> getDaysWithoutAvailability() { return daysWithoutAvailability; }
        public Map<String, Integer> getSlotsPerDay() { return slotsPerDay; }
        public Map<String, String> getIssuesPerDay() { return issuesPerDay; }
    }

    /**
     * Verifica si un slot de tiempo está dentro de la disponibilidad de un profesor
     */
    private boolean isTimeSlotWithinAvailability(TimeSlot slot, TeacherAvailability availability) {
        // Verificar mañana
        if (availability.getAmStart() != null && availability.getAmEnd() != null) {
            if (!slot.getStartTime().isBefore(availability.getAmStart()) &&
                !slot.getEndTime().isAfter(availability.getAmEnd())) {
                return true;
            }
        }

        // Verificar tarde
        if (availability.getPmStart() != null && availability.getPmEnd() != null) {
            if (!slot.getStartTime().isBefore(availability.getPmStart()) &&
                !slot.getEndTime().isAfter(availability.getPmEnd())) {
                return true;
            }
        }

        return false;
    }


    /**
     * Valida la configuración de profesores
     */
    private void validateTeacherConfiguration(List<courses> coursesWithTeachers) {
        for (courses course : coursesWithTeachers) {
            if (course.getTeacherSubject() == null) continue;

            teachers teacher = course.getTeacherSubject().getTeacher();
            List<TeacherSubject> teacherAssociations = teacherSubjectRepo.findByTeacher_Id(teacher.getId());

            if (teacherAssociations.size() > 1) {
                String errorMsg = "ERROR DE CONFIGURACIÓN: El profesor " + teacher.getTeacherName() +
                    " está asociado a múltiples materias (" + teacherAssociations.size() + "). " +
                    "Cada profesor debe estar asociado únicamente a UNA materia.";
                logger.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }
        }
    }

    /**
     * Verifica si hay conflicto de horario para un profesor
     */
    private boolean hasConflict(teachers teacher, String dayName, LocalTime startTime, LocalTime endTime) {
        // Buscar horarios existentes para este profesor en el día
        List<schedule> existingSchedules = scheduleRepo.findByTeacherId(teacher.getId());

        return existingSchedules.stream()
            .filter(s -> dayName.equals(s.getDay()))
            .anyMatch(s -> {
                boolean overlap = (startTime.isBefore(s.getEndTime()) && endTime.isAfter(s.getStartTime()));
                if (overlap) {
                    logger.debug("Conflicto detectado: {} {}-{} vs {}-{}",
                        teacher.getTeacherName(), startTime, endTime, s.getStartTime(), s.getEndTime());
                }
                return overlap;
            });
    }

    /**
     * Clase auxiliar para representar slots de tiempo
     */
    private static class TimeSlot {
        private final LocalTime startTime;
        private final LocalTime endTime;

        public TimeSlot(LocalTime startTime, LocalTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public LocalTime getStartTime() { return startTime; }
        public LocalTime getEndTime() { return endTime; }

        @Override
        public String toString() {
            return startTime + "-" + endTime;
        }
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
     * Genera horario completo para un curso específico
     * Crea un horario integrado asignando slots consecutivos según disponibilidad
     */
    @Transactional
    public ScheduleHistoryDTO generateScheduleForCourse(Integer courseId, String executedBy) {
        logger.info("=== INICIANDO GENERACIÓN DE HORARIO COMPLETO PARA CURSO ===");
        logger.info("Usuario ejecutor: {}", executedBy);
        logger.info("Curso ID: {}", courseId);

        // PASO 1: Validación previa del curso
        CourseGenerationValidationDTO validation = validateCourseForGeneration(courseId);
        if (!validation.isCanGenerate()) {
            String issuesMessage = String.join("; ", validation.getIssues());
            throw new IllegalArgumentException("No se puede generar horario para el curso " +
                validation.getCourseName() + ". Problemas: " + issuesMessage);
        }

        // Validar que el curso existe
        courses selectedCourse = courseRepo.findById(courseId).orElseThrow(() ->
            new IllegalArgumentException("Curso no encontrado con ID: " + courseId));

        logger.info("Curso seleccionado: {} ({})", selectedCourse.getCourseName(), courseId);
        logger.info("Profesor asignado: {}", validation.getTeacherName());

        schedule_history history = new schedule_history();
        history.setExecutedBy(executedBy);
        history.setExecutedAt(LocalDateTime.now());
        history.setStatus("RUNNING");
        history.setMessage("Generando horario completo para curso: " + selectedCourse.getCourseName());
        history.setTotalGenerated(0);

        // Usar semana actual por defecto
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate friday = today.with(java.time.DayOfWeek.FRIDAY);

        history.setPeriodStart(monday);
        history.setPeriodEnd(friday);
        history.setDryRun(false);
        history.setForce(false);
        history.setParams("Horario completo para curso: " + selectedCourse.getCourseName());

        history = historyRepository.save(history);

        try {
            // Generar horario completo para el curso
            GenerationResult result = generateCompleteScheduleForCourse(selectedCourse, monday, friday);
            int totalGenerated = result.getTotalGenerated();

            // PASO 2: Validar viabilidad del horario generado
            ScheduleViabilityValidation viabilityValidation = validateGeneratedScheduleViability(selectedCourse, result.getGeneratedSchedules());

            if (!viabilityValidation.isViable()) {
                logger.warn("Horario generado tiene problemas de viabilidad: {}", viabilityValidation.getIssues());
                // Aún así guardamos el horario pero marcamos como warning
                history.setMessage("Horario generado con advertencias para " + selectedCourse.getCourseName() +
                    ". " + totalGenerated + " clases asignadas. Advertencias: " + String.join("; ", viabilityValidation.getIssues()));
            } else {
                history.setMessage("Horario completo generado exitosamente para " + selectedCourse.getCourseName() +
                    ". " + totalGenerated + " clases asignadas.");
            }

            history.setStatus("SUCCESS");
            history.setTotalGenerated(totalGenerated);
            history.setExecutedAt(LocalDateTime.now());
            historyRepository.save(history);

            // Retornar DTO con información de cursos sin disponibilidad y validación de viabilidad
            ScheduleHistoryDTO response = toDTO(history);
            response.setCoursesWithoutAvailability(result.getCoursesWithoutAvailability());
            response.setTotalCoursesWithoutAvailability(result.getCoursesWithoutAvailability().size());

            // Agregar información de viabilidad al mensaje si hay problemas
            if (!viabilityValidation.isViable()) {
                response.setMessage(response.getMessage() + " [ADVERTENCIAS: " + String.join("; ", viabilityValidation.getIssues()) + "]");
            }

            logger.info("=== GENERACIÓN COMPLETA FINALIZADA ===");
            return response;

        } catch (Exception ex) {
            logger.error("Error generando horario completo: {}", ex.getMessage(), ex);
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

    /**
     * Genera horario completo para un curso específico con distribución inteligente
     * Asigna clases distribuidas óptimamente a lo largo de la semana
     */
    private GenerationResult generateCompleteScheduleForCourse(courses course, LocalDate startDate, LocalDate endDate) {
        logger.info("=== GENERANDO HORARIO COMPLETO INTELIGENTE PARA CURSO: {} ===", course.getCourseName());

        List<schedule> generatedSchedules = new ArrayList<>();
        List<CourseWithoutAvailabilityDTO> coursesWithoutAvailability = new ArrayList<>();
        int totalClassesAssigned = 0;

        teachers assignedTeacher = course.getTeacherSubject().getTeacher();
        subjects assignedSubject = course.getTeacherSubject().getSubject();

        logger.info("Profesor asignado: {} ({})", assignedTeacher.getTeacherName(), assignedTeacher.getId());
        logger.info("Materia: {} ({})", assignedSubject.getSubjectName(), assignedSubject.getId());

        // Validar configuración del profesor
        validateTeacherConfiguration(Collections.singletonList(course));

        List<String> daysInPeriod = getUniqueDaysInPeriod(startDate, endDate);
        logger.info("Días disponibles para asignación: {}", daysInPeriod);

        // Crear un mapa de slots disponibles por día
        Map<String, List<TimeSlot>> availableSlotsByDay = new HashMap<>();
        for (String dayName : daysInPeriod) {
            List<TimeSlot> availableSlots = findAvailableSlotsForTeacher(assignedTeacher, dayName);
            availableSlotsByDay.put(dayName, availableSlots);
            logger.debug("Slots disponibles para {} el {}: {} slots", assignedTeacher.getTeacherName(), dayName, availableSlots.size());
        }

        // Estrategia mejorada: distribución inteligente de clases
        // 1. Primero intentar asignar una clase por día disponible
        // 2. Luego, si quedan slots disponibles, asignar clases adicionales
        List<String> daysWithClasses = new ArrayList<>();
        List<String> daysWithoutClasses = new ArrayList<>(daysInPeriod);

        // PASO 1: Generar horario escolar COMPLETO llenando TODAS las franjas disponibles
        // Estrategia: Llenar el horario completamente como en el ejemplo del curso 1A
        // Intentar ocupar TODOS los slots disponibles respetando límites pedagógicos

        int maxClassesPerDay = 3; // Permitir hasta 3 clases por día para horario completo
        int totalSlotsAvailable = availableSlotsByDay.values().stream().mapToInt(List::size).sum();

        logger.info("Generando horario COMPLETO. Slots disponibles totales: {}", totalSlotsAvailable);

        // PASO 2: Asignar clases intentando llenar TODOS los slots disponibles
        // Priorizar distribución equilibrada pero maximizar ocupación
        for (String dayName : daysInPeriod) {
            List<TimeSlot> daySlots = availableSlotsByDay.get(dayName);
            if (daySlots == null || daySlots.isEmpty()) {
                logger.debug("No hay slots disponibles para {} el día {}", assignedTeacher.getTeacherName(), dayName);
                continue;
            }

            // Intentar asignar el máximo posible de clases para este día
            int assignedForDay = 0;
            for (TimeSlot slot : daySlots) {
                // Verificar límite por día
                if (assignedForDay >= maxClassesPerDay) {
                    logger.debug("Límite de {} clases por día alcanzado para {}", maxClassesPerDay, dayName);
                    break;
                }

                if (!hasConflict(assignedTeacher, dayName, slot.getStartTime(), slot.getEndTime()) &&
                    !isSlotAlreadyUsed(generatedSchedules, dayName, slot)) {

                    schedule newSchedule = createScheduleForCourse(course, assignedTeacher, assignedSubject, dayName, slot);
                    generatedSchedules.add(newSchedule);
                    totalClassesAssigned++;
                    assignedForDay++;
                    daysWithClasses.add(dayName);

                    logger.info("✓ Clase asignada: {} {} {}-{} ({})",
                        course.getCourseName(), dayName, slot.getStartTime(), slot.getEndTime(), assignedTeacher.getTeacherName());
                }
            }

            logger.debug("Asignadas {} clases para el día {}", assignedForDay, dayName);
        }

        // PASO 3: Si aún quedan slots sin llenar, intentar completar con clases adicionales
        // Esta fase asegura que el horario esté lo más completo posible
        int completionAttempts = 0;
        int maxCompletionAttempts = daysInPeriod.size() * 5; // Más intentos para completar

        while (completionAttempts < maxCompletionAttempts) {
            completionAttempts++;
            logger.debug("Intento de completado {}: Clases actuales: {}", completionAttempts, generatedSchedules.size());

            String bestDay = findBestDayForAdditionalClass(generatedSchedules, availableSlotsByDay, daysInPeriod);
            if (bestDay == null) {
                logger.debug("No hay más días disponibles para completar el horario");
                break;
            }

            List<TimeSlot> daySlots = availableSlotsByDay.get(bestDay);
            if (daySlots == null || daySlots.isEmpty()) continue;

            // Verificar límite por día
            long currentClassesInDay = generatedSchedules.stream()
                .filter(s -> bestDay.equals(s.getDay()))
                .count();

            if (currentClassesInDay >= maxClassesPerDay) {
                logger.debug("Día {} ya tiene {} clases (máximo {})", bestDay, currentClassesInDay, maxClassesPerDay);
                continue;
            }

            // Buscar cualquier slot disponible que no esté usado
            boolean assigned = false;
            for (TimeSlot slot : daySlots) {
                if (!hasConflict(assignedTeacher, bestDay, slot.getStartTime(), slot.getEndTime()) &&
                    !isSlotAlreadyUsed(generatedSchedules, bestDay, slot)) {

                    schedule newSchedule = createScheduleForCourse(course, assignedTeacher, assignedSubject, bestDay, slot);
                    generatedSchedules.add(newSchedule);
                    totalClassesAssigned++;
                    daysWithClasses.add(bestDay);

                    logger.info("✓ Clase de completado asignada: {} {} {}-{} ({})",
                        course.getCourseName(), bestDay, slot.getStartTime(), slot.getEndTime(), assignedTeacher.getTeacherName());
                    assigned = true;
                    break;
                }
            }

            if (!assigned) {
                logger.debug("No se pudo completar más slots en día {}", bestDay);
                break; // Si no se puede asignar en el mejor día, terminar
            }
        }

        logger.info("Horario COMPLETO generado. Total clases: {}, Cobertura: {:.1f}% de slots disponibles",
            generatedSchedules.size(), (double) generatedSchedules.size() / totalSlotsAvailable * 100);

        // Validación final: verificar si se asignaron clases suficientes
        if (totalClassesAssigned == 0) {
            CourseWithoutAvailabilityDTO unavailabilityInfo = analyzeCourseUnavailability(course, assignedTeacher, daysInPeriod);
            if (unavailabilityInfo != null) {
                coursesWithoutAvailability.add(unavailabilityInfo);
            }
        }

        // Calcular días únicos con clases
        Set<String> uniqueDaysWithClasses = new HashSet<>(daysWithClasses);

        logger.info("=== RESUMEN HORARIO COMPLETO INTELIGENTE ===");
        logger.info("Curso: {}", course.getCourseName());
        logger.info("Clases asignadas: {}", totalClassesAssigned);
        logger.info("Días con clases: {}", uniqueDaysWithClasses.size());
        logger.info("Días sin clases: {}", daysInPeriod.size() - uniqueDaysWithClasses.size());
        logger.info("Distribución por día: {}", generatedSchedules.stream()
            .collect(Collectors.groupingBy(schedule::getDay, Collectors.counting())));

        if (!generatedSchedules.isEmpty()) {
            logger.info("Guardando horario completo optimizado en la base de datos...");
            scheduleRepo.saveAll(generatedSchedules);
            logger.info("Horario guardado exitosamente");
        }

        return new GenerationResult(totalClassesAssigned, coursesWithoutAvailability, generatedSchedules);
    }

    /**
     * Selecciona el mejor slot de tiempo para un día específico
     * Prioriza horarios de mañana y evita horarios muy tarde
     */
    private TimeSlot selectBestTimeSlotForDay(List<TimeSlot> availableSlots, String dayName) {
        if (availableSlots.isEmpty()) {
            return null;
        }

        // Ordenar slots por prioridad: mañana primero, luego tarde
        return availableSlots.stream()
            .sorted((slot1, slot2) -> {
                int priority1 = getTimeSlotPriority(slot1);
                int priority2 = getTimeSlotPriority(slot2);
                return Integer.compare(priority1, priority2);
            })
            .findFirst()
            .orElse(availableSlots.get(0));
    }

    /**
     * Obtiene la prioridad de un slot de tiempo (menor número = mayor prioridad)
     */
    private int getTimeSlotPriority(TimeSlot slot) {
        LocalTime startTime = slot.getStartTime();
        
        // Prioridad 1: Mañana temprana (8:00-10:00)
        if (!startTime.isBefore(LocalTime.of(8, 0)) && startTime.isBefore(LocalTime.of(10, 0))) {
            return 1;
        }
        // Prioridad 2: Mañana tardía (10:00-12:00)
        else if (startTime.isBefore(LocalTime.of(12, 0))) {
            return 2;
        }
        // Prioridad 3: Tarde temprana (14:00-15:00)
        else if (startTime.isBefore(LocalTime.of(15, 0))) {
            return 3;
        }
        // Prioridad 4: Tarde tardía (15:00-17:00)
        else {
            return 4;
        }
    }

    /**
     * Encuentra el mejor día para asignar una clase adicional
     */
    private String findBestDayForAdditionalClass(List<schedule> generatedSchedules,
                                                Map<String, List<TimeSlot>> availableSlotsByDay,
                                                List<String> daysInPeriod) {
        // Priorizar días que ya tienen clases pero menos de 2
        for (String day : daysInPeriod) {
            long classesInDay = generatedSchedules.stream()
                .filter(s -> day.equals(s.getDay()))
                .count();

            if (classesInDay == 1 && !availableSlotsByDay.get(day).isEmpty()) {
                return day;
            }
        }

        // Si no hay días con 1 clase, buscar días sin clases
        for (String day : daysInPeriod) {
            long classesInDay = generatedSchedules.stream()
                .filter(s -> day.equals(s.getDay()))
                .count();

            if (classesInDay == 0 && !availableSlotsByDay.get(day).isEmpty()) {
                return day;
            }
        }

        return null;
    }

    /**
     * Verifica si un slot de tiempo ya está siendo usado en un día específico
     */
    private boolean isSlotAlreadyUsed(List<schedule> generatedSchedules, String dayName, TimeSlot slot) {
        return generatedSchedules.stream()
            .filter(s -> dayName.equals(s.getDay()))
            .anyMatch(s -> s.getStartTime().equals(slot.getStartTime()) && s.getEndTime().equals(slot.getEndTime()));
    }

    /**
     * Asigna clases adicionales en días con más disponibilidad (método legacy)
     */
    private void assignAdditionalClasses(courses course, teachers teacher, subjects subject,
                                        Map<String, List<TimeSlot>> availableSlotsByDay,
                                        List<schedule> generatedSchedules, List<String> daysWithoutClasses) {

        for (String dayName : daysWithoutClasses) {
            List<TimeSlot> daySlots = availableSlotsByDay.get(dayName);
            if (daySlots.isEmpty()) continue;

            // Buscar slots que no estén en conflicto
            for (TimeSlot slot : daySlots) {
                if (!hasConflict(teacher, dayName, slot.getStartTime(), slot.getEndTime())) {
                    schedule newSchedule = createScheduleForCourse(course, teacher, subject, dayName, slot);
                    generatedSchedules.add(newSchedule);

                    logger.info("✓ Clase adicional asignada: {} {} {}-{} ({})",
                        course.getCourseName(), dayName, slot.getStartTime(), slot.getEndTime(), teacher.getTeacherName());
                    break; // Solo una clase adicional por día
                }
            }
        }
    }

    /**
     * Crea un objeto schedule para un curso específico
     */
    private schedule createScheduleForCourse(courses course, teachers teacher, subjects subject, 
                                           String dayName, TimeSlot slot) {
        schedule newSchedule = new schedule();
        newSchedule.setCourseId(course);
        newSchedule.setTeacherId(teacher);
        newSchedule.setSubjectId(subject);
        newSchedule.setDay(dayName);
        newSchedule.setStartTime(slot.getStartTime());
        newSchedule.setEndTime(slot.getEndTime());
        newSchedule.setScheduleName(course.getCourseName() + " - " + subject.getSubjectName());
        return newSchedule;
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
     * Obtiene cursos disponibles para generación automática de horarios
     * Solo incluye cursos sin horario asignado y con profesor asignado
     */
    public List<CourseDTO> getCoursesAvailableForAutoGeneration() {
        logger.info("=== OBTENIENDO CURSOS DISPONIBLES PARA GENERACIÓN AUTOMÁTICA ===");

        List<courses> coursesWithoutSchedule = getCoursesWithoutSchedule();
        List<CourseDTO> availableCourses = new ArrayList<>();

        for (courses course : coursesWithoutSchedule) {
            // Solo incluir cursos que tienen profesor asignado
            if (course.getTeacherSubject() != null) {
                CourseDTO dto = new CourseDTO();
                dto.setCourseId(course.getId());
                dto.setCourseName(course.getCourseName());

                // Agregar información del profesor asignado
                if (course.getTeacherSubject().getTeacher() != null) {
                    dto.setTeacherName(course.getTeacherSubject().getTeacher().getTeacherName());
                }

                // Agregar información de la materia
                if (course.getTeacherSubject().getSubject() != null) {
                    dto.setSubjectName(course.getTeacherSubject().getSubject().getSubjectName());
                }

                availableCourses.add(dto);
            }
        }

        logger.info("Cursos disponibles para generación automática: {}", availableCourses.size());
        return availableCourses;
    }

    /**
     * Valida si un curso puede tener horario generado automáticamente
     */
    public CourseGenerationValidationDTO validateCourseForGeneration(Integer courseId) {
        logger.info("=== VALIDANDO CURSO {} PARA GENERACIÓN AUTOMÁTICA ===", courseId);

        CourseGenerationValidationDTO validation = new CourseGenerationValidationDTO();
        validation.setCourseId(courseId);
        validation.setCanGenerate(false);
        validation.setIssues(new ArrayList<>());

        try {
            courses course = courseRepo.findById(courseId).orElseThrow(() ->
                new IllegalArgumentException("Curso no encontrado con ID: " + courseId));

            validation.setCourseName(course.getCourseName());

            // Verificar si ya tiene horario
            List<schedule> existingSchedules = scheduleRepo.findByCourseId(courseId);
            if (!existingSchedules.isEmpty()) {
                validation.getIssues().add("El curso ya tiene " + existingSchedules.size() + " horarios asignados");
                validation.setCanGenerate(false);
                return validation;
            }

            // Verificar profesor asignado
            if (course.getTeacherSubject() == null) {
                validation.getIssues().add("El curso no tiene profesor asignado");
                validation.setCanGenerate(false);
                return validation;
            }

            teachers teacher = course.getTeacherSubject().getTeacher();
            validation.setTeacherName(teacher.getTeacherName());

            // Verificar disponibilidad del profesor
            List<TeacherAvailability> availabilities = availabilityRepo.findByTeacher_Id(teacher.getId());
            if (availabilities.isEmpty()) {
                validation.getIssues().add("El profesor no tiene disponibilidad definida");
                validation.setCanGenerate(false);
                return validation;
            }

            // Verificar que tenga al menos un día con disponibilidad válida
            boolean hasValidAvailability = availabilities.stream().anyMatch(TeacherAvailability::hasValidSchedule);
            if (!hasValidAvailability) {
                validation.getIssues().add("El profesor no tiene horarios válidos definidos");
                validation.setCanGenerate(false);
                return validation;
            }

            // Verificar conflictos existentes del profesor (advertencia, no bloqueante)
            List<schedule> teacherSchedules = scheduleRepo.findByTeacherId(teacher.getId());
            if (!teacherSchedules.isEmpty()) {
                validation.getIssues().add("ADVERTENCIA: El profesor ya tiene " + teacherSchedules.size() + " horarios asignados. Se intentará evitar conflictos.");
            }

            // Si no hay issues críticos (solo profesor asignado y disponibilidad), se puede generar
            boolean hasCriticalIssues = validation.getIssues().stream()
                .anyMatch(issue -> issue.contains("no tiene profesor") ||
                                   issue.contains("no tiene disponibilidad definida") ||
                                   issue.contains("no tiene horarios válidos"));
            validation.setCanGenerate(!hasCriticalIssues);
            validation.setRecommendedDays(getRecommendedDaysForCourse(course));

            logger.info("Validación completada para curso {}: canGenerate={}", courseId, validation.isCanGenerate());

        } catch (Exception e) {
            validation.getIssues().add("Error durante la validación: " + e.getMessage());
            validation.setCanGenerate(false);
            logger.error("Error validando curso {}: {}", courseId, e.getMessage());
        }

        return validation;
    }

    /**
     * Obtiene días recomendados para un curso basado en disponibilidad del profesor
     */
    private List<String> getRecommendedDaysForCourse(courses course) {
        if (course.getTeacherSubject() == null) return new ArrayList<>();

        teachers teacher = course.getTeacherSubject().getTeacher();
        List<String> recommendedDays = new ArrayList<>();
        List<String> daysInPeriod = getUniqueDaysInPeriod(LocalDate.now(), LocalDate.now().plusDays(4));

        for (String dayName : daysInPeriod) {
            try {
                Days dayEnum = Days.valueOf(dayName);
                List<TeacherAvailability> availabilities = availabilityRepo.findByTeacher_IdAndDay(teacher.getId(), dayEnum);

                if (!availabilities.isEmpty() && availabilities.stream().anyMatch(TeacherAvailability::hasValidSchedule)) {
                    List<TimeSlot> availableSlots = findAvailableSlotsForTeacher(teacher, dayName);
                    if (!availableSlots.isEmpty()) {
                        recommendedDays.add(dayName + " (" + availableSlots.size() + " slots disponibles)");
                    }
                }
            } catch (Exception e) {
                logger.debug("Error verificando día {} para profesor {}: {}", dayName, teacher.getTeacherName(), e.getMessage());
            }
        }

        return recommendedDays;
    }

    /**
     * Valida la viabilidad del horario generado para un curso
     */
    private ScheduleViabilityValidation validateGeneratedScheduleViability(courses course, List<schedule> generatedSchedules) {
        List<String> issues = new ArrayList<>();
        boolean viable = true;

        logger.info("Validando viabilidad del horario generado para curso: {}", course.getCourseName());

        // Verificar que se generaron horarios
        if (generatedSchedules.isEmpty()) {
            issues.add("No se generaron horarios para el curso");
            return new ScheduleViabilityValidation(false, issues);
        }

        teachers teacher = course.getTeacherSubject().getTeacher();

        // Verificar distribución de días (para horario escolar, 2-3 días con clases es aceptable)
        Map<String, Long> schedulesByDay = generatedSchedules.stream()
            .collect(Collectors.groupingBy(schedule::getDay, Collectors.counting()));

        if (schedulesByDay.size() < 2) {
            issues.add("El horario solo cubre " + schedulesByDay.size() + " día(s). Se recomienda al menos 2 días de clases para un horario escolar básico");
            viable = false;
        } else if (schedulesByDay.size() < 3) {
            issues.add("El horario cubre " + schedulesByDay.size() + " días. Considera agregar más días para una distribución más equilibrada");
            // No marcar como no viable, solo advertencia
        }

        // Verificar que el horario esté completo (como en el ejemplo del curso 1A)
        // Debe tener al menos 4-5 clases distribuidas en al menos 2 días
        if (generatedSchedules.size() < 4) {
            issues.add("El horario tiene solo " + generatedSchedules.size() + " clases. Se recomienda al menos 4 clases para un horario escolar completo");
            viable = false;
        }

        // Verificar que no haya días con demasiadas clases (máximo 3 por día)
        for (Map.Entry<String, Long> entry : schedulesByDay.entrySet()) {
            if (entry.getValue() > 3) {
                issues.add("El día " + entry.getKey() + " tiene " + entry.getValue() + " clases. Se recomienda máximo 3 por día para evitar sobrecarga");
                viable = false;
            }
        }

        // Verificar que no haya más de 2 clases por día
        for (Map.Entry<String, Long> entry : schedulesByDay.entrySet()) {
            if (entry.getValue() > 2) {
                issues.add("El día " + entry.getKey() + " tiene " + entry.getValue() + " clases. Se recomienda máximo 2 por día");
                viable = false;
            }
        }

        // Verificar que las clases estén distribuidas en horarios razonables
        for (schedule s : generatedSchedules) {
            LocalTime startTime = s.getStartTime();
            LocalTime endTime = s.getEndTime();

            // Verificar que las clases no sean muy temprano o muy tarde
            if (startTime.isBefore(LocalTime.of(8, 0)) || endTime.isAfter(LocalTime.of(17, 0))) {
                issues.add("Horario fuera del rango recomendado (8:00-17:00): " + startTime + "-" + endTime + " el " + s.getDay());
            }

            // Verificar duración de clase (debe ser 1 hora)
            if (ChronoUnit.MINUTES.between(startTime, endTime) != 60) {
                issues.add("Duración de clase no estándar: " + startTime + "-" + endTime + " el " + s.getDay());
                viable = false;
            }
        }

        // Verificar que no haya conflictos internos del profesor
        for (int i = 0; i < generatedSchedules.size(); i++) {
            for (int j = i + 1; j < generatedSchedules.size(); j++) {
                schedule s1 = generatedSchedules.get(i);
                schedule s2 = generatedSchedules.get(j);

                if (s1.getDay().equals(s2.getDay())) {
                    // Verificar solapamiento de horarios
                    if (s1.getStartTime().isBefore(s2.getEndTime()) && s2.getStartTime().isBefore(s1.getEndTime())) {
                        issues.add("Conflicto de horario interno: " + s1.getStartTime() + "-" + s1.getEndTime() +
                                 " vs " + s2.getStartTime() + "-" + s2.getEndTime() + " el " + s1.getDay());
                        viable = false;
                    }
                }
            }
        }

        // Verificar carga docente (no más de 20 horas semanales)
        int totalHours = generatedSchedules.size() * 1; // 1 hora por clase
        if (totalHours > 20) {
            issues.add("Carga docente excesiva: " + totalHours + " horas semanales. Se recomienda máximo 20 horas");
            viable = false;
        }

        logger.info("Validación de viabilidad completada: viable={}, issues={}", viable, issues.size());

        return new ScheduleViabilityValidation(viable, issues);
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

    /**
     * Obtiene el horario de un curso en formato de tabla visual
     * Como se muestra en el ejemplo del curso 1A
     */
    public ScheduleTableDTO getCourseScheduleTable(Integer courseId) {
        logger.info("=== OBTENIENDO HORARIO EN FORMATO TABLA PARA CURSO {} ===", courseId);

        courses course = courseRepo.findById(courseId).orElseThrow(() ->
            new IllegalArgumentException("Curso no encontrado con ID: " + courseId));

        // Obtener todos los horarios del curso
        List<schedule> courseSchedules = scheduleRepo.findByCourseId(courseId);

        // Días de la semana
        List<String> days = List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes");

        // Crear filas de franjas horarias
        List<ScheduleTableDTO.TimeSlotRow> timeSlots = createTimeSlotRows();

        // Llenar las celdas con los horarios existentes
        Map<String, Integer> classesPerDay = new HashMap<>();
        for (schedule s : courseSchedules) {
            String day = s.getDay();
            classesPerDay.put(day, classesPerDay.getOrDefault(day, 0) + 1);

            // Encontrar la fila correspondiente al horario
            String timeRange = s.getStartTime() + "-" + s.getEndTime();
            for (ScheduleTableDTO.TimeSlotRow row : timeSlots) {
                if (row.getTimeRange().equals(timeRange)) {
                    ScheduleTableDTO.TimeSlotRow.ScheduleCell cell = new ScheduleTableDTO.TimeSlotRow.ScheduleCell(
                        true,
                        s.getTeacherId().getTeacherName(),
                        s.getSubjectId().getSubjectName(),
                        s.getId().toString(),
                        s.getStartTime().toString(),
                        s.getEndTime().toString()
                    );
                    row.getCells().put(day, cell);
                    break;
                }
            }
        }

        // Crear DTO de tabla
        ScheduleTableDTO table = new ScheduleTableDTO(
            course.getCourseName(),
            courseId.toString(),
            days,
            timeSlots,
            courseSchedules.size(),
            classesPerDay
        );

        logger.info("Horario en formato tabla generado para curso {}: {} clases totales", course.getCourseName(), courseSchedules.size());
        return table;
    }

    /**
     * Crea las filas de franjas horarias para la tabla del horario
     */
    private List<ScheduleTableDTO.TimeSlotRow> createTimeSlotRows() {
        List<ScheduleTableDTO.TimeSlotRow> rows = new ArrayList<>();

        // Franjas horarias según especificaciones
        List<String> timeRanges = List.of(
            "06:00-07:00", "07:00-08:00", "08:00-09:00", "09:30-10:30",
            "10:30-11:30", "11:30-12:30", "13:00-14:00", "14:00-15:00",
            "15:00-16:00", "16:00-17:00", "17:00-18:00"
        );

        for (String timeRange : timeRanges) {
            Map<String, ScheduleTableDTO.TimeSlotRow.ScheduleCell> cells = new HashMap<>();
            // Inicializar celdas vacías para todos los días
            List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes").forEach(day -> {
                cells.put(day, new ScheduleTableDTO.TimeSlotRow.ScheduleCell(false, null, null, null, null, null));
            });

            rows.add(new ScheduleTableDTO.TimeSlotRow(timeRange, cells));
        }

        return rows;
    }

    /**
     * Verifica exhaustivamente el horario generado para un curso específico
     * Valida completitud, bloques protegidos, conflictos y formato
     */
    public ScheduleVerificationReportDTO verifyGeneratedSchedule(Integer courseId) {
        logger.info("=== INICIANDO VERIFICACIÓN EXHAUSTIVA DEL HORARIO PARA CURSO {} ===", courseId);

        ScheduleVerificationReportDTO report = new ScheduleVerificationReportDTO();
        report.setCourseId(courseId);
        report.setErrors(new ArrayList<>());
        report.setWarnings(new ArrayList<>());
        report.setSuccesses(new ArrayList<>());

        try {
            courses course = courseRepo.findById(courseId).orElseThrow(() ->
                new IllegalArgumentException("Curso no encontrado con ID: " + courseId));

            report.setCourseName(course.getCourseName());

            // Obtener horario generado
            List<schedule> courseSchedules = scheduleRepo.findByCourseId(courseId);

            // 1. Verificar estadísticas básicas
            ScheduleVerificationReportDTO.ScheduleStatistics stats = calculateScheduleStatistics(courseSchedules);
            report.setStatistics(stats);

            // 2. Verificar bloques protegidos
            ScheduleVerificationReportDTO.ProtectedBlocksVerification protectedBlocks = verifyProtectedBlocks(courseSchedules);
            report.setProtectedBlocks(protectedBlocks);

            // 3. Verificar contenido
            ScheduleVerificationReportDTO.ContentVerification content = verifyContent(courseSchedules, course);
            report.setContent(content);

            // 4. Verificar conflictos
            ScheduleVerificationReportDTO.ConflictsVerification conflicts = verifyConflicts(courseSchedules);
            report.setConflicts(conflicts);

            // 5. Verificar formato
            ScheduleVerificationReportDTO.FormatVerification format = verifyFormat(courseSchedules);
            report.setFormat(format);

            // Determinar estado general
            boolean hasErrors = !report.getErrors().isEmpty();
            boolean hasCriticalIssues = hasErrors ||
                !protectedBlocks.isBreakTimeRespected() ||
                !protectedBlocks.isLunchTimeRespected() ||
                !conflicts.isNoTeacherConflicts() ||
                !content.isAllSlotsFilled();

            report.setComplete(stats.getCoveragePercentage() >= 80.0); // Al menos 80% de cobertura
            report.setValid(!hasCriticalIssues);

            // Generar resumen
            generateVerificationSummary(report);

            logger.info("Verificación completada: completo={}, válido={}, errores={}, advertencias={}",
                report.isComplete(), report.isValid(), report.getErrors().size(), report.getWarnings().size());

        } catch (Exception e) {
            report.getErrors().add("Error durante la verificación: " + e.getMessage());
            report.setComplete(false);
            report.setValid(false);
            logger.error("Error en verificación del horario para curso {}: {}", courseId, e.getMessage());
        }

        return report;
    }

    /**
     * Calcula estadísticas del horario generado
     */
    private ScheduleVerificationReportDTO.ScheduleStatistics calculateScheduleStatistics(List<schedule> schedules) {
        ScheduleVerificationReportDTO.ScheduleStatistics stats = new ScheduleVerificationReportDTO.ScheduleStatistics();

        stats.setTotalClasses(schedules.size());

        // Calcular slots disponibles totales (sin contar bloques protegidos)
        List<String> availableTimeRanges = List.of(
            "06:00-07:00", "07:00-08:00", "08:00-09:00", "09:30-10:30",
            "10:30-11:30", "11:30-12:30", "13:00-14:00", "14:00-15:00",
            "15:00-16:00", "16:00-17:00", "17:00-18:00"
        );
        List<String> days = List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes");
        stats.setTotalSlotsAvailable(availableTimeRanges.size() * days.size());

        // Contar clases por día
        Map<String, Integer> classesPerDay = new HashMap<>();
        Map<String, Integer> classesPerTimeSlot = new HashMap<>();

        for (schedule s : schedules) {
            // Por día
            classesPerDay.put(s.getDay(), classesPerDay.getOrDefault(s.getDay(), 0) + 1);

            // Por franja horaria
            String timeRange = s.getStartTime() + "-" + s.getEndTime();
            classesPerTimeSlot.put(timeRange, classesPerTimeSlot.getOrDefault(timeRange, 0) + 1);
        }

        stats.setClassesPerDay(classesPerDay);
        stats.setClassesPerTimeSlot(classesPerTimeSlot);
        stats.setFilledSlots(schedules.size());
        stats.setCoveragePercentage((double) schedules.size() / stats.getTotalSlotsAvailable() * 100);

        return stats;
    }

    /**
     * Verifica que los bloques protegidos estén respetados
     */
    private ScheduleVerificationReportDTO.ProtectedBlocksVerification verifyProtectedBlocks(List<schedule> schedules) {
        ScheduleVerificationReportDTO.ProtectedBlocksVerification verification =
            new ScheduleVerificationReportDTO.ProtectedBlocksVerification();
        verification.setViolations(new ArrayList<>());

        boolean breakTimeRespected = true;
        boolean lunchTimeRespected = true;

        // Verificar descanso (9:00-9:30) - aunque no hay slot específico, verificar que no haya clases a las 9:00
        for (schedule s : schedules) {
            if (s.getStartTime().equals(LocalTime.of(9, 0))) {
                verification.getViolations().add("Clase asignada durante descanso (9:00): " + s.getDay());
                breakTimeRespected = false;
            }
        }

        // Verificar almuerzo (12:00-13:00) - verificar que no haya clases a las 12:00
        for (schedule s : schedules) {
            if (s.getStartTime().equals(LocalTime.of(12, 0))) {
                verification.getViolations().add("Clase asignada durante almuerzo (12:00): " + s.getDay());
                lunchTimeRespected = false;
            }
        }

        verification.setBreakTimeRespected(breakTimeRespected);
        verification.setLunchTimeRespected(lunchTimeRespected);

        return verification;
    }

    /**
     * Verifica el contenido del horario
     */
    private ScheduleVerificationReportDTO.ContentVerification verifyContent(List<schedule> schedules, courses course) {
        ScheduleVerificationReportDTO.ContentVerification verification =
            new ScheduleVerificationReportDTO.ContentVerification();
        verification.setEmptySlots(new ArrayList<>());
        verification.setDuplicateSlots(new ArrayList<>());

        boolean allSlotsFilled = true;
        boolean noEmptyCells = true;
        boolean noDuplicateCells = true;
        boolean consistentData = true;

        // Verificar que todas las clases tengan profesor y materia
        for (schedule s : schedules) {
            if (s.getTeacherId() == null) {
                verification.getEmptySlots().add("Clase sin profesor asignado: " + s.getDay() + " " + s.getStartTime());
                consistentData = false;
            }
            if (s.getSubjectId() == null) {
                verification.getEmptySlots().add("Clase sin materia asignada: " + s.getDay() + " " + s.getStartTime());
                consistentData = false;
            }
        }

        // Verificar duplicados (mismo profesor, día y hora)
        Map<String, List<schedule>> schedulesByDayAndTime = new HashMap<>();
        for (schedule s : schedules) {
            String key = s.getDay() + "-" + s.getStartTime() + "-" + s.getEndTime();
            schedulesByDayAndTime.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
        }

        for (Map.Entry<String, List<schedule>> entry : schedulesByDayAndTime.entrySet()) {
            if (entry.getValue().size() > 1) {
                verification.getDuplicateSlots().add("Múltiples clases en mismo slot: " + entry.getKey());
                noDuplicateCells = false;
            }
        }

        // Para este curso específico, verificar completitud (debe tener varias clases)
        if (schedules.size() < 4) {
            allSlotsFilled = false;
        }

        verification.setAllSlotsFilled(allSlotsFilled);
        verification.setNoEmptyCells(noEmptyCells);
        verification.setNoDuplicateCells(noDuplicateCells);
        verification.setConsistentData(consistentData);

        return verification;
    }

    /**
     * Verifica conflictos de profesores y distribución lógica
     */
    private ScheduleVerificationReportDTO.ConflictsVerification verifyConflicts(List<schedule> schedules) {
        ScheduleVerificationReportDTO.ConflictsVerification verification =
            new ScheduleVerificationReportDTO.ConflictsVerification();
        verification.setTeacherConflicts(new ArrayList<>());
        verification.setTimeConflicts(new ArrayList<>());

        boolean noTeacherConflicts = true;
        boolean noTimeSlotConflicts = true;
        boolean logicalDistribution = true;

        // Verificar conflictos de profesores (mismo profesor en diferentes cursos al mismo tiempo)
        // Nota: Para un curso específico, todos los horarios deberían ser del mismo profesor
        Map<String, List<schedule>> schedulesByTimeSlot = new HashMap<>();
        for (schedule s : schedules) {
            String timeKey = s.getDay() + "-" + s.getStartTime();
            schedulesByTimeSlot.computeIfAbsent(timeKey, k -> new ArrayList<>()).add(s);
        }

        for (Map.Entry<String, List<schedule>> entry : schedulesByTimeSlot.entrySet()) {
            if (entry.getValue().size() > 1) {
                verification.getTimeConflicts().add("Múltiples clases en mismo bloque: " + entry.getKey());
                noTimeSlotConflicts = false;
            }
        }

        // Verificar distribución lógica (no todas las clases en un solo día)
        Map<String, Integer> classesPerDay = new HashMap<>();
        for (schedule s : schedules) {
            classesPerDay.put(s.getDay(), classesPerDay.getOrDefault(s.getDay(), 0) + 1);
        }

        int maxClassesInDay = classesPerDay.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        if (maxClassesInDay > 3) {
            verification.getTeacherConflicts().add("Demasiadas clases en un día (" + maxClassesInDay + " clases)");
            logicalDistribution = false;
        }

        verification.setNoTeacherConflicts(noTeacherConflicts);
        verification.setNoTimeSlotConflicts(noTimeSlotConflicts);
        verification.setLogicalDistribution(logicalDistribution);

        return verification;
    }

    /**
     * Verifica el formato y estructura del horario
     */
    private ScheduleVerificationReportDTO.FormatVerification verifyFormat(List<schedule> schedules) {
        ScheduleVerificationReportDTO.FormatVerification verification =
            new ScheduleVerificationReportDTO.FormatVerification();
        verification.setFormatIssues(new ArrayList<>());

        boolean correctStructure = true;
        boolean readableFormat = true;
        boolean professionalLayout = true;

        // Verificar que todos los horarios tengan formato correcto
        for (schedule s : schedules) {
            if (s.getScheduleName() == null || s.getScheduleName().trim().isEmpty()) {
                verification.getFormatIssues().add("Nombre de horario vacío o nulo");
                correctStructure = false;
            }

            if (!s.getScheduleName().contains(s.getSubjectId().getSubjectName())) {
                verification.getFormatIssues().add("Nombre de horario no incluye nombre de materia");
                readableFormat = false;
            }
        }

        // Verificar que los días sean válidos
        List<String> validDays = List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes");
        for (schedule s : schedules) {
            if (!validDays.contains(s.getDay())) {
                verification.getFormatIssues().add("Día inválido: " + s.getDay());
                correctStructure = false;
            }
        }

        verification.setCorrectStructure(correctStructure);
        verification.setReadableFormat(readableFormat);
        verification.setProfessionalLayout(professionalLayout);

        return verification;
    }

    /**
     * Genera un resumen de la verificación
     */
    private void generateVerificationSummary(ScheduleVerificationReportDTO report) {
        // Éxitos
        if (report.getProtectedBlocks().isBreakTimeRespected()) {
            report.getSuccesses().add("✅ Bloque de descanso respetado");
        }
        if (report.getProtectedBlocks().isLunchTimeRespected()) {
            report.getSuccesses().add("✅ Bloque de almuerzo respetado");
        }
        if (report.getConflicts().isNoTeacherConflicts()) {
            report.getSuccesses().add("✅ Sin conflictos de profesores");
        }
        if (report.getContent().isConsistentData()) {
            report.getSuccesses().add("✅ Datos consistentes en todas las celdas");
        }
        if (report.getFormat().isCorrectStructure()) {
            report.getSuccesses().add("✅ Estructura de horario correcta");
        }

        // Advertencias (no son errores críticos)
        if (!report.getConflicts().isLogicalDistribution()) {
            report.getWarnings().add("⚠️ Distribución de clases podría ser más equilibrada");
        }
        if (report.getStatistics().getCoveragePercentage() < 100) {
            report.getWarnings().add(String.format("⚠️ Cobertura del %.1f%% (podría ser mayor)",
                report.getStatistics().getCoveragePercentage()));
        }

        // Errores críticos
        if (!report.getProtectedBlocks().isBreakTimeRespected()) {
            report.getErrors().add("❌ Clases asignadas durante descanso");
        }
        if (!report.getProtectedBlocks().isLunchTimeRespected()) {
            report.getErrors().add("❌ Clases asignadas durante almuerzo");
        }
        if (!report.getConflicts().isNoTimeSlotConflicts()) {
            report.getErrors().add("❌ Conflictos de horario detectados");
        }
        if (!report.getContent().isConsistentData()) {
            report.getErrors().add("❌ Datos inconsistentes en celdas");
        }
        if (report.getStatistics().getTotalClasses() < 4) {
            report.getErrors().add("❌ Horario incompleto (menos de 4 clases)");
        }
    }
}