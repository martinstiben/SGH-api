package com.horarios.SGH.Service;

import com.horarios.SGH.DTO.NotificationDTO;
import com.horarios.SGH.Model.NotificationLog;
import com.horarios.SGH.Model.NotificationStatus;
import com.horarios.SGH.Model.NotificationType;
import com.horarios.SGH.Model.users;
import com.horarios.SGH.Repository.INotificationLogRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servicio principal para el envío de notificaciones por correo electrónico
 * Sistema de Gestión de Horarios (SGH)
 */
@Slf4j
@Service
@EnableAsync
public class NotificationService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private INotificationLogRepository notificationLogRepository;
    
    @Autowired
    private usersService userService;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.notification.max-retries:3}")
    private int maxRetries;
    
    @Value("${app.notification.retry-delay:30000}")
    private long retryDelay; // 30 segundos por defecto
    
    private final ExecutorService emailExecutor = Executors.newFixedThreadPool(5);
    
    /**
     * Valida y prepara notificación
     */
    public void validateAndPrepareNotification(NotificationDTO notification) {
        log.info("Validando notificación para: {}", notification.getRecipientEmail());

        NotificationType notificationType = NotificationType.valueOf(notification.getNotificationType());
        validateNotificationTypeForRole(notificationType, notification.getRecipientRole());

        NotificationLog logEntry = new NotificationLog(
            notification.getRecipientEmail(),
            notification.getRecipientName(),
            notification.getRecipientRole(),
            notificationType,
            notification.getSubject(),
            notification.getContent()
        );

        notificationLogRepository.save(logEntry);
        log.info("Notificación validada y preparada para envío a: {}", notification.getRecipientEmail());
    }

    @Async("emailExecutor")
    public CompletableFuture<Void> sendNotificationAsync(NotificationDTO notification) {
        return CompletableFuture.runAsync(() -> {
            log.info("Iniciando envío asíncrono de notificación a: {}", notification.getRecipientEmail());

            try {
                LocalDateTime since = LocalDateTime.now().minusMinutes(5);
                List<NotificationLog> recentLogs = notificationLogRepository
                    .findRecentByRecipientEmail(notification.getRecipientEmail(), since);

                NotificationLog logEntry = recentLogs.stream()
                    .filter(log -> log.getNotificationType().name().equals(notification.getNotificationType()) &&
                                  log.getSubject().equals(notification.getSubject()) &&
                                  log.getStatus().equals(NotificationStatus.PENDING))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Log de notificación no encontrado para envío asíncrono"));

                sendWithRetry(logEntry, notification);
                log.info("Notificación enviada exitosamente a: {}", notification.getRecipientEmail());

            } catch (Exception e) {
                log.error("Error final al enviar notificación a {}: {}", notification.getRecipientEmail(), e.getMessage());
                throw new RuntimeException("Error al enviar notificación: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Envía notificación masiva a múltiples destinatarios
     */
    @Async("emailExecutor")
    public CompletableFuture<Void> sendBulkNotificationAsync(List<NotificationDTO> notifications) {
        log.info("Iniciando envío masivo de {} notificaciones", notifications.size());
        
        List<CompletableFuture<Void>> futures = notifications.stream()
            .map(this::sendNotificationAsync)
            .toList();
        
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        
        return allFutures.thenRun(() -> 
            log.info("Envío masivo de notificaciones completado")
        );
    }
    
    /**
     * Envía notificación a todos los usuarios de un rol específico
     */
    @Async("emailExecutor")
    public CompletableFuture<Void> sendNotificationToRoleAsync(String role, NotificationType type, String subject, 
                                                               Map<String, String> variables) {
        log.info("Enviando notificación a todos los usuarios con rol: {}", role);
        
        List<users> usersWithRole = userService.findUsersByRole(role);
        
        List<CompletableFuture<Void>> futures = usersWithRole.stream()
            .map(user -> {
                NotificationDTO notification = createNotificationFromTemplate(user, type, subject, variables);
                return sendNotificationAsync(notification);
            })
            .toList();
        
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        
        return allFutures.thenRun(() -> 
            log.info("Envío de notificaciones por rol '{}' completado para {} usuarios", role, usersWithRole.size())
        );
    }
    
    /**
     * Reintenta notificaciones fallidas
     */
    @Async
    public CompletableFuture<Void> retryFailedNotifications() {
        log.info("Iniciando reintento de notificaciones fallidas");
        
        List<NotificationLog> failedNotifications = notificationLogRepository
            .findFailedNotificationsToRetry(NotificationStatus.FAILED);
        
        int retryCount = 0;
        for (NotificationLog failedLog : failedNotifications) {
            if (failedLog.canRetry()) {
                try {
                    Thread.sleep(retryDelay);
                    NotificationDTO notification = new NotificationDTO();
                    notification.setRecipientEmail(failedLog.getRecipientEmail());
                    notification.setRecipientName(failedLog.getRecipientName());
                    notification.setRecipientRole(failedLog.getRecipientRole());
                    notification.setNotificationType(failedLog.getNotificationType().name());
                    notification.setSubject(failedLog.getSubject());
                    notification.setContent(failedLog.getContent());
                    
                    sendWithRetry(failedLog, notification);
                    retryCount++;
                } catch (Exception e) {
                    log.error("Error al reintentar notificación a {}: {}", failedLog.getRecipientEmail(), e.getMessage());
                }
            }
        }
        
        log.info("Completados {} reintentos de notificaciones fallidas", retryCount);
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Proceso principal de envío con reintentos automáticos
     */
    private void sendWithRetry(NotificationLog logEntry, NotificationDTO notification) {
        while (logEntry.canRetry()) {
            try {
                logEntry.incrementAttempts();
                log.info("Intento {} de {} para enviar notificación a: {}", 
                        logEntry.getAttemptsCount(), maxRetries, notification.getRecipientEmail());
                
                sendEmail(notification);
                logEntry.markAsSent();
                notificationLogRepository.save(logEntry);
                
                log.info("Notificación enviada exitosamente después de {} intentos", logEntry.getAttemptsCount());
                return;
                
            } catch (Exception e) {
                String errorMessage = String.format("Error en intento %d: %s", logEntry.getAttemptsCount(), e.getMessage());
                log.error("Error al enviar notificación a {}: {}", notification.getRecipientEmail(), e.getMessage());
                
                logEntry.markAsFailed(errorMessage);
                notificationLogRepository.save(logEntry);
                
                if (logEntry.canRetry()) {
                    try {
                        Thread.sleep(retryDelay * logEntry.getAttemptsCount());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("Se agotaron los {} intentos para enviar notificación a: {}", 
                             maxRetries, notification.getRecipientEmail());
                    break;
                }
            }
        }
    }
    
    /**
     * Envía correo electrónico usando plantillas HTML optimizadas para Gmail
     */
    private void sendEmail(NotificationDTO notification) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setTo(notification.getRecipientEmail());
        helper.setFrom(fromEmail);
        helper.setSubject(notification.getSubject());
        helper.setPriority(1);
        
        String htmlContent = generateHtmlContent(notification);
        helper.setText(htmlContent, true);
        
        message.setHeader("X-Notification-Type", notification.getNotificationType());
        message.setHeader("X-Recipient-Role", notification.getRecipientRole());
        message.setHeader("X-Sender", "SGH System");
        
        mailSender.send(message);
        
        log.info("Correo enviado exitosamente a {} con asunto: {}", 
                notification.getRecipientEmail(), notification.getSubject());
    }
    
    /**
     * Genera contenido HTML usando plantillas optimizadas para Gmail
     */
    private String generateHtmlContent(NotificationDTO notification) {
        try {
            if (notification.getIsHtml() && notification.getContent() != null && !notification.getContent().isEmpty()) {
                return notification.getContent();
            }
            
            return generateRoleBasedHtmlContent(notification);
            
        } catch (Exception e) {
            log.warn("Error al generar contenido HTML, usando contenido por defecto: {}", e.getMessage());
            return generateDefaultHtmlContent(notification);
        }
    }
    
    /**
     * Genera contenido HTML basado en rol del destinatario
     */
    private String generateRoleBasedHtmlContent(NotificationDTO notification) {
        String recipientRole = notification.getRecipientRole();

        switch (recipientRole) {
            case "ESTUDIANTE":
                return generateStudentHtmlContent(notification);
            case "MAESTRO":
                return generateTeacherHtmlContent(notification);
            case "DIRECTOR_DE_AREA":
                return generateDirectorHtmlContent(notification);
            case "COORDINADOR":
                return generateCoordinatorHtmlContent(notification);
            default:
                return generateGeneralHtmlContent(notification);
        }
    }

    /**
     * Plantilla HTML optimizada para Gmail - Estudiantes
     */
    private String generateStudentHtmlContent(NotificationDTO notification) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>SGH - Notificación para Estudiante</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; color: #333; line-height: 1.4; }
                    .container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background-color: #4CAF50; color: white; padding: 25px; text-align: center; }
                    .logo { font-size: 36px; margin-bottom: 15px; }
                    .header h1 { font-size: 22px; margin: 0 0 8px 0; font-weight: bold; }
                    .header p { font-size: 16px; margin: 0; }
                    .content { padding: 30px 25px; }
                    .notification-card { background-color: #ffffff; border: 1px solid #e0e0e0; border-radius: 6px; padding: 25px; margin-bottom: 25px; border-left: 4px solid #4CAF50; }
                    .notification-title { color: #2c3e50; font-size: 20px; font-weight: bold; margin: 0 0 15px 0; }
                    .notification-content { color: #495057; font-size: 16px; line-height: 1.6; margin-bottom: 20px; }
                    .info-table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                    .info-table td { padding: 12px 8px; border-bottom: 1px solid #e0e0e0; vertical-align: top; }
                    .info-table td:first-child { font-weight: bold; color: #6c757d; font-size: 12px; text-transform: uppercase; width: 40%%; }
                    .info-table td:last-child { color: #2c3e50; font-size: 14px; }
                    .action-section { background-color: #e8f5e8; border: 1px solid #c8e6c9; border-radius: 6px; padding: 20px; text-align: center; margin-top: 20px; }
                    .action-text { color: #2e7d32; font-size: 14px; margin-bottom: 15px; font-weight: 500; }
                    .action-button { display: inline-block; background-color: #4CAF50; color: white; padding: 12px 30px; text-decoration: none; border-radius: 4px; font-weight: bold; font-size: 14px; }
                    .footer { background-color: #2c3e50; color: white; padding: 25px; text-align: center; }
                    .footer-logo { font-size: 20px; font-weight: bold; margin-bottom: 10px; color: #4CAF50; }
                    .footer-text { font-size: 13px; opacity: 0.8; line-height: 1.5; margin-bottom: 15px; }
                    .footer-links { margin-top: 15px; }
                    .footer-links a { color: #4CAF50; text-decoration: none; margin: 0 10px; font-size: 12px; }
                    @media screen and (max-width: 600px) {
                        .container { margin: 10px; border-radius: 0; }
                        .header, .content, .footer { padding: 20px 15px; }
                        .notification-card { padding: 20px 15px; }
                        .info-table td { display: block; border-bottom: none; padding: 5px 0; }
                        .info-table td:first-child { border-bottom: 1px solid #e0e0e0; padding-bottom: 5px; }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">🎓</div>
                        <h1>Sistema de Gestión de Horarios</h1>
                        <p>¡Hola, %s!</p>
                    </div>

                    <div class="content">
                        <div class="notification-card">
                            <h2 class="notification-title">📚 %s</h2>
                            <div class="notification-content">%s</div>

                            <table class="info-table">
                                <tr><td>Destinatario</td><td>%s</td></tr>
                                <tr><td>Rol</td><td>Estudiante</td></tr>
                                <tr><td>Fecha y Hora</td><td>%s</td></tr>
                                <tr><td>Categoría</td><td>Información Académica</td></tr>
                            </table>

                            <div class="action-section">
                                <div class="action-text">📚 Esta notificación contiene información importante sobre tu horario académico</div>
                                <a href="#" class="action-button">Ver Horario</a>
                            </div>
                        </div>
                    </div>

                    <div class="footer">
                        <div class="footer-logo">SGH</div>
                        <div class="footer-text">
                            <p>Sistema de Gestión de Horarios Académicos</p>
                            <p>Institución Educativa - Transformando el futuro de la educación</p>
                        </div>
                        <div class="footer-links">
                            <a href="#">Portal Estudiantil</a>
                            <a href="#">Soporte</a>
                            <a href="#">Contacto</a>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
            notification.getRecipientName(),
            notification.getSubject(),
            notification.getContent(),
            notification.getRecipientEmail(),
            LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );
    }

    /**
     * Plantilla HTML optimizada para Gmail - Maestros
     */
    private String generateTeacherHtmlContent(NotificationDTO notification) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>SGH - Notificación para Docente</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; color: #333; line-height: 1.4; }
                    .container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background-color: #2196F3; color: white; padding: 25px; text-align: center; }
                    .logo { font-size: 36px; margin-bottom: 15px; }
                    .header h1 { font-size: 22px; margin: 0 0 8px 0; font-weight: bold; }
                    .header p { font-size: 16px; margin: 0; }
                    .content { padding: 30px 25px; }
                    .notification-card { background-color: #ffffff; border: 1px solid #e0e0e0; border-radius: 6px; padding: 25px; margin-bottom: 25px; border-left: 4px solid #2196F3; }
                    .notification-title { color: #2c3e50; font-size: 20px; font-weight: bold; margin: 0 0 15px 0; }
                    .notification-content { color: #495057; font-size: 16px; line-height: 1.6; margin-bottom: 20px; }
                    .info-table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                    .info-table td { padding: 12px 8px; border-bottom: 1px solid #e0e0e0; vertical-align: top; }
                    .info-table td:first-child { font-weight: bold; color: #6c757d; font-size: 12px; text-transform: uppercase; width: 40%%; }
                    .info-table td:last-child { color: #2c3e50; font-size: 14px; }
                    .action-section { background-color: #e3f2fd; border: 1px solid #bbdefb; border-radius: 6px; padding: 20px; text-align: center; margin-top: 20px; }
                    .action-text { color: #1565C0; font-size: 14px; margin-bottom: 15px; font-weight: 500; }
                    .action-button { display: inline-block; background-color: #2196F3; color: white; padding: 12px 30px; text-decoration: none; border-radius: 4px; font-weight: bold; font-size: 14px; }
                    .footer { background-color: #2c3e50; color: white; padding: 25px; text-align: center; }
                    .footer-logo { font-size: 20px; font-weight: bold; margin-bottom: 10px; color: #2196F3; }
                    .footer-text { font-size: 13px; opacity: 0.8; line-height: 1.5; margin-bottom: 15px; }
                    .footer-links { margin-top: 15px; }
                    .footer-links a { color: #2196F3; text-decoration: none; margin: 0 10px; font-size: 12px; }
                    @media screen and (max-width: 600px) {
                        .container { margin: 10px; border-radius: 0; }
                        .header, .content, .footer { padding: 20px 15px; }
                        .notification-card { padding: 20px 15px; }
                        .info-table td { display: block; border-bottom: none; padding: 5px 0; }
                        .info-table td:first-child { border-bottom: 1px solid #e0e0e0; padding-bottom: 5px; }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">👨‍🏫</div>
                        <h1>Sistema de Gestión de Horarios</h1>
                        <p>Profesor/a %s</p>
                    </div>

                    <div class="content">
                        <div class="notification-card">
                            <h2 class="notification-title">📋 %s</h2>
                            <div class="notification-content">%s</div>

                            <table class="info-table">
                                <tr><td>Destinatario</td><td>%s</td></tr>
                                <tr><td>Rol</td><td>Docente</td></tr>
                                <tr><td>Fecha y Hora</td><td>%s</td></tr>
                                <tr><td>Categoría</td><td>Gestión Académica</td></tr>
                            </table>

                            <div class="action-section">
                                <div class="action-text">📋 Esta notificación contiene información sobre tu horario</div>
                            </div>
                        </div>
                    </div>

                    <div class="footer">
                        <div class="footer-logo">SGH</div>
                        <div class="footer-text">
                            <p>Sistema de Gestión de Horarios Académicos</p>
                            <p>Institución Educativa - Excelencia en la educación</p>
                        </div>
                        <div class="footer-links">
                            <a href="#">Portal Docente</a>
                            <a href="#">Recursos</a>
                            <a href="#">Soporte</a>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
            notification.getRecipientName(),
            notification.getSubject(),
            notification.getContent(),
            notification.getRecipientEmail(),
            LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );
    }

    /**
     * Plantilla HTML optimizada para Gmail - Directores
     */
    private String generateDirectorHtmlContent(NotificationDTO notification) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>SGH - Notificación para Director</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; color: #333; line-height: 1.4; }
                    .container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background-color: #9C27B0; color: white; padding: 25px; text-align: center; }
                    .logo { font-size: 36px; margin-bottom: 15px; }
                    .header h1 { font-size: 22px; margin: 0 0 8px 0; font-weight: bold; }
                    .header p { font-size: 16px; margin: 0; }
                    .content { padding: 30px 25px; }
                    .notification-card { background-color: #ffffff; border: 1px solid #e0e0e0; border-radius: 6px; padding: 25px; margin-bottom: 25px; border-left: 4px solid #9C27B0; }
                    .notification-title { color: #2c3e50; font-size: 20px; font-weight: bold; margin: 0 0 15px 0; }
                    .notification-content { color: #495057; font-size: 16px; line-height: 1.6; margin-bottom: 20px; }
                    .priority-badge { display: inline-block; background-color: #FF5722; color: white; padding: 8px 16px; border-radius: 20px; font-size: 12px; font-weight: bold; text-transform: uppercase; margin-bottom: 15px; }
                    .info-table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                    .info-table td { padding: 12px 8px; border-bottom: 1px solid #e0e0e0; vertical-align: top; }
                    .info-table td:first-child { font-weight: bold; color: #6c757d; font-size: 12px; text-transform: uppercase; width: 40%%; }
                    .info-table td:last-child { color: #2c3e50; font-size: 14px; }
                    .action-section { background-color: #f3e5f5; border: 1px solid #ce93d8; border-radius: 6px; padding: 20px; text-align: center; margin-top: 20px; }
                    .action-text { color: #7B1FA2; font-size: 14px; margin-bottom: 15px; font-weight: 500; }
                    .action-button { display: inline-block; background-color: #9C27B0; color: white; padding: 12px 30px; text-decoration: none; border-radius: 4px; font-weight: bold; font-size: 14px; }
                    .footer { background-color: #2c3e50; color: white; padding: 25px; text-align: center; }
                    .footer-logo { font-size: 20px; font-weight: bold; margin-bottom: 10px; color: #9C27B0; }
                    .footer-text { font-size: 13px; opacity: 0.8; line-height: 1.5; margin-bottom: 15px; }
                    .footer-links { margin-top: 15px; }
                    .footer-links a { color: #9C27B0; text-decoration: none; margin: 0 10px; font-size: 12px; }
                    @media screen and (max-width: 600px) {
                        .container { margin: 10px; border-radius: 0; }
                        .header, .content, .footer { padding: 20px 15px; }
                        .notification-card { padding: 20px 15px; }
                        .info-table td { display: block; border-bottom: none; padding: 5px 0; }
                        .info-table td:first-child { border-bottom: 1px solid #e0e0e0; padding-bottom: 5px; }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">👔</div>
                        <h1>Sistema de Gestión de Horarios</h1>
                        <p>Director/a %s</p>
                    </div>

                    <div class="content">
                        <div class="notification-card">
                            <div class="priority-badge">⚠️ Alta Prioridad</div>
                            <h2 class="notification-title">🚨 %s</h2>
                            <div class="notification-content">%s</div>

                            <table class="info-table">
                                <tr><td>Destinatario</td><td>%s</td></tr>
                                <tr><td>Rol</td><td>Director de Área</td></tr>
                                <tr><td>Fecha y Hora</td><td>%s</td></tr>
                                <tr><td>Tipo</td><td>Gestión Administrativa</td></tr>
                            </table>

                            <div class="action-section">
                                <div class="action-text">🚨 Esta notificación requiere atención inmediata del área administrativa</div>
                                <a href="#" class="action-button">Revisar en el Sistema</a>
                            </div>
                        </div>
                    </div>

                    <div class="footer">
                        <div class="footer-logo">SGH</div>
                        <div class="footer-text">
                            <p>Sistema de Gestión de Horarios Académicos</p>
                            <p>Institución Educativa - Liderazgo y Excelencia</p>
                        </div>
                        <div class="footer-links">
                            <a href="#">Panel Administrativo</a>
                            <a href="#">Reportes</a>
                            <a href="#">Soporte</a>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
            notification.getRecipientName(),
            notification.getSubject(),
            notification.getContent(),
            notification.getRecipientEmail(),
            LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );
    }

    /**
     * Plantilla HTML optimizada para Gmail - Coordinadores
     */
    private String generateCoordinatorHtmlContent(NotificationDTO notification) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>SGH - Notificación para Coordinador</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; color: #333; line-height: 1.4; }
                    .container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background-color: #FF5722; color: white; padding: 25px; text-align: center; }
                    .logo { font-size: 36px; margin-bottom: 15px; }
                    .header h1 { font-size: 22px; margin: 0 0 8px 0; font-weight: bold; }
                    .header p { font-size: 16px; margin: 0; }
                    .content { padding: 30px 25px; }
                    .notification-card { background-color: #ffffff; border: 1px solid #e0e0e0; border-radius: 6px; padding: 25px; margin-bottom: 25px; border-left: 4px solid #FF5722; }
                    .notification-title { color: #2c3e50; font-size: 20px; font-weight: bold; margin: 0 0 15px 0; }
                    .notification-content { color: #495057; font-size: 16px; line-height: 1.6; margin-bottom: 20px; }
                    .system-status { background-color: #fff3e0; border: 1px solid #ffe0b2; border-radius: 6px; padding: 15px; margin-bottom: 20px; text-align: center; }
                    .status-indicator { display: inline-block; width: 12px; height: 12px; background: #FF5722; border-radius: 50%%; margin-right: 8px; animation: pulse 2s infinite; }
                    @keyframes pulse { 0%% { box-shadow: 0 0 0 0 rgba(255, 87, 34, 0.7); } 70%% { box-shadow: 0 0 0 10px rgba(255, 87, 34, 0); } 100%% { box-shadow: 0 0 0 0 rgba(255, 87, 34, 0); } }
                    .info-table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                    .info-table td { padding: 12px 8px; border-bottom: 1px solid #e0e0e0; vertical-align: top; }
                    .info-table td:first-child { font-weight: bold; color: #6c757d; font-size: 12px; text-transform: uppercase; width: 40%%; }
                    .info-table td:last-child { color: #2c3e50; font-size: 14px; }
                    .footer { background-color: #2c3e50; color: white; padding: 25px; text-align: center; }
                    .footer-logo { font-size: 20px; font-weight: bold; margin-bottom: 10px; color: #FF5722; }
                    .footer-text { font-size: 13px; opacity: 0.8; line-height: 1.5; margin-bottom: 15px; }
                    .footer-links { margin-top: 15px; }
                    .footer-links a { color: #FF5722; text-decoration: none; margin: 0 10px; font-size: 12px; }
                    @media screen and (max-width: 600px) {
                        .container { margin: 10px; border-radius: 0; }
                        .header, .content, .footer { padding: 20px 15px; }
                        .notification-card { padding: 20px 15px; }
                        .info-table td { display: block; border-bottom: none; padding: 5px 0; }
                        .info-table td:first-child { border-bottom: 1px solid #e0e0e0; padding-bottom: 5px; }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">⚙️</div>
                        <h1>Sistema de Gestión de Horarios</h1>
                        <p>Coordinador/a %s</p>
                    </div>

                    <div class="content">
                        <div class="notification-card">
                            <div class="system-status">
                                <span class="status-indicator"></span>
                                <strong>Notificación del Sistema de Gestión</strong>
                            </div>
                            <h2 class="notification-title">⚙️ %s</h2>
                            <div class="notification-content">%s</div>

                            <table class="info-table">
                                <tr><td>Destinatario</td><td>%s</td></tr>
                                <tr><td>Rol</td><td>Coordinador</td></tr>
                                <tr><td>Fecha y Hora</td><td>%s</td></tr>
                                <tr><td>Tipo</td><td>Administración del Sistema</td></tr>
                            </table>
                        </div>
                    </div>

                    <div class="footer">
                        <div class="footer-logo">SGH</div>
                        <div class="footer-text">
                            <p>Sistema de Gestión de Horarios Académicos</p>
                            <p>Institución Educativa - Control Total del Sistema</p>
                        </div>
                        <div class="footer-links">
                            <a href="#">Panel Admin</a>
                            <a href="#">Configuración</a>
                            <a href="#">Soporte</a>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
            notification.getRecipientName(),
            notification.getSubject(),
            notification.getContent(),
            notification.getRecipientEmail(),
            LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );
    }

    /**
     * Plantilla HTML general
     */
    private String generateGeneralHtmlContent(NotificationDTO notification) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>SGH - Notificación General</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; color: #333; line-height: 1.4; }
                    .container { max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background-color: #6c757d; color: white; padding: 25px; text-align: center; }
                    .logo { font-size: 36px; margin-bottom: 15px; }
                    .header h1 { font-size: 22px; margin: 0 0 8px 0; font-weight: bold; }
                    .header p { font-size: 16px; margin: 0; }
                    .content { padding: 30px 25px; }
                    .notification-card { background-color: #ffffff; border: 1px solid #e0e0e0; border-radius: 6px; padding: 25px; margin-bottom: 25px; border-left: 4px solid #6c757d; }
                    .notification-title { color: #2c3e50; font-size: 20px; font-weight: bold; margin: 0 0 15px 0; }
                    .notification-content { color: #495057; font-size: 16px; line-height: 1.6; margin-bottom: 20px; }
                    .info-table { width: 100%%; border-collapse: collapse; margin: 20px 0; }
                    .info-table td { padding: 12px 8px; border-bottom: 1px solid #e0e0e0; vertical-align: top; }
                    .info-table td:first-child { font-weight: bold; color: #6c757d; font-size: 12px; text-transform: uppercase; width: 40%%; }
                    .info-table td:last-child { color: #2c3e50; font-size: 14px; }
                    .footer { background-color: #2c3e50; color: white; padding: 25px; text-align: center; }
                    .footer-logo { font-size: 20px; font-weight: bold; margin-bottom: 10px; color: #6c757d; }
                    .footer-text { font-size: 13px; opacity: 0.8; line-height: 1.5; margin-bottom: 15px; }
                    .footer-links { margin-top: 15px; }
                    .footer-links a { color: #6c757d; text-decoration: none; margin: 0 10px; font-size: 12px; }
                    @media screen and (max-width: 600px) {
                        .container { margin: 10px; border-radius: 0; }
                        .header, .content, .footer { padding: 20px 15px; }
                        .notification-card { padding: 20px 15px; }
                        .info-table td { display: block; border-bottom: none; padding: 5px 0; }
                        .info-table td:first-child { border-bottom: 1px solid #e0e0e0; padding-bottom: 5px; }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">📢</div>
                        <h1>Sistema de Gestión de Horarios</h1>
                        <p>Notificación General</p>
                    </div>

                    <div class="content">
                        <div class="notification-card">
                            <h2 class="notification-title">📢 %s</h2>
                            <div class="notification-content">%s</div>

                            <table class="info-table">
                                <tr><td>Destinatario</td><td>%s</td></tr>
                                <tr><td>Rol</td><td>%s</td></tr>
                                <tr><td>Fecha y Hora</td><td>%s</td></tr>
                                <tr><td>Categoría</td><td>Notificación General</td></tr>
                            </table>
                        </div>
                    </div>

                    <div class="footer">
                        <div class="footer-logo">SGH</div>
                        <div class="footer-text">
                            <p>Sistema de Gestión de Horarios Académicos</p>
                            <p>Institución Educativa - Conectando el conocimiento</p>
                        </div>
                        <div class="footer-links">
                            <a href="#">Portal Principal</a>
                            <a href="#">Ayuda</a>
                            <a href="#">Contacto</a>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
            notification.getSubject(),
            notification.getContent(),
            notification.getRecipientName(),
            notification.getRecipientRole(),
            LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );
    }

    /**
     * Genera contenido HTML por defecto
     */
    private String generateDefaultHtmlContent(NotificationDTO notification) {
        return generateGeneralHtmlContent(notification);
    }

    /**
     * Crea NotificationDTO desde usuario y tipo de notificación
     */
    private NotificationDTO createNotificationFromTemplate(users user, NotificationType type, String subject,
                                                          Map<String, String> variables) {
        NotificationDTO notification = new NotificationDTO();
        notification.setRecipientEmail(user.getPerson().getEmail());
        notification.setRecipientName(user.getPerson().getFullName());
        notification.setRecipientRole(user.getRole().getRoleName());
        notification.setNotificationType(type.name());
        notification.setSubject(subject);
        notification.setContent("");
        notification.setSenderName("Sistema SGH");
        notification.setIsHtml(true);

        return notification;
    }

    /**
     * Obtiene estadísticas de notificaciones
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationStatistics() {
        java.util.Map<String, Object> stats = new java.util.concurrent.ConcurrentHashMap<>();
        stats.put("total", notificationLogRepository.count());
        stats.put("message", "Estadísticas básicas del sistema de notificaciones");
        stats.put("availableTypes", NotificationType.values());
        stats.put("availableRoles", new String[]{"ESTUDIANTE", "MAESTRO", "DIRECTOR_DE_AREA", "COORDINADOR"});

        return stats;
    }

    /**
     * Valida que el tipo de notificación sea válido para el rol especificado
     */
    private void validateNotificationTypeForRole(NotificationType notificationType, String recipientRole) {
        String[] allowedRoles = notificationType.getAllowedRoles();

        for (String allowedRole : allowedRoles) {
            if (allowedRole.equals(recipientRole)) {
                return;
            }
        }

        throw new IllegalArgumentException(
            String.format("El tipo de notificación '%s' no está permitido para el rol '%s'. " +
                         "Tipos permitidos para %s: %s",
                         notificationType.name(),
                         recipientRole,
                         recipientRole,
                         String.join(", ", allowedRoles))
        );
    }

    /**
     * Método público para testing directo - envía notificación inmediatamente
     */
    public String sendTestNotificationDirect(NotificationDTO notification) {
        try {
            NotificationLog logEntry = new NotificationLog(
                notification.getRecipientEmail(),
                notification.getRecipientName(),
                notification.getRecipientRole(),
                NotificationType.valueOf(notification.getNotificationType()),
                notification.getSubject(),
                notification.getContent()
            );
            notificationLogRepository.save(logEntry);

            sendEmail(notification);

            logEntry.markAsSent();
            notificationLogRepository.save(logEntry);

            return "OK";

        } catch (Exception e) {
            String errorMsg = e.getMessage();
            log.error("Error en envío directo de testing: {}", errorMsg);

            try {
                NotificationLog failedLog = new NotificationLog(
                    notification.getRecipientEmail(),
                    notification.getRecipientName(),
                    notification.getRecipientRole(),
                    NotificationType.valueOf(notification.getNotificationType()),
                    notification.getSubject(),
                    notification.getContent()
                );
                failedLog.markAsFailed(errorMsg);
                notificationLogRepository.save(failedLog);
            } catch (Exception logError) {
                log.warn("No se pudo crear log de error: {}", logError.getMessage());
            }

            return errorMsg;
        }
    }
}
