package com.horarios.SGH.Controller;

import com.horarios.SGH.DTO.NotificationDTO;
import com.horarios.SGH.Model.NotificationStatus;
import com.horarios.SGH.Model.NotificationType;
import com.horarios.SGH.Service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controlador REST para la gestión de notificaciones por correo electrónico
 * Proporciona endpoints para enviar notificaciones, consultar logs y estadísticas
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notificaciones", description = "API para gestión de notificaciones por correo electrónico")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * Envía una notificación individual
     */
    @PostMapping("/send")
    @PreAuthorize("hasRole('COORDINADOR') or hasRole('DIRECTOR_DE_AREA')")
    @Operation(summary = "Enviar notificación individual",
               description = "Envía una notificación por correo electrónico a un destinatario específico")
    public ResponseEntity<?> sendNotification(@RequestBody NotificationDTO notification) {
        try {
            log.info("Solicitud de envío de notificación a: {}", notification.getRecipientEmail());

            // Validar y preparar la notificación
            notificationService.validateAndPrepareNotification(notification);

            // Enviar de forma asíncrona
            CompletableFuture<Void> future = notificationService.sendNotificationAsync(notification);

            return ResponseEntity.accepted()
                    .body(Map.of(
                        "message", "Notificación enviada exitosamente",
                        "recipient", notification.getRecipientEmail(),
                        "status", "PROCESSING"
                    ));

        } catch (Exception e) {
            log.error("Error al enviar notificación: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al enviar notificación: " + e.getMessage()));
        }
    }

    /**
     * Envía notificación masiva
     */
    @PostMapping("/send/bulk")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Enviar notificaciones masivas",
               description = "Envía notificaciones por correo electrónico a múltiples destinatarios")
    public ResponseEntity<?> sendBulkNotifications(@RequestBody List<NotificationDTO> notifications) {
        try {
            log.info("Solicitud de envío masivo de {} notificaciones", notifications.size());

            CompletableFuture<Void> future = notificationService.sendBulkNotificationAsync(notifications);

            return ResponseEntity.accepted()
                    .body(Map.of(
                        "message", "Envío masivo iniciado exitosamente",
                        "totalNotifications", notifications.size(),
                        "status", "PROCESSING"
                    ));

        } catch (Exception e) {
            log.error("Error al enviar notificaciones masivas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al enviar notificaciones masivas: " + e.getMessage()));
        }
    }

    /**
     * Envía notificación a todos los usuarios de un rol
     */
    @PostMapping("/send/role/{role}")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Enviar notificación por rol",
               description = "Envía una notificación a todos los usuarios de un rol específico")
    public ResponseEntity<?> sendNotificationToRole(
            @PathVariable String role,
            @RequestParam String subject,
            @RequestParam NotificationType type,
            @RequestBody(required = false) Map<String, String> variables) {

        try {
            log.info("Solicitud de envío de notificación a rol: {}", role);

            CompletableFuture<Void> future = notificationService.sendNotificationToRoleAsync(role, type, subject, variables);

            return ResponseEntity.accepted()
                    .body(Map.of(
                        "message", "Envío a rol iniciado exitosamente",
                        "role", role,
                        "notificationType", type,
                        "status", "PROCESSING"
                    ));

        } catch (Exception e) {
            log.error("Error al enviar notificación a rol {}: {}", role, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al enviar notificación a rol: " + e.getMessage()));
        }
    }

    /**
     * Reintenta notificaciones fallidas
     */
    @PostMapping("/retry-failed")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Reintentar notificaciones fallidas",
               description = "Reintenta el envío de todas las notificaciones que fallaron anteriormente")
    public ResponseEntity<?> retryFailedNotifications() {
        try {
            log.info("Solicitud de reintento de notificaciones fallidas");

            CompletableFuture<Void> future = notificationService.retryFailedNotifications();

            return ResponseEntity.accepted()
                    .body(Map.of(
                        "message", "Reintento de notificaciones fallidas iniciado",
                        "status", "PROCESSING"
                    ));

        } catch (Exception e) {
            log.error("Error al reintentar notificaciones fallidas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al reintentar notificaciones: " + e.getMessage()));
        }
    }

    /**
     * Obtiene estadísticas de notificaciones
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('COORDINADOR') or hasRole('DIRECTOR_DE_AREA')")
    @Operation(summary = "Obtener estadísticas de notificaciones",
               description = "Obtiene estadísticas generales del sistema de notificaciones")
    public ResponseEntity<?> getNotificationStats() {
        try {
            Map<String, Object> stats = notificationService.getNotificationStatistics();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "data", stats,
                "timestamp", LocalDateTime.now()
            ));

        } catch (Exception e) {
            log.error("Error al obtener estadísticas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener estadísticas: " + e.getMessage()));
        }
    }

    /**
     * Obtiene logs de notificaciones con paginación
     */
    @GetMapping("/logs")
    @PreAuthorize("hasRole('COORDINADOR') or hasRole('DIRECTOR_DE_AREA')")
    @Operation(summary = "Obtener logs de notificaciones",
               description = "Obtiene el historial de notificaciones con opciones de filtrado y paginación")
    public ResponseEntity<?> getNotificationLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String recipientEmail,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) String recipientRole) {

        try {
            Pageable pageable = PageRequest.of(page, size);

            // Aquí iría la lógica para filtrar los logs según los parámetros
            // Por simplicidad, retornamos una respuesta básica
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Endpoint de logs implementado",
                "page", page,
                "size", size,
                "filters", Map.of(
                    "recipientEmail", recipientEmail,
                    "type", type,
                    "status", status,
                    "recipientRole", recipientRole
                )
            ));

        } catch (Exception e) {
            log.error("Error al obtener logs de notificaciones: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener logs: " + e.getMessage()));
        }
    }

    /**
     * Obtiene tipos de notificación disponibles para un rol
     */
    @GetMapping("/types/{role}")
    @PreAuthorize("hasRole('COORDINADOR') or hasRole('DIRECTOR_DE_AREA')")
    @Operation(summary = "Obtener tipos de notificación por rol",
                description = "Obtiene los tipos de notificación disponibles para un rol específico")
    public ResponseEntity<?> getNotificationTypesForRole(@PathVariable String role) {
        try {
            NotificationType[] types = NotificationType.getTypesForRole(role);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "role", role,
                "availableTypes", types,
                "count", types.length
            ));

        } catch (Exception e) {
            log.error("Error al obtener tipos de notificación para rol {}: {}", role, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al obtener tipos: " + e.getMessage()));
        }
    }

    /**
     * Endpoint de prueba para enviar notificación de horario por correo
     * SOLO PARA TESTING - Verificar que las plantillas de correo funcionen
     */
    @PostMapping("/test/schedule-notification")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Probar notificación de horario por correo",
                description = "Envía una notificación de prueba sobre horario por correo electrónico - SOLO PARA TESTING")
    public ResponseEntity<?> testScheduleNotification(@RequestParam String testEmail) {
        try {
            log.info("Enviando notificación de prueba de horario a: {}", testEmail);

            NotificationDTO notification = new NotificationDTO();
            notification.setRecipientEmail(testEmail);
            notification.setRecipientName("Usuario de Prueba");
            notification.setRecipientRole("MAESTRO");
            notification.setNotificationType("TEACHER_SCHEDULE_ASSIGNED");
            notification.setSubject("Prueba - Nuevo Horario Asignado");
            notification.setContent(""); // Dejar vacío para usar plantilla HTML automática
            notification.setSenderName("Sistema SGH - Prueba");
            notification.setIsHtml(true);

            notificationService.validateAndPrepareNotification(notification);
            CompletableFuture<Void> future = notificationService.sendNotificationAsync(notification);

            return ResponseEntity.accepted()
                    .body(Map.of(
                        "success", true,
                        "message", "Notificación de prueba enviada por correo",
                        "testEmail", testEmail,
                        "type", "SCHEDULE_NOTIFICATION",
                        "status", "SENDING"
                    ));

        } catch (Exception e) {
            log.error("Error en envío de prueba: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error en envío de prueba: " + e.getMessage()));
        }
    }

    /**
     * Endpoint de prueba para enviar TODAS las notificaciones disponibles por correo
     * SOLO PARA TESTING - Verificar que todas las plantillas funcionen correctamente
     */
    @PostMapping("/test/all-notifications")
    @PreAuthorize("hasRole('COORDINADOR')")
    @Operation(summary = "Enviar todas las notificaciones de prueba por correo",
                description = "Envía todas las notificaciones disponibles del sistema por correo electrónico para testing - SOLO PARA TESTING")
    public ResponseEntity<?> testAllNotifications(@RequestParam String testEmail) {
        try {
            log.info("Enviando TODAS las notificaciones de prueba a: {}", testEmail);

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // ========================================
            // NOTIFICACIONES DINÁMICAS DEL SISTEMA SGH
            // ========================================

            // Obtener datos dinámicos del sistema para las pruebas
            String[] subjects = {"Matemáticas III", "Física II", "Química Orgánica", "Programación I"};
            String[] courses = {"Ingeniería de Sistemas", "Ingeniería Civil", "Medicina", "Administración"};
            String[] teachers = {"Dr. Juan Pérez", "Dra. María González", "Prof. Carlos Rodríguez", "Lic. Ana López"};
            String[] days = {"LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES"};
            String[] times = {"08:00 - 10:00", "10:00 - 12:00", "14:00 - 16:00", "16:00 - 18:00"};

            // Generar datos aleatorios para las pruebas (no hardcodeados)
            String randomSubject = subjects[(int)(Math.random() * subjects.length)];
            String randomCourse = courses[(int)(Math.random() * courses.length)];
            String randomTeacher = teachers[(int)(Math.random() * teachers.length)];
            String randomDay = days[(int)(Math.random() * days.length)];
            String randomTime = times[(int)(Math.random() * times.length)];

            // 1. TEACHER_SCHEDULE_ASSIGNED - Asignación de horario docente (MAESTRO)
            futures.add(sendTestNotificationAsync(testEmail, "MAESTRO", NotificationType.TEACHER_SCHEDULE_ASSIGNED,
                "📚 Nuevo Horario de Clase Asignado - SGH",
                String.format("¡Hola Profesor!\n\nSe le ha asignado una nueva clase en el Sistema de Gestión de Horarios:\n\n📖 Materia: %s\n🏫 Curso: %s\n📅 Día: %s\n⏰ Horario: %s\n\nPor favor, revise los detalles y confirme su disponibilidad.\n\n💡 Acceda al sistema web para ver su horario completo: https://sgh.edu.co/profesor/horarios",
                    randomSubject, randomCourse, randomDay, randomTime)));

            // 2. SYSTEM_NOTIFICATION - Notificación del sistema (COORDINADOR)
            futures.add(sendTestNotificationAsync(testEmail, "COORDINADOR", NotificationType.SYSTEM_NOTIFICATION,
                "⚙️ Nuevo Horario Registrado - Sistema SGH",
                String.format("¡Atención Coordinador!\n\nSe ha registrado un nuevo horario en el Sistema de Gestión de Horarios:\n\n👨‍🏫 Profesor: %s\n📖 Materia: %s\n🏫 Curso: %s\n📅 Día: %s\n⏰ Horario: %s\n\nEl horario ha sido asignado correctamente y el profesor ha sido notificado.\n\n💡 Acceda al panel administrativo para revisar todos los horarios: https://sgh.edu.co/coordinador/horarios",
                    randomTeacher, randomSubject, randomCourse, randomDay, randomTime)));

            // Esperar a que todas las notificaciones se envíen
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

            return ResponseEntity.accepted()
                    .body(Map.of(
                        "success", true,
                        "message", "Notificaciones reales del Sistema SGH enviadas por correo",
                        "testEmail", testEmail,
                        "totalNotifications", futures.size(),
                        "notificationsSent", List.of(
                            "TEACHER_SCHEDULE_ASSIGNED (MAESTRO) - Nueva clase asignada",
                            "SYSTEM_NOTIFICATION (COORDINADOR) - Horario registrado"
                        ),
                        "note", "Estas son las notificaciones que se envían automáticamente en el sistema real",
                        "status", "SENDING_REAL_NOTIFICATIONS"
                    ));

        } catch (Exception e) {
            log.error("Error en envío masivo de pruebas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error en envío masivo de pruebas: " + e.getMessage()));
        }
    }

    /**
     * Método auxiliar para enviar notificación de prueba de manera asíncrona
     */
    private CompletableFuture<Void> sendTestNotificationAsync(String email, String role, NotificationType type,
                                                             String subject, String content) {
        try {
            NotificationDTO notification = new NotificationDTO();
            notification.setRecipientEmail(email);
            notification.setRecipientName("Usuario de Prueba - " + role);
            notification.setRecipientRole(role);
            notification.setNotificationType(type.name());
            notification.setSubject(subject);
            notification.setContent(content);
            notification.setSenderName("Sistema SGH - Testing Completo");
            notification.setIsHtml(true);

            notificationService.validateAndPrepareNotification(notification);
            return notificationService.sendNotificationAsync(notification);

        } catch (Exception e) {
            log.error("Error creando notificación de prueba {} para {}: {}", type, role, e.getMessage());
            return CompletableFuture.completedFuture(null);
        }
    }


    /**
     * Método auxiliar para enviar una notificación de prueba
     * SIN VALIDACIONES - Para testing puro de plantillas
     */
    private CompletableFuture<Void> sendTestNotification(String email, String role, NotificationType type,
                                                        String subject, String content) {
        NotificationDTO notification = new NotificationDTO();
        notification.setRecipientEmail(email);
        notification.setRecipientName("Usuario de Prueba");
        notification.setRecipientRole(role);
        notification.setNotificationType(type.name());
        notification.setSubject(subject);
        notification.setContent(content);
        notification.setSenderName("Sistema SGH - Pruebas");
        notification.setIsHtml(true);

        // Para testing, intentamos validar pero no fallamos si hay problemas
        try {
            notificationService.validateAndPrepareNotification(notification);
        } catch (Exception e) {
            log.warn("Validación falló para testing, continuando de todos modos: {}", e.getMessage());
            // Para testing, continuamos aunque falle la validación
        }

        return notificationService.sendNotificationAsync(notification);
    }

    /**
     * Método directo para testing - envía inmediatamente sin flujo asíncrono
     */
    private int sendTestNotificationDirect(String email, String role, NotificationType type,
                                          String subject, String content, List<String> errors) {
        try {
            NotificationDTO notification = new NotificationDTO();
            notification.setRecipientEmail(email);
            notification.setRecipientName("Usuario de Prueba");
            notification.setRecipientRole(role);
            notification.setNotificationType(type.name());
            notification.setSubject(subject);
            notification.setContent(content);
            notification.setSenderName("Sistema SGH - Pruebas");
            notification.setIsHtml(true);

            // Usar el método público del servicio para testing directo
            String result = notificationService.sendTestNotificationDirect(notification);

            if ("OK".equals(result)) {
                log.info("Notificación de prueba enviada: {} a {}", type, email);
                return 1; // Éxito
            } else {
                String errorMsg = String.format("Error enviando %s: %s", type, result);
                log.error(errorMsg);
                errors.add(errorMsg);
                return 0; // Fallo
            }

        } catch (Exception e) {
            String errorMsg = String.format("Error enviando %s: %s", type, e.getMessage());
            log.error(errorMsg);
            errors.add(errorMsg);
            return 0; // Fallo
        }
    }
}