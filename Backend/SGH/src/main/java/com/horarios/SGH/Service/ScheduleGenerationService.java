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
     * Crea slots de tiempo realistas para un día escolar
     */
    private List<TimeSlot> createTimeSlotsForDay() {
        List<TimeSlot> slots = new ArrayList<>();

        // Mañana: 8:00-12:00 (clases de 1 hora)
        for (int hour = 8; hour < 12; hour++) {
            slots.add(new TimeSlot(
                LocalTime.of(hour, 0),
                LocalTime.of(hour + 1, 0)
            ));
        }

        // Tarde: 14:00-17:00 (clases de 1 hora, después del almuerzo)
        for (int hour = 14; hour < 17; hour++) {
            slots.add(new TimeSlot(
                LocalTime.of(hour, 0),
                LocalTime.of(hour + 1, 0)
            ));
        }

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

        // Validar que el curso existe
        courses selectedCourse = courseRepo.findById(courseId).orElseThrow(() ->
            new IllegalArgumentException("Curso no encontrado con ID: " + courseId));

        logger.info("Curso seleccionado: {} ({})", selectedCourse.getCourseName(), courseId);

        // Validar que el curso tenga profesor asignado
        if (selectedCourse.getTeacherSubject() == null) {
            throw new IllegalArgumentException("El curso " + selectedCourse.getCourseName() +
                " no tiene profesor asignado. Asigne un profesor antes de generar el horario.");
        }

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

            history.setStatus("SUCCESS");
            history.setTotalGenerated(totalGenerated);
            history.setMessage("Horario completo generado exitosamente para " + selectedCourse.getCourseName() +
                ". " + totalGenerated + " clases asignadas.");
            history.setExecutedAt(LocalDateTime.now());
            historyRepository.save(history);

            // Retornar DTO con información de cursos sin disponibilidad
            ScheduleHistoryDTO response = toDTO(history);
            response.setCoursesWithoutAvailability(result.getCoursesWithoutAvailability());
            response.setTotalCoursesWithoutAvailability(result.getCoursesWithoutAvailability().size());

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
     * Genera horario completo para un curso específico
     * Asigna clases distribuidas a lo largo de la semana
     */
    private GenerationResult generateCompleteScheduleForCourse(courses course, LocalDate startDate, LocalDate endDate) {
        logger.info("=== GENERANDO HORARIO COMPLETO PARA CURSO: {} ===", course.getCourseName());

        List<schedule> generatedSchedules = new ArrayList<>();
        List<CourseWithoutAvailabilityDTO> coursesWithoutAvailability = new ArrayList<>();
        int totalClassesAssigned = 0;

        teachers assignedTeacher = course.getTeacherSubject().getTeacher();
        subjects assignedSubject = course.getTeacherSubject().getSubject();

        logger.info("Profesor asignado: {} ({})", assignedTeacher.getTeacherName(), assignedTeacher.getId());
        logger.info("Materia: {} ({})", assignedSubject.getSubjectName(), assignedSubject.getId());

        // Validar configuración del profesor
        List<TeacherSubject> teacherAssociations = teacherSubjectRepo.findByTeacher_Id(assignedTeacher.getId());
        if (teacherAssociations.size() > 1) {
            String errorMsg = "ERROR DE CONFIGURACIÓN: El profesor " + assignedTeacher.getTeacherName() +
                " está asociado a múltiples materias. Cada profesor debe estar asociado únicamente a UNA materia.";
            logger.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }

        List<String> daysInPeriod = getUniqueDaysInPeriod(startDate, endDate);
        logger.info("Días disponibles para asignación: {}", daysInPeriod);

        // Estrategia: Asignar clases distribuidas a lo largo de la semana
        // Intentar asignar al menos 1 clase por día disponible
        for (String dayName : daysInPeriod) {
            logger.debug("Intentando asignar clase para {} el día {}", course.getCourseName(), dayName);

            // Encontrar slots disponibles para este profesor en este día
            List<TimeSlot> availableSlots = findAvailableSlotsForTeacher(assignedTeacher, dayName);

            if (availableSlots.isEmpty()) {
                logger.debug("No hay slots disponibles para {} el día {}", assignedTeacher.getTeacherName(), dayName);
                continue;
            }

            // Intentar asignar una clase en este día
            boolean classAssignedForDay = false;

            for (TimeSlot slot : availableSlots) {
                if (classAssignedForDay) break;

                logger.debug("Probando slot: {} - {}", slot.getStartTime(), slot.getEndTime());

                // Verificar que no haya conflicto
                if (!hasConflict(assignedTeacher, dayName, slot.getStartTime(), slot.getEndTime())) {
                    // Crear el horario
                    schedule newSchedule = new schedule();
                    newSchedule.setCourseId(course);
                    newSchedule.setTeacherId(assignedTeacher);
                    newSchedule.setSubjectId(assignedSubject);
                    newSchedule.setDay(dayName);
                    newSchedule.setStartTime(slot.getStartTime());
                    newSchedule.setEndTime(slot.getEndTime());
                    newSchedule.setScheduleName(course.getCourseName() + " - " + assignedSubject.getSubjectName());

                    generatedSchedules.add(newSchedule);
                    totalClassesAssigned++;
                    classAssignedForDay = true;

                    logger.info("✓ Clase asignada: {} {} {}-{} ({})",
                        course.getCourseName(), dayName, slot.getStartTime(), slot.getEndTime(), assignedTeacher.getTeacherName());
                    break;
                }
            }

            if (!classAssignedForDay) {
                logger.warn("No se pudo asignar clase para {} el día {}", course.getCourseName(), dayName);
            }
        }

        // Si no se pudo asignar ninguna clase, agregar a cursos sin disponibilidad
        if (totalClassesAssigned == 0) {
            CourseWithoutAvailabilityDTO unavailabilityInfo = analyzeCourseUnavailability(course, assignedTeacher, daysInPeriod);
            if (unavailabilityInfo != null) {
                coursesWithoutAvailability.add(unavailabilityInfo);
            }
        }

        logger.info("=== RESUMEN HORARIO COMPLETO ===");
        logger.info("Curso: {}", course.getCourseName());
        logger.info("Clases asignadas: {}", totalClassesAssigned);
        logger.info("Días con clases: {}", generatedSchedules.stream().map(s -> s.getDay()).distinct().count());

        if (!generatedSchedules.isEmpty()) {
            logger.info("Guardando horario completo en la base de datos...");
            scheduleRepo.saveAll(generatedSchedules);
            logger.info("Horario guardado exitosamente");
        }

        return new GenerationResult(totalClassesAssigned, coursesWithoutAvailability);
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