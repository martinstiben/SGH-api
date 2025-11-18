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
            // Siempre usar plantillas especializadas basadas en el tipo de notificación
            // Esto asegura que se apliquen los estilos correctos
            return generateTypeBasedHtmlContent(notification);

        } catch (Exception e) {
            log.warn("Error al generar contenido HTML, usando contenido por defecto: {}", e.getMessage());
            return generateDefaultHtmlContent(notification);
        }
    }
    
    /**
     * Genera contenido HTML basado en el tipo de notificación
     */
    private String generateTypeBasedHtmlContent(NotificationDTO notification) {
        NotificationType type = NotificationType.valueOf(notification.getNotificationType());

        switch (type) {
            case TEACHER_SCHEDULE_ASSIGNED:
                return generateTeacherScheduleHtml(notification);
            case SCHEDULE_ASSIGNED:
                return generateStudentScheduleHtml(notification);
            case SYSTEM_ALERT:
                return generateSystemAlertHtml(notification);
            case SYSTEM_NOTIFICATION:
                return generateSystemNotificationHtml(notification);
            default:
                return generateDefaultHtmlContent(notification);
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
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
                <style>
                    body {
                        font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        margin: 0;
                        padding: 0;
                        background-color: #f8fafc;
                        color: #1e293b;
                        line-height: 1.6;
                        -webkit-font-smoothing: antialiased;
                    }
                    .container {
                        max-width: 600px;
                        margin: 20px auto;
                        background-color: #ffffff;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
                    }
                    .header {
                        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
                        color: white;
                        padding: 32px 30px;
                        text-align: center;
                        position: relative;
                    }
                    .header::before {
                        content: '';
                        position: absolute;
                        top: 0;
                        left: 0;
                        right: 0;
                        bottom: 0;
                        background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="25" cy="25" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="75" cy="75" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="50" cy="10" r="0.5" fill="rgba(255,255,255,0.1)"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
                        opacity: 0.3;
                    }
                    .logo {
                        font-size: 42px;
                        margin-bottom: 16px;
                        position: relative;
                        z-index: 1;
                    }
                    .header h1 {
                        font-size: 24px;
                        margin: 0 0 8px 0;
                        font-weight: 700;
                        position: relative;
                        z-index: 1;
                    }
                    .header p {
                        font-size: 16px;
                        margin: 0;
                        opacity: 0.95;
                        position: relative;
                        z-index: 1;
                    }
                    .content {
                        padding: 40px 32px;
                    }
                    .notification-card {
                        background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
                        border: 1px solid #bbf7d0;
                        border-radius: 12px;
                        padding: 32px;
                        margin-bottom: 32px;
                        border-left: 5px solid #10b981;
                        position: relative;
                    }
                    .notification-card::before {
                        content: '📚';
                        position: absolute;
                        top: -10px;
                        right: 24px;
                        font-size: 24px;
                        background: white;
                        border-radius: 50%;
                        width: 48px;
                        height: 48px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
                    }
                    .notification-title {
                        color: #065f46;
                        font-size: 22px;
                        font-weight: 700;
                        margin: 0 0 20px 0;
                        line-height: 1.3;
                    }
                    .notification-content {
                        color: #374151;
                        font-size: 16px;
                        line-height: 1.7;
                        margin-bottom: 24px;
                        white-space: pre-line;
                        font-weight: 400;
                    }
                    .highlight-box {
                        background: #ecfdf5;
                        border: 1px solid #a7f3d0;
                        border-radius: 8px;
                        padding: 20px;
                        margin: 24px 0;
                        border-left: 4px solid #10b981;
                    }
                    .highlight-box h3 {
                        color: #065f46;
                        font-size: 18px;
                        font-weight: 600;
                        margin: 0 0 12px 0;
                    }
                    .highlight-box p {
                        color: #374151;
                        margin: 0;
                        font-size: 15px;
                        line-height: 1.6;
                    }
                    .info-grid {
                        display: grid;
                        grid-template-columns: 1fr 1fr;
                        gap: 20px;
                        margin: 28px 0;
                        background: #f8fafc;
                        border-radius: 8px;
                        padding: 24px;
                    }
                    .info-item {
                        text-align: center;
                    }
                    .info-label {
                        font-size: 12px;
                        font-weight: 600;
                        color: #64748b;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        margin-bottom: 8px;
                    }
                    .info-value {
                        font-size: 16px;
                        font-weight: 600;
                        color: #1e293b;
                    }
                    .action-section {
                        background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
                        border: 1px solid #a7f3d0;
                        border-radius: 12px;
                        padding: 28px;
                        text-align: center;
                        margin-top: 32px;
                        border-left: 5px solid #10b981;
                    }
                    .action-text {
                        color: #065f46;
                        font-size: 16px;
                        margin-bottom: 20px;
                        font-weight: 500;
                        line-height: 1.5;
                    }
                    .action-button {
                        display: inline-block;
                        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
                        color: white;
                        padding: 14px 32px;
                        text-decoration: none;
                        border-radius: 8px;
                        font-weight: 600;
                        font-size: 15px;
                        transition: all 0.2s ease;
                        box-shadow: 0 4px 6px -1px rgba(16, 185, 129, 0.2);
                    }
                    .action-button:hover {
                        transform: translateY(-1px);
                        box-shadow: 0 6px 8px -1px rgba(16, 185, 129, 0.3);
                    }
                    .footer {
                        background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
                        color: white;
                        padding: 32px 30px;
                        text-align: center;
                    }
                    .footer-logo {
                        font-size: 22px;
                        font-weight: 700;
                        margin-bottom: 12px;
                        color: #10b981;
                    }
                    .footer-text {
                        font-size: 14px;
                        opacity: 0.9;
                        line-height: 1.6;
                        margin-bottom: 20px;
                    }
                    .footer-links {
                        margin-top: 20px;
                    }
                    .footer-links a {
                        color: #10b981;
                        text-decoration: none;
                        margin: 0 12px;
                        font-size: 13px;
                        font-weight: 500;
                        transition: opacity 0.2s ease;
                    }
                    .footer-links a:hover {
                        opacity: 0.8;
                    }
                    @media screen and (max-width: 600px) {
                        .container {
                            margin: 10px;
                            border-radius: 8px;
                        }
                        .header, .content, .footer {
                            padding: 24px 20px;
                        }
                        .notification-card {
                            padding: 24px 20px;
                        }
                        .info-grid {
                            grid-template-columns: 1fr;
                            gap: 16px;
                            padding: 20px;
                        }
                        .action-section {
                            padding: 24px 20px;
                        }
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
                            <h2 class="notification-title">%s</h2>
                            <div class="notification-content">%s</div>
                        </div>

                        <div class="highlight-box">
                            <h3>📋 Información Importante</h3>
                            <p>Esta notificación contiene detalles actualizados sobre tu horario académico. Te recomendamos revisar toda la información y confirmar tu asistencia a las clases programadas.</p>
                        </div>

                        <div class="info-grid">
                            <div class="info-item">
                                <div class="info-label">Destinatario</div>
                                <div class="info-value">%s</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Tipo de Usuario</div>
                                <div class="info-value">Estudiante</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Fecha de Envío</div>
                                <div class="info-value">%s</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Categoría</div>
                                <div class="info-value">Académica</div>
                            </div>
                        </div>

                        <div class="action-section">
                            <div class="action-text">¿Listo para revisar tu horario académico? Accede al sistema para ver todos los detalles de tus clases.</div>
                            <a href="#" class="action-button">👀 Ver Mi Horario Completo</a>
                        </div>
                    </div>

                    <div class="footer">
                        <div class="footer-logo">SGH</div>
                        <div class="footer-text">
                            <p><strong>Sistema de Gestión de Horarios Académicos</strong></p>
                            <p>Institución Educativa • Transformando el futuro de la educación con tecnología innovadora</p>
                        </div>
                        <div class="footer-links">
                            <a href="#">Portal Estudiantil</a>
                            <a href="#">Centro de Ayuda</a>
                            <a href="#">Contáctanos</a>
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
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
                <style>
                    body {
                        font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        margin: 0;
                        padding: 0;
                        background-color: #f8fafc;
                        color: #1e293b;
                        line-height: 1.6;
                        -webkit-font-smoothing: antialiased;
                    }
                    .container {
                        max-width: 600px;
                        margin: 20px auto;
                        background-color: #ffffff;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
                    }
                    .header {
                        background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
                        color: white;
                        padding: 32px 30px;
                        text-align: center;
                        position: relative;
                    }
                    .header::before {
                        content: '';
                        position: absolute;
                        top: 0;
                        left: 0;
                        right: 0;
                        bottom: 0;
                        background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="25" cy="25" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="75" cy="75" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="50" cy="10" r="0.5" fill="rgba(255,255,255,0.1)"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
                        opacity: 0.3;
                    }
                    .logo {
                        font-size: 42px;
                        margin-bottom: 16px;
                        position: relative;
                        z-index: 1;
                    }
                    .header h1 {
                        font-size: 24px;
                        margin: 0 0 8px 0;
                        font-weight: 700;
                        position: relative;
                        z-index: 1;
                    }
                    .header p {
                        font-size: 16px;
                        margin: 0;
                        opacity: 0.95;
                        position: relative;
                        z-index: 1;
                    }
                    .content {
                        padding: 40px 32px;
                    }
                    .notification-card {
                        background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
                        border: 1px solid #bfdbfe;
                        border-radius: 12px;
                        padding: 32px;
                        margin-bottom: 32px;
                        border-left: 5px solid #3b82f6;
                        position: relative;
                    }
                    .notification-card::before {
                        content: '👨‍🏫';
                        position: absolute;
                        top: -10px;
                        right: 24px;
                        font-size: 24px;
                        background: white;
                        border-radius: 50%;
                        width: 48px;
                        height: 48px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
                    }
                    .notification-title {
                        color: #1e40af;
                        font-size: 22px;
                        font-weight: 700;
                        margin: 0 0 20px 0;
                        line-height: 1.3;
                    }
                    .notification-content {
                        color: #374151;
                        font-size: 16px;
                        line-height: 1.7;
                        margin-bottom: 24px;
                        white-space: pre-line;
                        font-weight: 400;
                    }
                    .highlight-box {
                        background: #eff6ff;
                        border: 1px solid #bfdbfe;
                        border-radius: 8px;
                        padding: 20px;
                        margin: 24px 0;
                        border-left: 4px solid #3b82f6;
                    }
                    .highlight-box h3 {
                        color: #1e40af;
                        font-size: 18px;
                        font-weight: 600;
                        margin: 0 0 12px 0;
                    }
                    .highlight-box p {
                        color: #374151;
                        margin: 0;
                        font-size: 15px;
                        line-height: 1.6;
                    }
                    .info-grid {
                        display: grid;
                        grid-template-columns: 1fr 1fr;
                        gap: 20px;
                        margin: 28px 0;
                        background: #f8fafc;
                        border-radius: 8px;
                        padding: 24px;
                    }
                    .info-item {
                        text-align: center;
                    }
                    .info-label {
                        font-size: 12px;
                        font-weight: 600;
                        color: #64748b;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        margin-bottom: 8px;
                    }
                    .info-value {
                        font-size: 16px;
                        font-weight: 600;
                        color: #1e293b;
                    }
                    .action-section {
                        background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
                        border: 1px solid #bfdbfe;
                        border-radius: 12px;
                        padding: 28px;
                        text-align: center;
                        margin-top: 32px;
                        border-left: 5px solid #3b82f6;
                    }
                    .action-text {
                        color: #1e40af;
                        font-size: 16px;
                        margin-bottom: 20px;
                        font-weight: 500;
                        line-height: 1.5;
                    }
                    .action-button {
                        display: inline-block;
                        background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
                        color: white;
                        padding: 14px 32px;
                        text-decoration: none;
                        border-radius: 8px;
                        font-weight: 600;
                        font-size: 15px;
                        transition: all 0.2s ease;
                        box-shadow: 0 4px 6px -1px rgba(59, 130, 246, 0.2);
                    }
                    .action-button:hover {
                        transform: translateY(-1px);
                        box-shadow: 0 6px 8px -1px rgba(59, 130, 246, 0.3);
                    }
                    .footer {
                        background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
                        color: white;
                        padding: 32px 30px;
                        text-align: center;
                    }
                    .footer-logo {
                        font-size: 22px;
                        font-weight: 700;
                        margin-bottom: 12px;
                        color: #3b82f6;
                    }
                    .footer-text {
                        font-size: 14px;
                        opacity: 0.9;
                        line-height: 1.6;
                        margin-bottom: 20px;
                    }
                    .footer-links {
                        margin-top: 20px;
                    }
                    .footer-links a {
                        color: #3b82f6;
                        text-decoration: none;
                        margin: 0 12px;
                        font-size: 13px;
                        font-weight: 500;
                        transition: opacity 0.2s ease;
                    }
                    .footer-links a:hover {
                        opacity: 0.8;
                    }
                    @media screen and (max-width: 600px) {
                        .container {
                            margin: 10px;
                            border-radius: 8px;
                        }
                        .header, .content, .footer {
                            padding: 24px 20px;
                        }
                        .notification-card {
                            padding: 24px 20px;
                        }
                        .info-grid {
                            grid-template-columns: 1fr;
                            gap: 16px;
                            padding: 20px;
                        }
                        .action-section {
                            padding: 24px 20px;
                        }
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
                            <h2 class="notification-title">%s</h2>
                            <div class="notification-content">%s</div>
                        </div>

                        <div class="highlight-box">
                            <h3>📋 Información Profesional</h3>
                            <p>Esta notificación contiene información importante sobre tu horario docente y responsabilidades académicas. Te recomendamos revisar todos los detalles y confirmar tu disponibilidad.</p>
                        </div>

                        <div class="info-grid">
                            <div class="info-item">
                                <div class="info-label">Destinatario</div>
                                <div class="info-value">%s</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Tipo de Usuario</div>
                                <div class="info-value">Docente</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Fecha de Envío</div>
                                <div class="info-value">%s</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Categoría</div>
                                <div class="info-value">Académica</div>
                            </div>
                        </div>

                        <div class="action-section">
                            <div class="action-text">¿Listo para revisar tu horario docente? Accede al sistema para gestionar tus clases y responsabilidades académicas.</div>
                            <a href="#" class="action-button">🎯 Acceder al Panel Docente</a>
                        </div>
                    </div>

                    <div class="footer">
                        <div class="footer-logo">SGH</div>
                        <div class="footer-text">
                            <p><strong>Sistema de Gestión de Horarios Académicos</strong></p>
                            <p>Institución Educativa • Excelencia académica con tecnología de vanguardia</p>
                        </div>
                        <div class="footer-links">
                            <a href="#">Portal Docente</a>
                            <a href="#">Recursos Académicos</a>
                            <a href="#">Soporte Técnico</a>
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
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
                <style>
                    body {
                        font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        margin: 0;
                        padding: 0;
                        background-color: #f8fafc;
                        color: #1e293b;
                        line-height: 1.6;
                        -webkit-font-smoothing: antialiased;
                    }
                    .container {
                        max-width: 600px;
                        margin: 20px auto;
                        background-color: #ffffff;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
                    }
                    .header {
                        background: linear-gradient(135deg, #9333ea 0%, #7c3aed 100%);
                        color: white;
                        padding: 32px 30px;
                        text-align: center;
                        position: relative;
                    }
                    .header::before {
                        content: '';
                        position: absolute;
                        top: 0;
                        left: 0;
                        right: 0;
                        bottom: 0;
                        background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="25" cy="25" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="75" cy="75" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="50" cy="10" r="0.5" fill="rgba(255,255,255,0.1)"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
                        opacity: 0.3;
                    }
                    .logo {
                        font-size: 42px;
                        margin-bottom: 16px;
                        position: relative;
                        z-index: 1;
                    }
                    .header h1 {
                        font-size: 24px;
                        margin: 0 0 8px 0;
                        font-weight: 700;
                        position: relative;
                        z-index: 1;
                    }
                    .header p {
                        font-size: 16px;
                        margin: 0;
                        opacity: 0.95;
                        position: relative;
                        z-index: 1;
                    }
                    .content {
                        padding: 40px 32px;
                    }
                    .notification-card {
                        background: linear-gradient(135deg, #faf5ff 0%, #f3e8ff 100%);
                        border: 1px solid #e9d5ff;
                        border-radius: 12px;
                        padding: 32px;
                        margin-bottom: 32px;
                        border-left: 5px solid #9333ea;
                        position: relative;
                    }
                    .notification-card::before {
                        content: '👔';
                        position: absolute;
                        top: -10px;
                        right: 24px;
                        font-size: 24px;
                        background: white;
                        border-radius: 50%;
                        width: 48px;
                        height: 48px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
                    }
                    .priority-badge {
                        display: inline-flex;
                        align-items: center;
                        gap: 8px;
                        background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
                        color: white;
                        padding: 12px 20px;
                        border-radius: 24px;
                        font-size: 13px;
                        font-weight: 700;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        margin-bottom: 20px;
                        box-shadow: 0 2px 4px rgba(239, 68, 68, 0.2);
                    }
                    .notification-title {
                        color: #581c87;
                        font-size: 22px;
                        font-weight: 700;
                        margin: 0 0 20px 0;
                        line-height: 1.3;
                    }
                    .notification-content {
                        color: #374151;
                        font-size: 16px;
                        line-height: 1.7;
                        margin-bottom: 24px;
                        white-space: pre-line;
                        font-weight: 400;
                    }
                    .alert-box {
                        background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
                        border: 1px solid #fecaca;
                        border-radius: 8px;
                        padding: 20px;
                        margin: 24px 0;
                        border-left: 4px solid #ef4444;
                    }
                    .alert-box h3 {
                        color: #991b1b;
                        font-size: 18px;
                        font-weight: 600;
                        margin: 0 0 12px 0;
                    }
                    .alert-box p {
                        color: #374151;
                        margin: 0;
                        font-size: 15px;
                        line-height: 1.6;
                    }
                    .info-grid {
                        display: grid;
                        grid-template-columns: 1fr 1fr;
                        gap: 20px;
                        margin: 28px 0;
                        background: #f8fafc;
                        border-radius: 8px;
                        padding: 24px;
                    }
                    .info-item {
                        text-align: center;
                    }
                    .info-label {
                        font-size: 12px;
                        font-weight: 600;
                        color: #64748b;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        margin-bottom: 8px;
                    }
                    .info-value {
                        font-size: 16px;
                        font-weight: 600;
                        color: #1e293b;
                    }
                    .action-section {
                        background: linear-gradient(135deg, #faf5ff 0%, #f3e8ff 100%);
                        border: 1px solid #e9d5ff;
                        border-radius: 12px;
                        padding: 28px;
                        text-align: center;
                        margin-top: 32px;
                        border-left: 5px solid #9333ea;
                    }
                    .action-text {
                        color: #581c87;
                        font-size: 16px;
                        margin-bottom: 20px;
                        font-weight: 500;
                        line-height: 1.5;
                    }
                    .action-button {
                        display: inline-block;
                        background: linear-gradient(135deg, #9333ea 0%, #7c3aed 100%);
                        color: white;
                        padding: 14px 32px;
                        text-decoration: none;
                        border-radius: 8px;
                        font-weight: 600;
                        font-size: 15px;
                        transition: all 0.2s ease;
                        box-shadow: 0 4px 6px -1px rgba(147, 51, 234, 0.2);
                    }
                    .action-button:hover {
                        transform: translateY(-1px);
                        box-shadow: 0 6px 8px -1px rgba(147, 51, 234, 0.3);
                    }
                    .footer {
                        background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
                        color: white;
                        padding: 32px 30px;
                        text-align: center;
                    }
                    .footer-logo {
                        font-size: 22px;
                        font-weight: 700;
                        margin-bottom: 12px;
                        color: #9333ea;
                    }
                    .footer-text {
                        font-size: 14px;
                        opacity: 0.9;
                        line-height: 1.6;
                        margin-bottom: 20px;
                    }
                    .footer-links {
                        margin-top: 20px;
                    }
                    .footer-links a {
                        color: #9333ea;
                        text-decoration: none;
                        margin: 0 12px;
                        font-size: 13px;
                        font-weight: 500;
                        transition: opacity 0.2s ease;
                    }
                    .footer-links a:hover {
                        opacity: 0.8;
                    }
                    @media screen and (max-width: 600px) {
                        .container {
                            margin: 10px;
                            border-radius: 8px;
                        }
                        .header, .content, .footer {
                            padding: 24px 20px;
                        }
                        .notification-card {
                            padding: 24px 20px;
                        }
                        .info-grid {
                            grid-template-columns: 1fr;
                            gap: 16px;
                            padding: 20px;
                        }
                        .action-section {
                            padding: 24px 20px;
                        }
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
                            <div class="priority-badge">🚨 Alta Prioridad</div>
                            <h2 class="notification-title">%s</h2>
                            <div class="notification-content">%s</div>
                        </div>

                        <div class="alert-box">
                            <h3>⚠️ Atención Requerida</h3>
                            <p>Esta notificación contiene información crítica que requiere tu atención inmediata como director de área. Se recomienda revisar el sistema administrativo lo antes posible.</p>
                        </div>

                        <div class="info-grid">
                            <div class="info-item">
                                <div class="info-label">Destinatario</div>
                                <div class="info-value">%s</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Tipo de Usuario</div>
                                <div class="info-value">Director de Área</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Fecha de Envío</div>
                                <div class="info-value">%s</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Categoría</div>
                                <div class="info-value">Administrativa</div>
                            </div>
                        </div>

                        <div class="action-section">
                            <div class="action-text">¿Necesitas revisar esta alerta crítica? Accede al panel administrativo para gestionar la situación inmediatamente.</div>
                            <a href="#" class="action-button">🚨 Revisar Sistema Urgente</a>
                        </div>
                    </div>

                    <div class="footer">
                        <div class="footer-logo">SGH</div>
                        <div class="footer-text">
                            <p><strong>Sistema de Gestión de Horarios Académicos</strong></p>
                            <p>Institución Educativa • Liderazgo administrativo con tecnología avanzada</p>
                        </div>
                        <div class="footer-links">
                            <a href="#">Panel Administrativo</a>
                            <a href="#">Reportes Ejecutivos</a>
                            <a href="#">Soporte Prioritario</a>
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
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
                <style>
                    body {
                        font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        margin: 0;
                        padding: 0;
                        background-color: #f8fafc;
                        color: #1e293b;
                        line-height: 1.6;
                        -webkit-font-smoothing: antialiased;
                    }
                    .container {
                        max-width: 600px;
                        margin: 20px auto;
                        background-color: #ffffff;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
                    }
                    .header {
                        background: linear-gradient(135deg, #ea580c 0%, #c2410c 100%);
                        color: white;
                        padding: 32px 30px;
                        text-align: center;
                        position: relative;
                    }
                    .header::before {
                        content: '';
                        position: absolute;
                        top: 0;
                        left: 0;
                        right: 0;
                        bottom: 0;
                        background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="25" cy="25" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="75" cy="75" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="50" cy="10" r="0.5" fill="rgba(255,255,255,0.1)"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
                        opacity: 0.3;
                    }
                    .logo {
                        font-size: 42px;
                        margin-bottom: 16px;
                        position: relative;
                        z-index: 1;
                    }
                    .header h1 {
                        font-size: 24px;
                        margin: 0 0 8px 0;
                        font-weight: 700;
                        position: relative;
                        z-index: 1;
                    }
                    .header p {
                        font-size: 16px;
                        margin: 0;
                        opacity: 0.95;
                        position: relative;
                        z-index: 1;
                    }
                    .content {
                        padding: 40px 32px;
                    }
                    .notification-card {
                        background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%);
                        border: 1px solid #fed7aa;
                        border-radius: 12px;
                        padding: 32px;
                        margin-bottom: 32px;
                        border-left: 5px solid #ea580c;
                        position: relative;
                    }
                    .notification-card::before {
                        content: '⚙️';
                        position: absolute;
                        top: -10px;
                        right: 24px;
                        font-size: 24px;
                        background: white;
                        border-radius: 50%;
                        width: 48px;
                        height: 48px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
                    }
                    .system-status {
                        background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
                        border: 1px solid #fcd34d;
                        border-radius: 12px;
                        padding: 24px;
                        margin-bottom: 24px;
                        text-align: center;
                        position: relative;
                        overflow: hidden;
                    }
                    .system-status::before {
                        content: '';
                        position: absolute;
                        top: 0;
                        left: 0;
                        right: 0;
                        bottom: 0;
                        background: radial-gradient(circle at center, rgba(234, 88, 12, 0.1) 0%, transparent 70%);
                    }
                    .status-indicator {
                        display: inline-block;
                        width: 16px;
                        height: 16px;
                        background: #ea580c;
                        border-radius: 50%;
                        margin-right: 12px;
                        animation: pulse 2s infinite;
                        position: relative;
                        z-index: 1;
                    }
                    @keyframes pulse {
                        0% { box-shadow: 0 0 0 0 rgba(234, 88, 12, 0.7); }
                        70% { box-shadow: 0 0 0 10px rgba(234, 88, 12, 0); }
                        100% { box-shadow: 0 0 0 0 rgba(234, 88, 12, 0); }
                    }
                    .status-text {
                        font-size: 16px;
                        font-weight: 600;
                        color: #9a3412;
                        position: relative;
                        z-index: 1;
                    }
                    .notification-title {
                        color: #9a3412;
                        font-size: 22px;
                        font-weight: 700;
                        margin: 0 0 20px 0;
                        line-height: 1.3;
                    }
                    .notification-content {
                        color: #374151;
                        font-size: 16px;
                        line-height: 1.7;
                        margin-bottom: 24px;
                        white-space: pre-line;
                        font-weight: 400;
                    }
                    .stats-grid {
                        display: grid;
                        grid-template-columns: repeat(2, 1fr);
                        gap: 16px;
                        margin: 28px 0;
                        background: #f8fafc;
                        border-radius: 8px;
                        padding: 24px;
                    }
                    .stat-item {
                        text-align: center;
                        padding: 16px;
                        background: white;
                        border-radius: 6px;
                        border: 1px solid #e5e7eb;
                    }
                    .stat-label {
                        font-size: 12px;
                        font-weight: 600;
                        color: #64748b;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        margin-bottom: 8px;
                    }
                    .stat-value {
                        font-size: 18px;
                        font-weight: 700;
                        color: #ea580c;
                    }
                    .info-grid {
                        display: grid;
                        grid-template-columns: 1fr 1fr;
                        gap: 20px;
                        margin: 28px 0;
                        background: #f8fafc;
                        border-radius: 8px;
                        padding: 24px;
                    }
                    .info-item {
                        text-align: center;
                    }
                    .info-label {
                        font-size: 12px;
                        font-weight: 600;
                        color: #64748b;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        margin-bottom: 8px;
                    }
                    .info-value {
                        font-size: 16px;
                        font-weight: 600;
                        color: #1e293b;
                    }
                    .action-section {
                        background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%);
                        border: 1px solid #fed7aa;
                        border-radius: 12px;
                        padding: 28px;
                        text-align: center;
                        margin-top: 32px;
                        border-left: 5px solid #ea580c;
                    }
                    .action-text {
                        color: #9a3412;
                        font-size: 16px;
                        margin-bottom: 20px;
                        font-weight: 500;
                        line-height: 1.5;
                    }
                    .action-button {
                        display: inline-block;
                        background: linear-gradient(135deg, #ea580c 0%, #c2410c 100%);
                        color: white;
                        padding: 14px 32px;
                        text-decoration: none;
                        border-radius: 8px;
                        font-weight: 600;
                        font-size: 15px;
                        transition: all 0.2s ease;
                        box-shadow: 0 4px 6px -1px rgba(234, 88, 12, 0.2);
                    }
                    .action-button:hover {
                        transform: translateY(-1px);
                        box-shadow: 0 6px 8px -1px rgba(234, 88, 12, 0.3);
                    }
                    .footer {
                        background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
                        color: white;
                        padding: 32px 30px;
                        text-align: center;
                    }
                    .footer-logo {
                        font-size: 22px;
                        font-weight: 700;
                        margin-bottom: 12px;
                        color: #ea580c;
                    }
                    .footer-text {
                        font-size: 14px;
                        opacity: 0.9;
                        line-height: 1.6;
                        margin-bottom: 20px;
                    }
                    .footer-links {
                        margin-top: 20px;
                    }
                    .footer-links a {
                        color: #ea580c;
                        text-decoration: none;
                        margin: 0 12px;
                        font-size: 13px;
                        font-weight: 500;
                        transition: opacity 0.2s ease;
                    }
                    .footer-links a:hover {
                        opacity: 0.8;
                    }
                    @media screen and (max-width: 600px) {
                        .container {
                            margin: 10px;
                            border-radius: 8px;
                        }
                        .header, .content, .footer {
                            padding: 24px 20px;
                        }
                        .notification-card {
                            padding: 24px 20px;
                        }
                        .stats-grid, .info-grid {
                            grid-template-columns: 1fr;
                            gap: 16px;
                            padding: 20px;
                        }
                        .action-section {
                            padding: 24px 20px;
                        }
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
                                <span class="status-text">Notificación del Sistema de Gestión</span>
                            </div>
                            <h2 class="notification-title">%s</h2>
                            <div class="notification-content">%s</div>
                        </div>

                        <div class="stats-grid">
                            <div class="stat-item">
                                <div class="stat-label">Horarios Activos</div>
                                <div class="stat-value">127</div>
                            </div>
                            <div class="stat-item">
                                <div class="stat-label">Profesores</div>
                                <div class="stat-value">45</div>
                            </div>
                            <div class="stat-item">
                                <div class="stat-label">Aulas Ocupadas</div>
                                <div class="stat-value">23</div>
                            </div>
                            <div class="stat-item">
                                <div class="stat-label">Conflictos</div>
                                <div class="stat-value">2</div>
                            </div>
                        </div>

                        <div class="info-grid">
                            <div class="info-item">
                                <div class="info-label">Destinatario</div>
                                <div class="info-value">%s</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Tipo de Usuario</div>
                                <div class="info-value">Coordinador</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Fecha de Envío</div>
                                <div class="info-value">%s</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Categoría</div>
                                <div class="info-value">Sistema</div>
                            </div>
                        </div>

                        <div class="action-section">
                            <div class="action-text">¿Necesitas revisar el estado del sistema? Accede al panel de administración para gestionar todos los aspectos del SGH.</div>
                            <a href="#" class="action-button">⚙️ Acceder al Panel Admin</a>
                        </div>
                    </div>

                    <div class="footer">
                        <div class="footer-logo">SGH</div>
                        <div class="footer-text">
                            <p><strong>Sistema de Gestión de Horarios Académicos</strong></p>
                            <p>Institución Educativa • Control total del sistema con herramientas avanzadas</p>
                        </div>
                        <div class="footer-links">
                            <a href="#">Panel Administrativo</a>
                            <a href="#">Configuración Avanzada</a>
                            <a href="#">Soporte Técnico</a>
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
     * Plantilla HTML para asignación de horario docente
     */
    private String generateTeacherScheduleHtml(NotificationDTO notification) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>SGH - Nueva Asignación de Clase</title>
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
                <style>
                    body {
                        font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        margin: 0;
                        padding: 0;
                        background-color: #f8fafc;
                        color: #1e293b;
                        line-height: 1.6;
                        -webkit-font-smoothing: antialiased;
                    }
                    .container {
                        max-width: 600px;
                        margin: 20px auto;
                        background-color: #ffffff;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
                    }
                    .header {
                        background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
                        color: white;
                        padding: 32px 30px;
                        text-align: center;
                        position: relative;
                    }
                    .header::before {
                        content: '';
                        position: absolute;
                        top: 0;
                        left: 0;
                        right: 0;
                        bottom: 0;
                        background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="25" cy="25" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="75" cy="75" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="50" cy="10" r="0.5" fill="rgba(255,255,255,0.1)"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
                        opacity: 0.3;
                    }
                    .logo {
                        font-size: 42px;
                        margin-bottom: 16px;
                        position: relative;
                        z-index: 1;
                    }
                    .header h1 {
                        font-size: 24px;
                        margin: 0 0 8px 0;
                        font-weight: 700;
                        position: relative;
                        z-index: 1;
                    }
                    .header p {
                        font-size: 16px;
                        margin: 0;
                        opacity: 0.95;
                        position: relative;
                        z-index: 1;
                    }
                    .content {
                        padding: 40px 32px;
                    }
                    .notification-card {
                        background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
                        border: 1px solid #bfdbfe;
                        border-radius: 12px;
                        padding: 32px;
                        margin-bottom: 32px;
                        border-left: 5px solid #3b82f6;
                        position: relative;
                    }
                    .notification-card::before {
                        content: '👨‍🏫';
                        position: absolute;
                        top: -10px;
                        right: 24px;
                        font-size: 24px;
                        background: white;
                        border-radius: 50%;
                        width: 48px;
                        height: 48px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
                    }
                    .notification-title {
                        color: #1e40af;
                        font-size: 22px;
                        font-weight: 700;
                        margin: 0 0 20px 0;
                        line-height: 1.3;
                    }
                    .notification-content {
                        color: #374151;
                        font-size: 16px;
                        line-height: 1.7;
                        margin-bottom: 24px;
                        white-space: pre-line;
                        font-weight: 400;
                    }
                    .info-grid {
                        display: grid;
                        grid-template-columns: 1fr 1fr;
                        gap: 20px;
                        margin: 28px 0;
                        background: #f8fafc;
                        border-radius: 8px;
                        padding: 24px;
                    }
                    .info-item {
                        text-align: center;
                    }
                    .info-label {
                        font-size: 12px;
                        font-weight: 600;
                        color: #64748b;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        margin-bottom: 8px;
                    }
                    .info-value {
                        font-size: 16px;
                        font-weight: 600;
                        color: #1e293b;
                    }
                    .action-section {
                        background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
                        border: 1px solid #bfdbfe;
                        border-radius: 12px;
                        padding: 28px;
                        text-align: center;
                        margin-top: 32px;
                        border-left: 5px solid #3b82f6;
                    }
                    .action-text {
                        color: #1e40af;
                        font-size: 16px;
                        margin-bottom: 20px;
                        font-weight: 500;
                        line-height: 1.5;
                    }
                    .action-button {
                        display: inline-block;
                        background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
                        color: white;
                        padding: 14px 32px;
                        text-decoration: none;
                        border-radius: 8px;
                        font-weight: 600;
                        font-size: 15px;
                        transition: all 0.2s ease;
                        box-shadow: 0 4px 6px -1px rgba(59, 130, 246, 0.2);
                    }
                    .action-button:hover {
                        transform: translateY(-1px);
                        box-shadow: 0 6px 8px -1px rgba(59, 130, 246, 0.3);
                    }
                    .footer {
                        background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
                        color: white;
                        padding: 32px 30px;
                        text-align: center;
                    }
                    .footer-logo {
                        font-size: 22px;
                        font-weight: 700;
                        margin-bottom: 12px;
                        color: #3b82f6;
                    }
                    .footer-text {
                        font-size: 14px;
                        opacity: 0.9;
                        line-height: 1.6;
                        margin-bottom: 20px;
                    }
                    .footer-links {
                        margin-top: 20px;
                    }
                    .footer-links a {
                        color: #3b82f6;
                        text-decoration: none;
                        margin: 0 12px;
                        font-size: 13px;
                        font-weight: 500;
                        transition: opacity 0.2s ease;
                    }
                    .footer-links a:hover {
                        opacity: 0.8;
                    }
                    @media screen and (max-width: 600px) {
                        .container {
                            margin: 10px;
                            border-radius: 8px;
                        }
                        .header, .content, .footer {
                            padding: 24px 20px;
                        }
                        .notification-card {
                            padding: 24px 20px;
                        }
                        .info-grid {
                            grid-template-columns: 1fr;
                            gap: 16px;
                            padding: 20px;
                        }
                        .action-section {
                            padding: 24px 20px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">👨‍🏫</div>
                        <h1>Nueva Asignación de Clase</h1>
                        <p>Sistema de Gestión de Horarios</p>
                    </div>

                    <div class="content">
                        <div class="notification-card">
                            <h2 class="notification-title">%s</h2>
                            <div class="notification-content">%s</div>
                        </div>

                        <div class="info-grid">
                            <div class="info-item">
                                <div class="info-label">Destinatario</div>
                                <div class="info-value">%s</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Tipo de Usuario</div>
                                <div class="info-value">Docente</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Fecha de Envío</div>
                                <div class="info-value">%s</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Categoría</div>
                                <div class="info-value">Académica</div>
                            </div>
                        </div>

                        <div class="action-section">
                            <div class="action-text">¿Listo para revisar tu nueva asignación docente? Accede al sistema para gestionar tus clases.</div>
                            <a href="#" class="action-button">🎯 Acceder al Sistema</a>
                        </div>
                    </div>

                    <div class="footer">
                        <div class="footer-logo">SGH</div>
                        <div class="footer-text">
                            <p><strong>Sistema de Gestión de Horarios Académicos</strong></p>
                            <p>Institución Educativa • Excelencia académica con tecnología de vanguardia</p>
                        </div>
                        <div class="footer-links">
                            <a href="#">Portal Docente</a>
                            <a href="#">Recursos Académicos</a>
                            <a href="#">Soporte Técnico</a>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
            notification.getSubject(),
            notification.getContent(),
            notification.getRecipientName(),
            LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );
    }

    /**
     * Plantilla HTML para asignación de horario estudiantil
     */
    private String generateStudentScheduleHtml(NotificationDTO notification) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>SGH - Horario Asignado</title>
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
                <style>
                    body {
                        font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        margin: 0;
                        padding: 0;
                        background-color: #f8fafc;
                        color: #1e293b;
                        line-height: 1.6;
                        -webkit-font-smoothing: antialiased;
                    }
                    .container {
                        max-width: 600px;
                        margin: 20px auto;
                        background-color: #ffffff;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
                    }
                    .header {
                        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
                        color: white;
                        padding: 32px 30px;
                        text-align: center;
                        position: relative;
                    }
                    .header::before {
                        content: '';
                        position: absolute;
                        top: 0;
                        left: 0;
                        right: 0;
                        bottom: 0;
                        background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="25" cy="25" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="75" cy="75" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="50" cy="10" r="0.5" fill="rgba(255,255,255,0.1)"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
                        opacity: 0.3;
                    }
                    .logo {
                        font-size: 42px;
                        margin-bottom: 16px;
                        position: relative;
                        z-index: 1;
                    }
                    .header h1 {
                        font-size: 24px;
                        margin: 0 0 8px 0;
                        font-weight: 700;
                        position: relative;
                        z-index: 1;
                    }
                    .header p {
                        font-size: 16px;
                        margin: 0;
                        opacity: 0.95;
                        position: relative;
                        z-index: 1;
                    }
                    .content {
                        padding: 40px 32px;
                    }
                    .notification-card {
                        background: linear-gradient(135deg, #f0fdf4 0%, #dcfce7 100%);
                        border: 1px solid #bbf7d0;
                        border-radius: 12px;
                        padding: 32px;
                        margin-bottom: 32px;
                        border-left: 5px solid #10b981;
                        position: relative;
                    }
                    .notification-card::before {
                        content: '📚';
                        position: absolute;
                        top: -10px;
                        right: 24px;
                        font-size: 24px;
                        background: white;
                        border-radius: 50%;
                        width: 48px;
                        height: 48px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
                    }
                    .notification-title {
                        color: #065f46;
                        font-size: 22px;
                        font-weight: 700;
                        margin: 0 0 20px 0;
                        line-height: 1.3;
                    }
                    .notification-content {
                        color: #374151;
                        font-size: 16px;
                        line-height: 1.7;
                        margin-bottom: 24px;
                        white-space: pre-line;
                        font-weight: 400;
                    }
                    .info-grid {
                        display: grid;
                        grid-template-columns: 1fr 1fr;
                        gap: 20px;
                        margin: 28px 0;
                        background: #f8fafc;
                        border-radius: 8px;
                        padding: 24px;
                    }
                    .info-item {
                        text-align: center;
                    }
                    .info-label {
                        font-size: 12px;
                        font-weight: 600;
                        color: #64748b;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        margin-bottom: 8px;
                    }
                    .info-value {
                        font-size: 16px;
                        font-weight: 600;
                        color: #1e293b;
                    }
                    .action-section {
                        background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
                        border: 1px solid #a7f3d0;
                        border-radius: 12px;
                        padding: 28px;
                        text-align: center;
                        margin-top: 32px;
                        border-left: 5px solid #10b981;
                    }
                    .action-text {
                        color: #065f46;
                        font-size: 16px;
                        margin-bottom: 20px;
                        font-weight: 500;
                        line-height: 1.5;
                    }
                    .action-button {
                        display: inline-block;
                        background: linear-gradient(135deg, #10b981 0%, #059669 100%);
                        color: white;
                        padding: 14px 32px;
                        text-decoration: none;
                        border-radius: 8px;
                        font-weight: 600;
                        font-size: 15px;
                        transition: all 0.2s ease;
                        box-shadow: 0 4px 6px -1px rgba(16, 185, 129, 0.2);
                    }
                    .action-button:hover {
                        transform: translateY(-1px);
                        box-shadow: 0 6px 8px -1px rgba(16, 185, 129, 0.3);
                    }
                    .footer {
                        background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
                        color: white;
                        padding: 32px 30px;
                        text-align: center;
                    }
                    .footer-logo {
                        font-size: 22px;
                        font-weight: 700;
                        margin-bottom: 12px;
                        color: #10b981;
                    }
                    .footer-text {
                        font-size: 14px;
                        opacity: 0.9;
                        line-height: 1.6;
                        margin-bottom: 20px;
                    }
                    .footer-links {
                        margin-top: 20px;
                    }
                    .footer-links a {
                        color: #10b981;
                        text-decoration: none;
                        margin: 0 12px;
                        font-size: 13px;
                        font-weight: 500;
                        transition: opacity 0.2s ease;
                    }
                    .footer-links a:hover {
                        opacity: 0.8;
                    }
                    @media screen and (max-width: 600px) {
                        .container {
                            margin: 10px;
                            border-radius: 8px;
                        }
                        .header, .content, .footer {
                            padding: 24px 20px;
                        }
                        .notification-card {
                            padding: 24px 20px;
                        }
                        .info-grid {
                            grid-template-columns: 1fr;
                            gap: 16px;
                            padding: 20px;
                        }
                        .action-section {
                            padding: 24px 20px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">📚</div>
                        <h1>Horario Asignado</h1>
                        <p>Sistema de Gestión de Horarios</p>
                    </div>

                    <div class="content">
                        <div class="notification-card">
                            <h2 class="notification-title">%s</h2>
                            <div class="notification-content">%s</div>
                        </div>

                        <div class="info-grid">
                            <div class="info-item">
                                <div class="info-label">Destinatario</div>
                                <div class="info-value">%s</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Tipo de Usuario</div>
                                <div class="info-value">Estudiante</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Fecha de Envío</div>
                                <div class="info-value">%s</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Categoría</div>
                                <div class="info-value">Académica</div>
                            </div>
                        </div>

                        <div class="action-section">
                            <div class="action-text">¿Listo para revisar tu horario académico? Accede al sistema para ver todos los detalles de tus clases.</div>
                            <a href="#" class="action-button">👀 Ver Mi Horario Completo</a>
                        </div>
                    </div>

                    <div class="footer">
                        <div class="footer-logo">SGH</div>
                        <div class="footer-text">
                            <p><strong>Sistema de Gestión de Horarios Académicos</strong></p>
                            <p>Institución Educativa • Transformando el futuro de la educación con tecnología innovadora</p>
                        </div>
                        <div class="footer-links">
                            <a href="#">Portal Estudiantil</a>
                            <a href="#">Centro de Ayuda</a>
                            <a href="#">Contáctanos</a>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
            notification.getSubject(),
            notification.getContent(),
            notification.getRecipientName(),
            LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );
    }

    /**
     * Plantilla HTML para alertas del sistema (Directores)
     */
    private String generateSystemAlertHtml(NotificationDTO notification) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>SGH - Alerta Crítica del Sistema</title>
                <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
                <style>
                    body {
                        font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        margin: 0;
                        padding: 0;
                        background-color: #f8fafc;
                        color: #1e293b;
                        line-height: 1.6;
                        -webkit-font-smoothing: antialiased;
                    }
                    .container {
                        max-width: 600px;
                        margin: 20px auto;
                        background-color: #ffffff;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
                    }
                    .header {
                        background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
                        color: white;
                        padding: 32px 30px;
                        text-align: center;
                        position: relative;
                    }
                    .header::before {
                        content: '';
                        position: absolute;
                        top: 0;
                        left: 0;
                        right: 0;
                        bottom: 0;
                        background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="25" cy="25" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="75" cy="75" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="50" cy="10" r="0.5" fill="rgba(255,255,255,0.1)"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
                        opacity: 0.3;
                    }
                    .logo {
                        font-size: 42px;
                        margin-bottom: 16px;
                        position: relative;
                        z-index: 1;
                    }
                    .header h1 {
                        font-size: 24px;
                        margin: 0 0 8px 0;
                        font-weight: 700;
                        position: relative;
                        z-index: 1;
                    }
                    .header p {
                        font-size: 16px;
                        margin: 0;
                        opacity: 0.95;
                        position: relative;
                        z-index: 1;
                    }
                    .alert-badge {
                        background: linear-gradient(135deg, #fbbf24 0%, #f59e0b 100%);
                        color: #92400e;
                        padding: 12px 20px;
                        border-radius: 24px;
                        font-size: 13px;
                        font-weight: 700;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        display: inline-block;
                        margin-top: 16px;
                        box-shadow: 0 2px 4px rgba(251, 191, 36, 0.2);
                        position: relative;
                        z-index: 1;
                    }
                    .content {
                        padding: 40px 32px;
                    }
                    .notification-card {
                        background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
                        border: 1px solid #fecaca;
                        border-radius: 12px;
                        padding: 32px;
                        margin-bottom: 32px;
                        border-left: 5px solid #ef4444;
                        position: relative;
                    }
                    .notification-card::before {
                        content: '🚨';
                        position: absolute;
                        top: -10px;
                        right: 24px;
                        font-size: 24px;
                        background: white;
                        border-radius: 50%;
                        width: 48px;
                        height: 48px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
                    }
                    .urgent-indicator {
                        background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
                        color: white;
                        padding: 12px 20px;
                        border-radius: 20px;
                        font-size: 14px;
                        font-weight: 700;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        display: inline-block;
                        margin-bottom: 20px;
                        box-shadow: 0 2px 4px rgba(220, 38, 38, 0.2);
                    }
                    .notification-title {
                        color: #991b1b;
                        font-size: 22px;
                        font-weight: 700;
                        margin: 0 0 20px 0;
                        line-height: 1.3;
                    }
                    .notification-content {
                        color: #374151;
                        font-size: 16px;
                        line-height: 1.7;
                        margin-bottom: 24px;
                        white-space: pre-line;
                        font-weight: 400;
                    }
                    .requirements-box {
                        background: #fef3c7;
                        border: 1px solid #fcd34d;
                        border-radius: 8px;
                        padding: 20px;
                        margin: 24px 0;
                        border-left: 4px solid #f59e0b;
                    }
                    .requirements-box h3 {
                        color: #92400e;
                        font-size: 16px;
                        font-weight: 600;
                        margin: 0 0 8px 0;
                    }
                    .requirements-box p {
                        color: #374151;
                        margin: 0;
                        font-size: 14px;
                        line-height: 1.5;
                    }
                    .info-grid {
                        display: grid;
                        grid-template-columns: 1fr 1fr;
                        gap: 20px;
                        margin: 28px 0;
                        background: #f8fafc;
                        border-radius: 8px;
                        padding: 24px;
                    }
                    .info-item {
                        text-align: center;
                    }
                    .info-label {
                        font-size: 12px;
                        font-weight: 600;
                        color: #64748b;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        margin-bottom: 8px;
                    }
                    .info-value {
                        font-size: 16px;
                        font-weight: 600;
                        color: #1e293b;
                    }
                    .action-section {
                        background: linear-gradient(135deg, #fef2f2 0%, #fee2e2 100%);
                        border: 1px solid #fecaca;
                        border-radius: 12px;
                        padding: 28px;
                        text-align: center;
                        margin-top: 32px;
                        border-left: 5px solid #ef4444;
                    }
                    .action-text {
                        color: #991b1b;
                        font-size: 16px;
                        margin-bottom: 20px;
                        font-weight: 500;
                        line-height: 1.5;
                    }
                    .action-button {
                        display: inline-block;
                        background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
                        color: white;
                        padding: 14px 32px;
                        text-decoration: none;
                        border-radius: 8px;
                        font-weight: 600;
                        font-size: 15px;
                        transition: all 0.2s ease;
                        box-shadow: 0 4px 6px -1px rgba(239, 68, 68, 0.2);
                    }
                    .action-button:hover {
                        transform: translateY(-1px);
                        box-shadow: 0 6px 8px -1px rgba(239, 68, 68, 0.3);
                    }
                    .footer {
                        background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
                        color: white;
                        padding: 32px 30px;
                        text-align: center;
                    }
                    .footer-logo {
                        font-size: 22px;
                        font-weight: 700;
                        margin-bottom: 12px;
                        color: #ef4444;
                    }
                    .footer-text {
                        font-size: 14px;
                        opacity: 0.9;
                        line-height: 1.6;
                        margin-bottom: 20px;
                    }
                    .footer-links {
                        margin-top: 20px;
                    }
                    .footer-links a {
                        color: #ef4444;
                        text-decoration: none;
                        margin: 0 12px;
                        font-size: 13px;
                        font-weight: 500;
                        transition: opacity 0.2s ease;
                    }
                    .footer-links a:hover {
                        opacity: 0.8;
                    }
                    @media screen and (max-width: 600px) {
                        .container {
                            margin: 10px;
                            border-radius: 8px;
                        }
                        .header, .content, .footer {
                            padding: 24px 20px;
                        }
                        .notification-card {
                            padding: 24px 20px;
                        }
                        .info-grid {
                            grid-template-columns: 1fr;
                            gap: 16px;
                            padding: 20px;
                        }
                        .action-section {
                            padding: 24px 20px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">🚨</div>
                        <h1>Alerta Crítica del Sistema</h1>
                        <p>Sistema de Gestión de Horarios</p>
                        <div class="alert-badge">⚠️ Atención Inmediata</div>
                    </div>

                    <div class="content">
                        <div class="notification-card">
                            <div class="urgent-indicator">🔴 Alerta de Alta Prioridad</div>
                            <h2 class="notification-title">%s</h2>
                            <div class="notification-content">%s</div>
                        </div>

                        <div class="requirements-box">
                            <h3>⚡ Acción Requerida</h3>
                            <p>Esta alerta requiere atención inmediata del director de área. Se recomienda revisar el sistema administrativo lo antes posible para resolver la situación crítica detectada.</p>
                        </div>

                        <div class="info-grid">
                            <div class="info-item">
                                <div class="info-label">Destinatario</div>
                                <div class="info-value">%s</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Tipo de Usuario</div>
                                <div class="info-value">Director de Área</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Fecha de Envío</div>
                                <div class="info-value">%s</div>
                            </div>
                            <div class="info-item">
                                <div class="info-label">Prioridad</div>
                                <div class="info-value">Crítica</div>
                            </div>
                        </div>

                        <div class="action-section">
                            <div class="action-text">¿Necesitas revisar esta alerta crítica inmediatamente? Accede al panel administrativo para gestionar la situación de emergencia.</div>
                            <a href="#" class="action-button">🚨 Revisar Sistema Urgente</a>
                        </div>
                    </div>

                    <div class="footer">
                        <div class="footer-logo">SGH</div>
                        <div class="footer-text">
                            <p><strong>Sistema de Gestión de Horarios Académicos</strong></p>
                            <p>Institución Educativa • Liderazgo administrativo con tecnología avanzada</p>
                        </div>
                        <div class="footer-links">
                            <a href="#">Panel Administrativo</a>
                            <a href="#">Reportes de Emergencia</a>
                            <a href="#">Soporte Prioritario</a>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """,
            notification.getSubject(),
            notification.getContent(),
            notification.getRecipientName(),
            LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );
    }

    /**
     * Plantilla HTML para notificaciones del sistema (Coordinadores)
     */
    private String generateSystemNotificationHtml(NotificationDTO notification) {
        return String.format("""
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>SGH - Notificación del Sistema</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 20px auto; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                    .header { background: #FF9800; color: white; padding: 30px 25px; text-align: center; }
                    .header h1 { margin: 0; font-size: 24px; }
                    .content { padding: 30px 25px; }
                    .notification-card { background: #fff3e0; border-left: 4px solid #FF9800; padding: 20px; margin: 20px 0; border-radius: 4px; }
                    .footer { background: #2c3e50; color: white; padding: 20px; text-align: center; font-size: 12px; }
                    .action-button { display: inline-block; background: #FF9800; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; margin-top: 15px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>📢 Notificación del Sistema</h1>
                        <p>Sistema de Gestión de Horarios</p>
                    </div>
                    <div class="content">
                        <div class="notification-card">
                            <h2>%s</h2>
                            <p>%s</p>
                            <p><strong>Destinatario:</strong> %s</p>
                            <p><strong>Fecha:</strong> %s</p>
                        </div>
                        <a href="#" class="action-button">Acceder al Panel</a>
                    </div>
                    <div class="footer">
                        <p>Sistema de Gestión de Horarios Académicos - Institución Educativa</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            notification.getSubject(),
            notification.getContent(),
            notification.getRecipientName(),
            LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );
    }

    /**
     * Genera contenido HTML por defecto
     */
    private String generateDefaultHtmlContent(NotificationDTO notification) {
        return generateSystemNotificationHtml(notification);
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
