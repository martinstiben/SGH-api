package com.horarios.SGH.Service;

import com.horarios.SGH.DTO.ScheduleDTO;
import com.horarios.SGH.Model.Days;
import com.horarios.SGH.Model.TeacherAvailability;
import com.horarios.SGH.Model.schedule;
import com.horarios.SGH.Model.courses;
import com.horarios.SGH.Model.subjects;
import com.horarios.SGH.Model.teachers;
import com.horarios.SGH.Model.TeacherSubject;
import com.horarios.SGH.Model.User;
import com.horarios.SGH.Repository.IScheduleRepository;
import com.horarios.SGH.Repository.ITeacherAvailabilityRepository;
import com.horarios.SGH.Repository.Icourses;
import com.horarios.SGH.Repository.Iteachers;
import com.horarios.SGH.Repository.Isubjects;
import com.horarios.SGH.Repository.IUserRepository;
import com.horarios.SGH.Repository.TeacherSubjectRepository;
import com.horarios.SGH.DTO.NotificationDTO;
import com.horarios.SGH.Model.NotificationType;
import com.horarios.SGH.DTO.InAppNotificationDTO;
import com.horarios.SGH.Model.NotificationPriority;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de horarios académicos con funcionalidades completas
 * de creación, actualización, eliminación y notificaciones.
 * 
 * @author Sistema SGH
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private static final Logger logger = Logger.getLogger(ScheduleService.class.getName());

    private final IScheduleRepository scheduleRepo;
    private final ITeacherAvailabilityRepository availabilityRepo;
    private final Icourses courseRepo;
    private final Iteachers teacherRepo;
    private final Isubjects subjectRepo;
    private final IUserRepository userRepo;
    private final TeacherSubjectRepository teacherSubjectRepo;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private InAppNotificationService inAppNotificationService;

    @Autowired
    private usersService userService;

    /**
     * Verifica si un profesor está disponible en el horario especificado.
     * 
     * @param teacherId ID del profesor
     * @param day día de la semana
     * @param start hora de inicio
     * @param end hora de fin
     * @return true si el profesor está disponible
     */
    private boolean isTeacherAvailable(Integer teacherId, String day, LocalTime start, LocalTime end) {
        try {
            Days dayEnum = Days.valueOf(day);
            List<TeacherAvailability> disponibilidad = availabilityRepo.findByTeacher_IdAndDay(teacherId, dayEnum);
            return disponibilidad.stream().anyMatch(d -> {
                // Verificar si el horario solicitado está cubierto por AM o PM
                boolean coveredByAM = d.getAmStart() != null && d.getAmEnd() != null &&
                        !start.isBefore(d.getAmStart()) && !end.isAfter(d.getAmEnd());
                boolean coveredByPM = d.getPmStart() != null && d.getPmEnd() != null &&
                        !start.isBefore(d.getPmStart()) && !end.isAfter(d.getPmEnd());
                return coveredByAM || coveredByPM;
            });
        } catch (IllegalArgumentException e) {
            // Día no válido (ej. Sábado o Domingo)
            return false;
        }
    }

    /**
     * Crea un nuevo horario o múltiples horarios.
     * 
     * @param assignments lista de asignaciones de horarios
     * @param executedBy usuario que ejecuta la acción
     * @return lista de DTOs de horarios creados
     */
    @Transactional
    public List<ScheduleDTO> createSchedule(List<ScheduleDTO> assignments, String executedBy) {
        if (assignments == null || assignments.isEmpty()) {
            throw new IllegalArgumentException("La lista de asignaciones no puede estar vacía");
        }

        List<schedule> entities = new ArrayList<>();

        for (ScheduleDTO dto : assignments) {
            courses course = courseRepo.findById(dto.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado con ID: " + dto.getCourseId()));

            teachers teacher;
            subjects subject;

            // VALIDACIÓN: Si se especifica teacherId, subjectId es obligatorio y viceversa
            if (dto.getTeacherId() != null && dto.getSubjectId() == null) {
                throw new RuntimeException("Si especificas teacherId, también debes especificar subjectId.");
            }
            if (dto.getSubjectId() != null && dto.getTeacherId() == null) {
                throw new RuntimeException("Si especificas subjectId, también debes especificar teacherId.");
            }

            // Si se especifica teacherId y subjectId, usar esos valores
            if (dto.getTeacherId() != null && dto.getSubjectId() != null) {
                teacher = teacherRepo.findById(dto.getTeacherId())
                    .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado con ID: " + dto.getTeacherId()));
                subject = subjectRepo.findById(dto.getSubjectId())
                    .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada con ID: " + dto.getSubjectId()));

                // VALIDACIÓN: Un profesor solo puede estar asociado a UNA materia
                List<TeacherSubject> teacherAssociations = teacherSubjectRepo.findByTeacher_Id(teacher.getId());
                if (teacherAssociations.size() > 1) {
                    throw new RuntimeException("El profesor " + teacher.getTeacherName() +
                        " está asociado a múltiples materias. Cada profesor debe estar asociado únicamente a una materia.");
                }

                // Validar que el profesor esté vinculado específicamente a esta materia
                boolean isLinkedToSubject = teacherSubjectRepo.existsByTeacher_IdAndSubject_Id(teacher.getId(), subject.getId());
                if (!isLinkedToSubject) {
                    throw new RuntimeException("El profesor " + teacher.getTeacherName() +
                        " no está vinculado a la materia " + subject.getSubjectName() +
                        ". Debe existir una relación TeacherSubject entre ellos.");
                }
            } else {
                // Si no se especifica profesor/materia, es un error
                throw new RuntimeException("Debes especificar tanto teacherId como subjectId para crear el horario.");
            }

            if (!isTeacherAvailable(teacher.getId(), dto.getDay(), dto.getStartTimeAsLocalTime(), dto.getEndTimeAsLocalTime())) {
                throw new RuntimeException("El profesor " + teacher.getTeacherName() + " no está disponible el " + dto.getDay());
            }

            schedule s = toEntity(dto);
            entities.add(s);
        }

        List<schedule> saved = scheduleRepo.saveAll(entities);
        
        // Enviar notificaciones
        sendScheduleNotifications(saved, "CREATED");
        
        logger.log(Level.INFO, "Se crearon {0} horarios por el usuario: {1}", 
                  new Object[]{saved.size(), executedBy});

        return saved.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Obtiene horarios por nombre.
     * 
     * @param scheduleName nombre del horario
     * @return lista de DTOs de horarios
     */
    @Transactional(readOnly = true)
    public List<ScheduleDTO> getByName(String scheduleName) {
        return scheduleRepo.findByScheduleName(scheduleName)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene horarios por curso.
     * 
     * @param courseId ID del curso
     * @return lista de DTOs de horarios
     */
    public List<ScheduleDTO> getByCourse(Integer courseId) {
        return scheduleRepo.findByCourseId(courseId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Obtiene horarios por profesor.
     * 
     * @param teacherId ID del profesor
     * @return lista de DTOs de horarios
     */
    public List<ScheduleDTO> getByTeacher(Integer teacherId) {
        return scheduleRepo.findByTeacherId(teacherId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Obtiene todos los horarios.
     * 
     * @return lista de todos los DTOs de horarios
     */
    public List<ScheduleDTO> getAll() {
        return scheduleRepo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Obtiene horarios por email de estudiante.
     * 
     * @param email email del estudiante
     * @return lista de DTOs de horarios del curso del estudiante
     */
    public List<ScheduleDTO> getByStudentEmail(String email) {
        User student = userRepo.findByUserName(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (student.getCourse() == null) {
            throw new RuntimeException("El estudiante no tiene un curso asignado");
        }

        return getByCourse(student.getCourse().getId());
    }

    /**
     * Obtiene usuario por email.
     * 
     * @param email email del usuario
     * @return usuario encontrado
     */
    public User getUserByEmail(String email) {
        return userRepo.findByUserName(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    /**
     * Actualiza un horario existente.
     * 
     * @param id ID del horario
     * @param dto nuevos datos del horario
     * @param executedBy usuario que ejecuta la acción
     * @return DTO del horario actualizado
     */
    @Transactional
    public ScheduleDTO updateSchedule(Integer id, ScheduleDTO dto, String executedBy) {
        logger.log(Level.INFO, "Actualizando horario ID: {0} por usuario: {1}", new Object[]{id, executedBy});
        
        schedule existing = scheduleRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Horario no encontrado"));

        courses course = courseRepo.findById(dto.getCourseId())
            .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado con ID: " + dto.getCourseId()));

        teachers teacher;
        subjects subject;

        // VALIDACIÓN: Si se especifica teacherId, subjectId es obligatorio y viceversa
        if (dto.getTeacherId() != null && dto.getSubjectId() == null) {
            throw new RuntimeException("Si especificas teacherId, también debes especificar subjectId.");
        }
        if (dto.getSubjectId() != null && dto.getTeacherId() == null) {
            throw new RuntimeException("Si especificas subjectId, también debes especificar teacherId.");
        }

        // Si se especifica teacherId y subjectId, usar esos valores
        if (dto.getTeacherId() != null && dto.getSubjectId() != null) {
            teacher = teacherRepo.findById(dto.getTeacherId())
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado con ID: " + dto.getTeacherId()));
            subject = subjectRepo.findById(dto.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada con ID: " + dto.getSubjectId()));

            // VALIDACIÓN: Un profesor solo puede estar asociado a UNA materia
            List<TeacherSubject> teacherAssociations = teacherSubjectRepo.findByTeacher_Id(teacher.getId());
            if (teacherAssociations.size() > 1) {
                throw new RuntimeException("El profesor " + teacher.getTeacherName() +
                    " está asociado a múltiples materias. Cada profesor debe estar asociado únicamente a una materia.");
            }

            // Validar que el profesor esté vinculado específicamente a esta materia
            boolean isLinkedToSubject = teacherSubjectRepo.existsByTeacher_IdAndSubject_Id(teacher.getId(), subject.getId());
            if (!isLinkedToSubject) {
                throw new RuntimeException("El profesor " + teacher.getTeacherName() +
                    " no está vinculado a la materia " + subject.getSubjectName() +
                    ". Debe existir una relación TeacherSubject entre ellos.");
            }
        } else {
            // Si no se especifica profesor/materia, es un error
            throw new RuntimeException("Debes especificar tanto teacherId como subjectId para actualizar el horario.");
        }

        if (!isTeacherAvailable(teacher.getId(), dto.getDay(), dto.getStartTimeAsLocalTime(), dto.getEndTimeAsLocalTime())) {
            throw new RuntimeException("El profesor " + teacher.getTeacherName() + " no está disponible el " + dto.getDay());
        }

        // Actualizar la entidad existente
        existing.setCourseId(course);
        existing.setTeacherId(teacher);
        existing.setSubjectId(subject);
        existing.setDay(Days.valueOf(dto.getDay()));
        existing.setStartTime(dto.getStartTimeAsLocalTime());
        existing.setEndTime(dto.getEndTimeAsLocalTime());
        existing.setScheduleName(dto.getScheduleName());

        schedule saved = scheduleRepo.save(existing);
        
        // Enviar notificaciones
        List<schedule> updatedList = new ArrayList<>();
        updatedList.add(saved);
        sendScheduleNotifications(updatedList, "UPDATED");
        
        logger.log(Level.INFO, "Horario ID: {0} actualizado exitosamente", id);

        return toDTO(saved);
    }

    /**
     * Elimina un horario.
     * 
     * @param id ID del horario
     * @param executedBy usuario que ejecuta la acción
     */
    @Transactional
    public void deleteSchedule(Integer id, String executedBy) {
        if (!scheduleRepo.existsById(id)) {
            throw new RuntimeException("Horario no encontrado");
        }
        scheduleRepo.deleteById(id);
        logger.log(Level.INFO, "Horario ID: {0} eliminado por usuario: {1}", new Object[]{id, executedBy});
    }

    /**
     * Elimina horarios por día.
     * 
     * @param day día a eliminar
     */
    @Transactional
    public void deleteByDay(String day) {
        scheduleRepo.deleteByDay(day);
    }

    /**
     * Elimina todos los horarios.
     */
    @Transactional
    public void deleteAllSchedules() {
        scheduleRepo.deleteAll();
    }

    /**
     * Convierte DTO a entidad.
     * 
     * @param dto DTO a convertir
     * @return entidad schedule
     */
    private schedule toEntity(ScheduleDTO dto) {
        schedule s = new schedule();
        s.setId(dto.getId());
        s.setCourseId(courseRepo.findById(dto.getCourseId()).orElseThrow());
        s.setTeacherId(teacherRepo.findById(dto.getTeacherId()).orElseThrow());
        s.setSubjectId(subjectRepo.findById(dto.getSubjectId()).orElseThrow());
        s.setDay(Days.valueOf(dto.getDay()));
        s.setStartTime(dto.getStartTimeAsLocalTime());
        s.setEndTime(dto.getEndTimeAsLocalTime());
        s.setScheduleName(dto.getScheduleName());
        return s;
    }

    /**
     * Convierte entidad a DTO.
     * 
     * @param s entidad a convertir
     * @return DTO
     */
    private ScheduleDTO toDTO(schedule s) {
        ScheduleDTO dto = new ScheduleDTO();
        dto.setId(s.getId());
        dto.setCourseId(s.getCourseId().getId());
        dto.setTeacherId(s.getTeacherId().getId());
        dto.setSubjectId(s.getSubjectId().getId());
        dto.setDay(s.getDay().name());
        dto.setStartTimeFromLocalTime(s.getStartTime());
        dto.setEndTimeFromLocalTime(s.getEndTime());
        dto.setScheduleName(s.getScheduleName());
        dto.setTeacherName(s.getTeacherId().getTeacherName());
        dto.setSubjectName(s.getSubjectId().getSubjectName());

        return dto;
    }

    /**
     * Envía notificaciones relacionadas con cambios en horarios.
     * 
     * @param schedules lista de horarios
     * @param action acción realizada (CREATED, UPDATED, DELETED)
     */
    private void sendScheduleNotifications(List<schedule> schedules, String action) {
        for (schedule s : schedules) {
            try {
                // Notificar al profesor sobre la asignación
                sendTeacherScheduleNotification(s, action);

                // Notificar a los estudiantes del curso sobre el cambio
                sendStudentsScheduleNotification(s, action);

            } catch (Exception e) {
                logger.log(Level.WARNING, "Error enviando notificación para horario " + s.getId() + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * Envía notificación al profesor sobre cambios en su horario.
     * 
     * @param s horario
     * @param action acción realizada
     */
    private void sendTeacherScheduleNotification(schedule s, String action) {
        try {
            // Asumir que teacher.getId() es el userId
            Long teacherUserId = (long) s.getTeacherId().getId();

            // ===========================================
            // 1. ENVIAR NOTIFICACIÓN IN-APP
            // ===========================================
            InAppNotificationDTO inAppNotification = new InAppNotificationDTO();
            inAppNotification.setUserId(teacherUserId);
            inAppNotification.setNotificationType(NotificationType.TEACHER_SCHEDULE_ASSIGNED.name());
            inAppNotification.setPriority(NotificationPriority.MEDIUM.name());
            inAppNotification.setCategory("SCHEDULE");

            String title;
            String message;

            if ("CREATED".equals(action)) {
                title = "Nuevo Horario Asignado";
                message = String.format(
                    "Se te ha asignado un horario de clase.\n\n" +
                    "Materia: %s\n" +
                    "Curso: %s\n" +
                    "Día: %s\n" +
                    "Horario: %s - %s",
                    s.getSubjectId().getSubjectName(),
                    s.getCourseId().getCourseName(),
                    s.getDay(),
                    s.getStartTime().toString(),
                    s.getEndTime().toString()
                );
            } else {
                title = "Horario Modificado";
                message = String.format(
                    "Se ha modificado tu horario de clase.\n\n" +
                    "Materia: %s\n" +
                    "Curso: %s\n" +
                    "Día: %s\n" +
                    "Horario: %s - %s",
                    s.getSubjectId().getSubjectName(),
                    s.getCourseId().getCourseName(),
                    s.getDay(),
                    s.getStartTime().toString(),
                    s.getEndTime().toString()
                );
            }

            inAppNotification.setTitle(title);
            inAppNotification.setMessage(message);
            inAppNotification.setIcon("📚");

            inAppNotificationService.sendInAppNotificationAsync(inAppNotification);

            // ===========================================
            // 2. ENVIAR NOTIFICACIÓN POR EMAIL
            // ===========================================
            NotificationDTO emailNotification = new NotificationDTO();
            emailNotification.setRecipientEmail("profesor" + teacherUserId + "@sgh.edu"); // Placeholder - debería ser email real
            emailNotification.setRecipientName(s.getTeacherId().getTeacherName());
            emailNotification.setRecipientRole("MAESTRO");
            emailNotification.setNotificationType(NotificationType.TEACHER_SCHEDULE_ASSIGNED.name());
            emailNotification.setSubject(title);
            emailNotification.setContent(message);
            emailNotification.setSenderName("Sistema SGH");
            emailNotification.setIsHtml(true);

            notificationService.validateAndPrepareNotification(emailNotification);
            notificationService.sendNotificationAsync(emailNotification);

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error enviando notificación al profesor: " + e.getMessage(), e);
        }
    }

    /**
     * Envía notificación a los coordinadores sobre cambios en el horario.
     * 
     * @param s horario
     * @param action acción realizada
     */
    private void sendStudentsScheduleNotification(schedule s, String action) {
        try {
            // Enviar notificación al coordinador sobre el cambio de horario
            List<User> coordinators = userService.findUsersByRole("COORDINADOR");

            if (coordinators.isEmpty()) {
                logger.log(Level.WARNING, "No se encontraron coordinadores para enviar notificación");
                return;
            }

            // Enviar a todos los coordinadores
            for (User coordinator : coordinators) {
                // ===========================================
                // 1. ENVIAR NOTIFICACIÓN IN-APP
                // ===========================================
                InAppNotificationDTO inAppNotification = new InAppNotificationDTO();
                inAppNotification.setUserId(coordinator.getUserId());
                inAppNotification.setNotificationType(NotificationType.SYSTEM_NOTIFICATION.name());
                inAppNotification.setPriority(NotificationPriority.MEDIUM.name());
                inAppNotification.setCategory("SCHEDULE");

                String title;
                String message;

                if ("CREATED".equals(action)) {
                    title = "Nuevo Horario Registrado";
                    message = String.format(
                        "Se ha registrado un nuevo horario en el sistema.\n\n" +
                        "Profesor: %s\n" +
                        "Materia: %s\n" +
                        "Curso: %s\n" +
                        "Día: %s\n" +
                        "Horario: %s - %s",
                        s.getTeacherId().getTeacherName(),
                        s.getSubjectId().getSubjectName(),
                        s.getCourseId().getCourseName(),
                        s.getDay(),
                        s.getStartTime().toString(),
                        s.getEndTime().toString()
                    );
                } else {
                    title = "Horario Modificado";
                    message = String.format(
                        "Se ha modificado un horario en el sistema.\n\n" +
                        "Profesor: %s\n" +
                        "Materia: %s\n" +
                        "Curso: %s\n" +
                        "Día: %s\n" +
                        "Horario: %s - %s",
                        s.getTeacherId().getTeacherName(),
                        s.getSubjectId().getSubjectName(),
                        s.getCourseId().getCourseName(),
                        s.getDay(),
                        s.getStartTime().toString(),
                        s.getEndTime().toString()
                    );
                }

                inAppNotification.setTitle(title);
                inAppNotification.setMessage(message);
                inAppNotification.setIcon("⚙️");

                inAppNotificationService.sendInAppNotificationAsync(inAppNotification);

                // ===========================================
                // 2. ENVIAR NOTIFICACIÓN POR EMAIL
                // ===========================================
                NotificationDTO emailNotification = new NotificationDTO();
                emailNotification.setRecipientEmail(coordinator.getPerson().getEmail()); // Email real del coordinador
                emailNotification.setRecipientName(coordinator.getPerson().getFullName());
                emailNotification.setRecipientRole("COORDINADOR");
                emailNotification.setNotificationType(NotificationType.SYSTEM_NOTIFICATION.name());
                emailNotification.setSubject(title);
                emailNotification.setContent(message);
                emailNotification.setSenderName("Sistema SGH");
                emailNotification.setIsHtml(true);

                notificationService.validateAndPrepareNotification(emailNotification);
                notificationService.sendNotificationAsync(emailNotification);
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error enviando notificación a coordinadores: " + e.getMessage(), e);
        }
    }
}