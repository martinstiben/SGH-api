package com.horarios.SGH.Service;

import com.horarios.SGH.DTO.InAppNotificationDTO;
import com.horarios.SGH.DTO.NotificationDTO;
import com.horarios.SGH.Model.NotificationPriority;
import com.horarios.SGH.Model.NotificationType;
import com.horarios.SGH.Model.schedule;
import java.util.logging.Logger;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio especializado para manejar notificaciones relacionadas con cambios en horarios.
 * Implementa el patrón de responsabilidad única separando la lógica de notificaciones
 * del servicio principal de horarios.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Service
public class ScheduleNotificationService {

    private final NotificationService notificationService;
    private final InAppNotificationService inAppNotificationService;
    private final usersService userService;
    
    /**
     * Logger para registro de eventos del servicio de notificaciones de horarios.
     */
    private static final Logger logger = Logger.getLogger(ScheduleNotificationService.class.getName());
    
    /**
     * Logger estático para compatibilidad con código existente.
     */
    private static final Logger log = logger;
    
    /**
     * Constructor manual para inyección de dependencias.
     * Mantiene compatibilidad con Spring y permite testing.
     *
     * @param notificationService Servicio de notificaciones
     * @param inAppNotificationService Servicio de notificaciones in-app
     * @param userService Servicio de usuarios
     */
    public ScheduleNotificationService(NotificationService notificationService,
                                      InAppNotificationService inAppNotificationService,
                                      usersService userService) {
        this.notificationService = notificationService;
        this.inAppNotificationService = inAppNotificationService;
        this.userService = userService;
    }

    /**
     * Envía notificaciones relacionadas con cambios en horarios.
     *
     * @param schedules lista de horarios afectados
     * @param action acción realizada (CREATED, UPDATED, DELETED)
     */
    public void sendScheduleNotifications(List<schedule> schedules, String action) {
        for (schedule s : schedules) {
            try {
                // Notificar al profesor sobre la asignación
                sendTeacherScheduleNotification(s, action);

                // Notificar a los estudiantes del curso sobre el cambio
                sendStudentsScheduleNotification(s, action);

            } catch (Exception e) {
                logger.warning("Error enviando notificación para horario " + s.getId() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Envía notificación al profesor sobre cambios en su horario.
     *
     * @param s horario afectado
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
            logger.warning("Error enviando notificación al profesor: " + e.getMessage());
        }
    }

    /**
     * Envía notificación a los coordinadores sobre cambios en el horario.
     *
     * @param s horario afectado
     * @param action acción realizada
     */
    private void sendStudentsScheduleNotification(schedule s, String action) {
        try {
            // Enviar notificación al coordinador sobre el cambio de horario
            List<com.horarios.SGH.Model.User> coordinators = userService.findUsersByRole("COORDINADOR");

            if (coordinators.isEmpty()) {
                logger.warning("No se encontraron coordinadores para enviar notificación");
                return;
            }

            // Enviar a todos los coordinadores
            for (com.horarios.SGH.Model.User coordinator : coordinators) {
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
            logger.warning("Error enviando notificación a coordinadores: " + e.getMessage());
        }
    }
}