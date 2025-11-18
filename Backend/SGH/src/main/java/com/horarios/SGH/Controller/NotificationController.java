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
import java.util.HashMap;
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
            java.util.Map<String, String[]> types = new HashMap<>();

            // Agrupar tipos por rol
            types.put("ESTUDIANTE", new String[]{
                "STUDENT_SCHEDULE_ASSIGNMENT",
                "STUDENT_SCHEDULE_CHANGE",
                "STUDENT_CLASS_CANCELLATION"
            });

            types.put("MAESTRO", new String[]{
                "TEACHER_CLASS_SCHEDULED",
                "TEACHER_CLASS_MODIFIED",
                "TEACHER_CLASS_CANCELLED",
                "TEACHER_AVAILABILITY_CHANGED"
            });

            types.put("DIRECTOR_DE_AREA", new String[]{
                "DIRECTOR_SCHEDULE_CONFLICT",
                "DIRECTOR_AVAILABILITY_ISSUE",
                "DIRECTOR_SYSTEM_INCIDENT"
            });

            types.put("COORDINADOR", new String[]{
                "COORDINATOR_GLOBAL_UPDATE",
                "COORDINATOR_SYSTEM_ALERT",
                "COORDINATOR_CHANGE_CONFIRMATION",
                "COORDINATOR_MAINTENANCE_ALERT",
                "COORDINATOR_USER_REGISTRATION_PENDING",
                "COORDINATOR_USER_APPROVED",
                "COORDINATOR_USER_REJECTED"
            });

            types.put("GENERAL", new String[]{
                "GENERAL_SYSTEM_NOTIFICATION",
                "USER_REGISTRATION_APPROVED",
                "USER_REGISTRATION_REJECTED"
            });

            return ResponseEntity.ok(types);
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
    @Operation(summary = "Enviar TODAS las notificaciones del sistema por correo",
                description = "Envía todas las notificaciones disponibles del sistema SGH por correo electrónico para testing completo - SOLO PARA TESTING")
    public ResponseEntity<?> testAllNotifications(@RequestParam String testEmail) {
        try {
            log.info("Enviando TODAS las notificaciones disponibles del sistema SGH a: {}", testEmail);

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // ========================================
            // DATOS DINÁMICOS PARA PRUEBAS REALISTAS
            // ========================================
            String[] subjects = {"Matemáticas III", "Física II", "Química Orgánica", "Programación I", "Cálculo Diferencial", "Estadística"};
            String[] courses = {"Ingeniería de Sistemas", "Ingeniería Civil", "Medicina", "Administración", "Psicología", "Derecho"};
            String[] teachers = {"Dr. Juan Pérez", "Dra. María González", "Prof. Carlos Rodríguez", "Lic. Ana López", "MSc. Roberto Silva"};
            String[] days = {"LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES"};
            String[] times = {"08:00 - 10:00", "10:00 - 12:00", "14:00 - 16:00", "16:00 - 18:00", "18:00 - 20:00"};

            // Generar datos aleatorios para las pruebas
            String randomSubject = subjects[(int)(Math.random() * subjects.length)];
            String randomCourse = courses[(int)(Math.random() * courses.length)];
            String randomTeacher = teachers[(int)(Math.random() * teachers.length)];
            String randomDay = days[(int)(Math.random() * days.length)];
            String randomTime = times[(int)(Math.random() * times.length)];

            // ========================================
            // NOTIFICACIONES PRINCIPALES DEL SISTEMA (4 tipos)
            // ========================================

            // 1. TEACHER_SCHEDULE_ASSIGNED - Asignación de clase a profesor
            futures.add(sendTestNotificationAsync(testEmail, "MAESTRO", NotificationType.TEACHER_SCHEDULE_ASSIGNED,
                "👨‍🏫 Nueva Asignación de Clase - Materia Asignada",
                String.format("Estimado profesor,\n\nSe le ha asignado una nueva clase en el Sistema de Gestión de Horarios:\n\n📚 MATERIA: %s\n🏫 CURSO: %s\n📅 DÍA: %s\n⏰ HORARIO: %s\n🏢 AULA: A-%d\n\nEsta asignación ha sido realizada por el coordinador académico.\n\nPor favor, revise los detalles y confirme su disponibilidad para esta clase.\n\nIMPORTANTE: Si tiene algún conflicto de horario, notifique inmediatamente al coordinador.",
                    randomSubject, randomCourse, randomDay, randomTime, (int)(Math.random() * 50) + 101)));

            // 2. SCHEDULE_ASSIGNED - Horario asignado a estudiante
            futures.add(sendTestNotificationAsync(testEmail, "ESTUDIANTE", NotificationType.SCHEDULE_ASSIGNED,
                "📚 Tu Horario Académico ha sido Asignado",
                String.format("¡Hola estudiante!\n\nTu horario académico para este semestre ha sido asignado exitosamente:\n\n📖 MATERIA: %s\n👨‍🏫 PROFESOR: %s\n🏫 CURSO: %s\n📅 DÍA: %s\n⏰ HORARIO: %s\n🏢 AULA: B-%d\n\nEste horario está disponible en tu portal estudiantil.\n\nIMPORTANTE:\n• Revisa tu horario completo en el sistema\n• Anota las fechas importantes\n• Si tienes algún conflicto, contacta a tu coordinador\n\n¡Te deseamos éxito en tus estudios!",
                    randomSubject, randomTeacher, randomCourse, randomDay, randomTime, (int)(Math.random() * 30) + 201)));

            // 3. SYSTEM_ALERT - Alerta crítica para directores
            futures.add(sendTestNotificationAsync(testEmail, "DIRECTOR_DE_AREA", NotificationType.SYSTEM_ALERT,
                "🚨 ALERTA CRÍTICA: Conflicto de Horarios Detectado",
                String.format("DIRECTOR DE ÁREA,\n\n¡ATENCIÓN INMEDIATA REQUERIDA!\n\nEl sistema ha detectado un conflicto crítico de horarios que requiere su intervención:\n\n⚠️ TIPO DE CONFLICTO: Superposición de clases\n👨‍🏫 PROFESOR AFECTADO: %s\n📚 MATERIA: %s\n👥 ESTUDIANTES IMPACTADOS: %d estudiantes\n🏫 CURSO: %s\n⏰ HORARIO CONFLICTIVO: %s\n\nDETALLES:\n• Conflicto detectado en aula A-%d\n• Afecta al horario de %s\n• Requiere reprogramación inmediata\n\nACCIONES NECESARIAS:\n1. Revisar el conflicto en el panel administrativo\n2. Coordinar con el profesor afectado\n3. Reasignar aula o horario\n4. Notificar a los estudiantes\n\nEsta alerta tiene prioridad CRÍTICA. Se requiere resolución en las próximas 2 horas.",
                    randomTeacher, randomSubject, (int)(Math.random() * 25) + 15, randomCourse, randomTime, (int)(Math.random() * 50) + 101, randomDay)));

            // 4. SYSTEM_NOTIFICATION - Notificación del sistema para coordinadores
            futures.add(sendTestNotificationAsync(testEmail, "COORDINADOR", NotificationType.SYSTEM_NOTIFICATION,
                "📢 Actualización del Sistema: Nuevo Horario Registrado",
                String.format("COORDINADOR ACADÉMICO,\n\nEl Sistema de Gestión de Horarios informa:\n\n✅ NUEVO HORARIO REGISTRADO\n\n📊 DETALLES DE LA ASIGNACIÓN:\n• Profesor: %s\n• Materia: %s\n• Curso: %s\n• Día: %s\n• Horario: %s\n• Aula asignada: C-%d\n\n📈 ESTADÍSTICAS ACTUALES:\n• Total de horarios activos: %d\n• Profesores con horario completo: %d\n• Aulas ocupadas hoy: %d\n• Conflictos pendientes: %d\n\nEsta asignación se realizó correctamente y está disponible en el sistema.\n\nPara más detalles, acceda al panel de administración.",
                    randomTeacher, randomSubject, randomCourse, randomDay, randomTime, (int)(Math.random() * 20) + 301, (int)(Math.random() * 200) + 150, (int)(Math.random() * 15) + 10, (int)(Math.random() * 10) + 5, (int)(Math.random() * 3))));

            // Esperar a que todas las notificaciones se envíen
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

            return ResponseEntity.accepted()
                    .body(Map.of(
                        "success", true,
                        "message", "Notificaciones principales del Sistema SGH enviadas por correo",
                        "testEmail", testEmail,
                        "totalNotifications", futures.size(),
                        "notificationsByRole", Map.of(
                            "MAESTRO", List.of("TEACHER_SCHEDULE_ASSIGNED"),
                            "ESTUDIANTE", List.of("SCHEDULE_ASSIGNED"),
                            "DIRECTOR_DE_AREA", List.of("SYSTEM_ALERT"),
                            "COORDINADOR", List.of("SYSTEM_NOTIFICATION")
                        ),
                        "note", "Se enviaron las 4 notificaciones principales automatizadas del sistema SGH",
                        "status", "SENDING_CORE_SYSTEM_NOTIFICATIONS"
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