package com.horarios.SGH.Service;

import com.horarios.SGH.DTO.ScheduleHistoryDTO;
import com.horarios.SGH.DTO.CourseWithoutAvailabilityDTO;
import com.horarios.SGH.Model.*;
import com.horarios.SGH.Model.schedule;
import com.horarios.SGH.Repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleGenerationService {

    private final IScheduleHistory historyRepository;
    private final Icourses courseRepo;
    private final Iteachers teacherRepo;
    private final ITeacherAvailabilityRepository availabilityRepo;
    private final IScheduleRepository scheduleRepo;
    private final Isubjects subjectRepo;
    private final TeacherSubjectRepository teacherSubjectRepo;

    @Transactional
    public ScheduleHistoryDTO generate(ScheduleHistoryDTO request, String executedBy) {
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
            if (days < 0) throw new IllegalArgumentException("El rango de fechas es inválido");

            int totalGenerated = 0;
            List<CourseWithoutAvailabilityDTO> coursesWithoutAvailability = new ArrayList<>();

            if (!request.isDryRun()) {
                // Real schedule generation for courses
                GenerationResult result = generateSchedulesForPeriod(request.getPeriodStart(), request.getPeriodEnd(), request.getParams());
                totalGenerated = result.getTotalGenerated();
                coursesWithoutAvailability = result.getCoursesWithoutAvailability();
            } else {
                // Simulation: count courses without assigned schedule
                List<courses> coursesWithoutSchedule = getCoursesWithoutSchedule();
                totalGenerated = coursesWithoutSchedule.size();
                
                // En modo simulación, también detectar cursos sin disponibilidad
                SimulationResult simulationResult = analyzeCoursesWithoutAvailability(request.getPeriodStart(), request.getPeriodEnd());
                coursesWithoutAvailability = simulationResult.getCoursesWithoutAvailability();
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
            
            return response;
        } catch (Exception ex) {
            history.setStatus("FAILED");
            history.setMessage(ex.getMessage() != null ? ex.getMessage() : "Error en la generación");
            history.setExecutedAt(LocalDateTime.now());
            historyRepository.save(history);
            
            ScheduleHistoryDTO response = toDTO(history);
            response.setCoursesWithoutAvailability(new ArrayList<>());
            response.setTotalCoursesWithoutAvailability(0);
            
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
        List<courses> coursesWithoutSchedule = getCoursesWithoutSchedule();
        List<schedule> generatedSchedules = new ArrayList<>();
        List<CourseWithoutAvailabilityDTO> coursesWithoutAvailability = new ArrayList<>();
        int totalGenerated = 0;

        List<String> daysInPeriod = getUniqueDaysInPeriod(startDate, endDate);

        for (courses course : coursesWithoutSchedule) {
            if (course.getTeacherSubject() == null) {
                // Curso sin profesor/materia asignada
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

            // VALIDACIÓN CRÍTICA: Un profesor solo puede estar asociado a UNA materia
            List<TeacherSubject> teacherAssociations = teacherSubjectRepo.findByTeacher_Id(assignedTeacher.getId());
            if (teacherAssociations.size() > 1) {
                throw new RuntimeException("ERROR DE CONFIGURACIÓN: El profesor " + assignedTeacher.getTeacherName() +
                    " está asociado a múltiples materias (" + teacherAssociations.size() + "). " +
                    "Cada profesor debe estar asociado únicamente a UNA materia.");
            }

            boolean courseAssigned = false;

            // Try to assign the course ONLY to the assigned teacher (no alternatives)
            for (String dayName : daysInPeriod) {
                if (courseAssigned) break;

                if (tryAssignCourseToTeacher(course, assignedTeacher, dayName, generatedSchedules)) {
                    courseAssigned = true;
                    totalGenerated++;
                    break;
                }
            }

            // Si el curso no pudo ser asignado, analizar el motivo
            if (!courseAssigned) {
                CourseWithoutAvailabilityDTO unavailabilityInfo = analyzeCourseUnavailability(course, assignedTeacher, daysInPeriod);
                if (unavailabilityInfo != null) {
                    coursesWithoutAvailability.add(unavailabilityInfo);
                }
            }
        }

        if (!generatedSchedules.isEmpty()) {
            scheduleRepo.saveAll(generatedSchedules);
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
        // Verificar si el profesor tiene disponibilidad definida para algún día
        boolean hasAnyAvailability = false;
        List<String> daysWithoutAvailability = new ArrayList<>();

        for (String dayName : daysInPeriod) {
            List<TeacherAvailability> availabilities = availabilityRepo.findByTeacher_IdAndDay(teacher.getId(), Days.valueOf(dayName));
            if (availabilities.isEmpty() || !availabilities.stream().anyMatch(TeacherAvailability::hasValidSchedule)) {
                daysWithoutAvailability.add(dayName);
            } else {
                hasAnyAvailability = true;
            }
        }

        if (!hasAnyAvailability) {
            return new CourseWithoutAvailabilityDTO(
                course.getId(),
                course.getCourseName(),
                teacher.getId(),
                teacher.getTeacherName(),
                "NO_AVAILABILITY_DEFINED",
                "El profesor " + teacher.getTeacherName() + " no tiene disponibilidad configurada para ningún día: " + 
                String.join(", ", daysWithoutAvailability)
            );
        }

        // Verificar conflictos con horarios existentes
        for (String dayName : daysInPeriod) {
            List<schedule> existingSchedules = scheduleRepo.findByTeacherId(teacher.getId());
            List<schedule> daySchedules = existingSchedules.stream()
                .filter(s -> dayName.equals(s.getDay()))
                .collect(Collectors.toList());

            if (!daySchedules.isEmpty()) {
                return new CourseWithoutAvailabilityDTO(
                    course.getId(),
                    course.getCourseName(),
                    teacher.getId(),
                    teacher.getTeacherName(),
                    "CONFLICTS_WITH_EXISTING",
                    "El profesor " + teacher.getTeacherName() + " tiene conflictos de horario existentes el día " + dayName
                );
            }
        }

        // Si llegamos aquí, no se encontró disponibilidad por otros motivos
        return new CourseWithoutAvailabilityDTO(
            course.getId(),
            course.getCourseName(),
            teacher.getId(),
            teacher.getTeacherName(),
            "NO_TIME_SLOTS_AVAILABLE",
            "No se encontraron espacios de tiempo disponibles para el profesor " + teacher.getTeacherName() + 
            " en los días: " + String.join(", ", daysInPeriod)
        );
    }

    private List<courses> getCoursesWithoutSchedule() {
        List<courses> allCourses = courseRepo.findAll();
        return allCourses.stream()
            .filter(course -> {
                List<schedule> schedules = scheduleRepo.findByCourseId(course.getId());
                return schedules.isEmpty(); // Solo cursos sin horario asignado
            })
            .collect(Collectors.toList());
    }

    private boolean tryAssignCourseToTeacher(courses course, teachers teacher, String dayName, List<schedule> generatedSchedules) {
        // Verificar disponibilidad del profesor para este día
        List<TeacherAvailability> availabilities = availabilityRepo.findByTeacher_IdAndDay(teacher.getId(), Days.valueOf(dayName));

        for (TeacherAvailability availability : availabilities) {
            if (availability.hasValidSchedule()) {
                // Generar slots disponibles para este profesor en este día
                List<schedule> availableSlots = generateAvailableTimeSlotsForCourse(course, teacher, availability, dayName);

                // Filtrar slots que no tengan conflictos
                List<schedule> conflictFreeSlots = filterSlotsWithoutConflicts(availableSlots, teacher.getId(), dayName);

                if (!conflictFreeSlots.isEmpty()) {
                    // Asignar el primer slot disponible
                    schedule assignedSlot = conflictFreeSlots.get(0);
                    generatedSchedules.add(assignedSlot);
                    return true;
                }
            }
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
            default -> null; // No generar para fines de semana
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

        return days;
    }

    private List<schedule> generateAvailableTimeSlotsForCourse(courses course, teachers teacher, TeacherAvailability availability, String dayName) {
        List<schedule> slots = new ArrayList<>();
        subjects subject = course.getTeacherSubject().getSubject();
        String scheduleName = course.getCourseName() + " - " + subject.getSubjectName();

        // Generar slots para mañana
        if (availability.getAmStart() != null && availability.getAmEnd() != null) {
            slots.addAll(generateSlotsInPeriodForCourse(course, teacher, availability.getAmStart(), availability.getAmEnd(),
                                                      dayName, scheduleName));
        }

        // Generar slots para tarde
        if (availability.getPmStart() != null && availability.getPmEnd() != null) {
            slots.addAll(generateSlotsInPeriodForCourse(course, teacher, availability.getPmStart(), availability.getPmEnd(),
                                                      dayName, scheduleName));
        }

        return slots;
    }

    private List<schedule> generateSlotsInPeriodForCourse(courses course, teachers teacher, LocalTime start, LocalTime end,
                                                       String day, String scheduleName) {
        List<schedule> slots = new ArrayList<>();
        LocalTime currentTime = start;

        // Asumir clases de 1 hora (puedes ajustar según necesidad)
        while (currentTime.isBefore(end)) {
            LocalTime slotEnd = currentTime.plusHours(1);
            if (slotEnd.isAfter(end)) break;

            schedule slot = new schedule();
            slot.setCourseId(course);
            slot.setDay(day);
            slot.setStartTime(currentTime);
            slot.setEndTime(slotEnd);
            slot.setScheduleName(scheduleName);

            slots.add(slot);
            currentTime = slotEnd;
        }

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

        // Filtrar por día y verificar conflictos de horario
        return existingSchedules.stream()
            .filter(existing -> dayName.equals(existing.getDay())) // Solo del mismo día
            .anyMatch(existing ->
                // Verificar si los horarios se solapan
                (slot.getStartTime().isBefore(existing.getEndTime()) &&
                 slot.getEndTime().isAfter(existing.getStartTime()))
            );
    }

    private String buildSuccessMessage(int totalGenerated, int totalWithoutAvailability) {
        if (totalWithoutAvailability == 0) {
            return "Generación completada exitosamente. " + totalGenerated + " horarios generados.";
        } else {
            return String.format("Generación completada. %d horarios generados, %d cursos sin disponibilidad de profesores.", 
                totalGenerated, totalWithoutAvailability);
        }
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
}