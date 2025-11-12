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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servicio principal para el envío de notificaciones por correo electrónico
 * Maneja el envío asíncrono, plantillas HTML, reintentos automáticos y logging
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
     * Envía notificación de forma asíncrona con reintentos automáticos
     */
    @Async("emailExecutor")
    public CompletableFuture<Void> sendNotificationAsync(NotificationDTO notification) {
        log.info("Iniciando envío asíncrono de notificación a: {}", notification.getRecipientEmail());
        
        NotificationLog logEntry = new NotificationLog(
            notification.getRecipientEmail(),
            notification.getRecipientName(),
            notification.getRecipientRole(),
            NotificationType.valueOf(notification.getNotificationType()),
            notification.getSubject(),
            notification.getContent()
        );
        
        notificationLogRepository.save(logEntry);
        
        try {
            sendWithRetry(logEntry, notification);
            log.info("Notificación enviada exitosamente a: {}", notification.getRecipientEmail());
        } catch (Exception e) {
            log.error("Error final al enviar notificación a {}: {}", notification.getRecipientEmail(), e.getMessage());
        }
        
        return CompletableFuture.completedFuture(null);
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
                    Thread.sleep(retryDelay); // Esperar antes de reintentar
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
                        Thread.sleep(retryDelay * logEntry.getAttemptsCount()); // Backoff exponencial
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
     * Envía correo electrónico usando plantillas HTML
     */
    private void sendEmail(NotificationDTO notification) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        // Configurar destinatario y asunto
        helper.setTo(notification.getRecipientEmail());
        helper.setFrom(fromEmail);
        helper.setSubject(notification.getSubject());
        helper.setPriority(1); // Alta prioridad
        
        // Generar contenido HTML
        String htmlContent = generateHtmlContent(notification);
        helper.setText(htmlContent, true);
        
        // Agregar headers personalizados
        message.setHeader("X-Notification-Type", notification.getNotificationType());
        message.setHeader("X-Recipient-Role", notification.getRecipientRole());
        message.setHeader("X-Sender", "SGH System");
        
        // Enviar correo
        mailSender.send(message);
        
        log.info("Correo enviado exitosamente a {} con asunto: {}", 
                notification.getRecipientEmail(), notification.getSubject());
    }
    
    /**
     * Genera contenido HTML usando plantillas
     */
    private String generateHtmlContent(NotificationDTO notification) {
        try {
            // Usar contenido directo si está disponible
            if (notification.getIsHtml() && notification.getContent() != null && !notification.getContent().isEmpty()) {
                return notification.getContent();
            }
            
            // Generar contenido basado en rol y tipo
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
        String notificationType = notification.getNotificationType();
        
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
     * Plantilla HTML para estudiantes
     */
    private String generateStudentHtmlContent(NotificationDTO notification) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Notificación de Horarios - Estudiante</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f0f8ff; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #4CAF50, #45a049); color: white; padding: 25px; border-radius: 12px 12px 0 0; text-align: center; }
                    .icon { font-size: 48px; margin-bottom: 10px; }
                    .content { padding: 25px 0; line-height: 1.8; }
                    .info-box { background-color: #e8f5e8; border-left: 5px solid #4CAF50; padding: 20px; margin: 20px 0; border-radius: 5px; }
                    .footer { background-color: #f8f9fa; padding: 20px; border-radius: 0 0 12px 12px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="icon">📚</div>
                        <h1>Actualización de Horarios</h1>
                        <p>Hola %s</p>
                    </div>
                    <div class="content">
                        <h2>%s</h2>
                        <div class="info-box">
                            %s
                        </div>
                        <p><strong>📧 Destinatario:</strong> %s</p>
                        <p><strong>🎯 Rol:</strong> Estudiante</p>
                        <p><strong>⏰ Fecha y hora:</strong> %s</p>
                        <p>Si tienes alguna pregunta sobre esta actualización, contacta a tu coordinador.</p>
                    </div>
                    <div class="footer">
                        <p>🎓 Este es un mensaje del Sistema de Gestión de Horarios (SGH)</p>
                        <p>¡Mantente al día con tus horarios académicos!</p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            notification.getRecipientName(),
            notification.getSubject(),
            notification.getContent(),
            notification.getRecipientEmail(),
            LocalDateTime.now().toString()
        );
    }
    
    /**
     * Plantilla HTML para maestros
     */
    private String generateTeacherHtmlContent(NotificationDTO notification) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Notificación de Clases - Maestro</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #fff8e1; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #2196F3, #1976D2); color: white; padding: 25px; border-radius: 12px 12px 0 0; text-align: center; }
                    .icon { font-size: 48px; margin-bottom: 10px; }
                    .content { padding: 25px 0; line-height: 1.8; }
                    .info-box { background-color: #e3f2fd; border-left: 5px solid #2196F3; padding: 20px; margin: 20px 0; border-radius: 5px; }
                    .footer { background-color: #f8f9fa; padding: 20px; border-radius: 0 0 12px 12px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="icon">👨‍🏫</div>
                        <h1>Notificación de Clases</h1>
                        <p>Profesor/a %s</p>
                    </div>
                    <div class="content">
                        <h2>%s</h2>
                        <div class="info-box">
                            %s
                        </div>
                        <p><strong>📧 Email:</strong> %s</p>
                        <p><strong>🎯 Rol:</strong> Maestro</p>
                        <p><strong>⏰ Fecha y hora:</strong> %s</p>
                        <p>Por favor, revisa tu horario actualizado en el sistema.</p>
                    </div>
                    <div class="footer">
                        <p>🏫 Este es un mensaje del Sistema de Gestión de Horarios (SGH)</p>
                        <p>¡Gracias por tu dedicación en la educación!</p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            notification.getRecipientName(),
            notification.getSubject(),
            notification.getContent(),
            notification.getRecipientEmail(),
            LocalDateTime.now().toString()
        );
    }
    
    /**
     * Plantilla HTML para directores
     */
    private String generateDirectorHtmlContent(NotificationDTO notification) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Alerta de Gestión - Director de Área</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #fce4ec; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #9C27B0, #7B1FA2); color: white; padding: 25px; border-radius: 12px 12px 0 0; text-align: center; }
                    .icon { font-size: 48px; margin-bottom: 10px; }
                    .content { padding: 25px 0; line-height: 1.8; }
                    .info-box { background-color: #f3e5f5; border-left: 5px solid #9C27B0; padding: 20px; margin: 20px 0; border-radius: 5px; }
                    .footer { background-color: #f8f9fa; padding: 20px; border-radius: 0 0 12px 12px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="icon">👔</div>
                        <h1>Alerta de Gestión</h1>
                        <p>Director/a %s</p>
                    </div>
                    <div class="content">
                        <h2>%s</h2>
                        <div class="info-box">
                            %s
                        </div>
                        <p><strong>📧 Email:</strong> %s</p>
                        <p><strong>🎯 Rol:</strong> Director de Área</p>
                        <p><strong>⏰ Fecha y hora:</strong> %s</p>
                        <p>Esta notificación requiere su atención para revisión y acción.</p>
                    </div>
                    <div class="footer">
                        <p>💼 Este es un mensaje del Sistema de Gestión de Horarios (SGH)</p>
                        <p>Su gestión es fundamental para el funcionamiento óptimo del sistema.</p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            notification.getRecipientName(),
            notification.getSubject(),
            notification.getContent(),
            notification.getRecipientEmail(),
            LocalDateTime.now().toString()
        );
    }
    
    /**
     * Plantilla HTML para coordinadores
     */
    private String generateCoordinatorHtmlContent(NotificationDTO notification) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Notificación del Sistema - Coordinador</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #fff3e0; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #FF5722, #E64A19); color: white; padding: 25px; border-radius: 12px 12px 0 0; text-align: center; }
                    .icon { font-size: 48px; margin-bottom: 10px; }
                    .content { padding: 25px 0; line-height: 1.8; }
                    .info-box { background-color: #fff8e1; border-left: 5px solid #FF5722; padding: 20px; margin: 20px 0; border-radius: 5px; }
                    .footer { background-color: #f8f9fa; padding: 20px; border-radius: 0 0 12px 12px; text-align: center; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="icon">⚙️</div>
                        <h1>Notificación del Sistema</h1>
                        <p>Coordinador/a %s</p>
                    </div>
                    <div class="content">
                        <h2>%s</h2>
                        <div class="info-box">
                            %s
                        </div>
                        <p><strong>📧 Email:</strong> %s</p>
                        <p><strong>🎯 Rol:</strong> Coordinador</p>
                        <p><strong>⏰ Fecha y hora:</strong> %s</p>
                        <p>Puede acceder al panel de control para gestionar esta notificación.</p>
                    </div>
                    <div class="footer">
                        <p>🎛️ Este es un mensaje del Sistema de Gestión de Horarios (SGH)</p>
                        <p>Usted tiene control total sobre las notificaciones del sistema.</p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            notification.getRecipientName(),
            notification.getSubject(),
            notification.getContent(),
            notification.getRecipientEmail(),
            LocalDateTime.now().toString()
        );
    }
    
    /**
     * Plantilla HTML general
     */
    private String generateGeneralHtmlContent(NotificationDTO notification) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Notificación SGH</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background-color: #007bff; color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center; }
                    .content { padding: 20px 0; line-height: 1.6; }
                    .footer { background-color: #f8f9fa; padding: 15px; border-radius: 0 0 8px 8px; text-align: center; font-size: 12px; color: #666; }
                    .highlight { background-color: #e7f3ff; padding: 15px; border-left: 4px solid #007bff; margin: 15px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Sistema de Gestión de Horarios</h1>
                        <p>Notificación para %s</p>
                    </div>
                    <div class="content">
                        <h2>%s</h2>
                        <div class="highlight">
                            %s
                        </div>
                        <p><strong>Destinatario:</strong> %s (%s)</p>
                        <p><strong>Fecha y hora:</strong> %s</p>
                    </div>
                    <div class="footer">
                        <p>Este es un mensaje automático del Sistema de Gestión de Horarios (SGH)</p>
                        <p>Si tiene preguntas, contacte al administrador del sistema</p>
                    </div>
                </div>
            </body>
            </html>
            """, 
            notification.getRecipientRole(),
            notification.getSubject(),
            notification.getContent(),
            notification.getRecipientName(),
            notification.getRecipientEmail(),
            LocalDateTime.now().toString()
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
        notification.setContent(""); // Se llenará desde la plantilla
        notification.setSenderName("Sistema SGH");
        notification.setIsHtml(true);
        
        return notification;
    }
    
    /**
     * Obtiene estadísticas de notificaciones
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalToday", notificationLogRepository.count());
        
        // Obtener estadísticas por tipo y estado
        stats.put("pending", notificationLogRepository.countByTypeAndStatus(
            NotificationType.STUDENT_SCHEDULE_ASSIGNMENT, NotificationStatus.PENDING) +
            notificationLogRepository.countByTypeAndStatus(
                NotificationType.TEACHER_CLASS_SCHEDULED, NotificationStatus.PENDING));
        
        stats.put("sent", notificationLogRepository.countByTypeAndStatus(
            NotificationType.STUDENT_SCHEDULE_ASSIGNMENT, NotificationStatus.SENT) +
            notificationLogRepository.countByTypeAndStatus(
                NotificationType.TEACHER_CLASS_SCHEDULED, NotificationStatus.SENT));
        
        stats.put("failed", notificationLogRepository.countByTypeAndStatus(
            NotificationType.STUDENT_SCHEDULE_ASSIGNMENT, NotificationStatus.FAILED) +
            notificationLogRepository.countByTypeAndStatus(
                NotificationType.TEACHER_CLASS_SCHEDULED, NotificationStatus.FAILED));
        
        return stats;
    }
}