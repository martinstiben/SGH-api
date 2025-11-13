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
    public void validateAndPrepareNotification(NotificationDTO notification) {
        log.info("Validando notificación para: {}", notification.getRecipientEmail());

        // Validar que el tipo de notificación sea válido para el rol
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
                // Buscar el log más reciente creado en los últimos 5 minutos
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
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>SGH - Notificación para Estudiante</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        min-height: 100vh;
                        padding: 20px;
                    }
                    .container {
                        max-width: 650px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 20px;
                        overflow: hidden;
                        box-shadow: 0 20px 40px rgba(0,0,0,0.1);
                        animation: slideIn 0.6s ease-out;
                    }
                    @keyframes slideIn {
                        from { transform: translateY(-30px); opacity: 0; }
                        to { transform: translateY(0); opacity: 1; }
                    }
                    .header {
                        background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
                        color: white;
                        padding: 40px 30px;
                        text-align: center;
                        position: relative;
                        overflow: hidden;
                    }
                    .header::before {
                        content: '';
                        position: absolute;
                        top: -50%;
                        left: -50%;
                        width: 200%;
                        height: 200%;
                        background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="20" cy="20" r="2" fill="rgba(255,255,255,0.1)"/><circle cx="80" cy="80" r="2" fill="rgba(255,255,255,0.1)"/><circle cx="40" cy="60" r="1" fill="rgba(255,255,255,0.1)"/></svg>');
                        animation: float 6s ease-in-out infinite;
                    }
                    @keyframes float {
                        0%, 100% { transform: translateY(0px) rotate(0deg); }
                        50% { transform: translateY(-10px) rotate(180deg); }
                    }
                    .logo {
                        width: 80px;
                        height: 80px;
                        background: rgba(255,255,255,0.2);
                        border-radius: 50%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        margin: 0 auto 20px;
                        font-size: 36px;
                        backdrop-filter: blur(10px);
                        border: 2px solid rgba(255,255,255,0.3);
                    }
                    .header h1 {
                        font-size: 28px;
                        font-weight: 700;
                        margin-bottom: 10px;
                        text-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .header p {
                        font-size: 18px;
                        opacity: 0.9;
                        font-weight: 300;
                    }
                    .content {
                        padding: 40px 30px;
                        background: #fafafa;
                    }
                    .notification-card {
                        background: white;
                        border-radius: 15px;
                        padding: 30px;
                        margin-bottom: 30px;
                        border-left: 5px solid #4CAF50;
                        box-shadow: 0 8px 25px rgba(0,0,0,0.08);
                        transition: transform 0.3s ease;
                    }
                    .notification-card:hover {
                        transform: translateY(-2px);
                    }
                    .notification-title {
                        color: #2c3e50;
                        font-size: 22px;
                        font-weight: 600;
                        margin-bottom: 20px;
                        display: flex;
                        align-items: center;
                    }
                    .notification-title::before {
                        content: '📚';
                        margin-right: 10px;
                        font-size: 24px;
                    }
                    .notification-content {
                        color: #555;
                        line-height: 1.7;
                        font-size: 16px;
                        margin-bottom: 25px;
                    }
                    .info-grid {
                        display: grid;
                        grid-template-columns: 1fr 1fr;
                        gap: 20px;
                        margin-bottom: 25px;
                    }
                    .info-item {
                        background: #f8f9fa;
                        padding: 15px;
                        border-radius: 10px;
                        border-left: 3px solid #4CAF50;
                    }
                    .info-label {
                        font-size: 12px;
                        color: #666;
                        text-transform: uppercase;
                        font-weight: 600;
                        letter-spacing: 0.5px;
                        margin-bottom: 5px;
                    }
                    .info-value {
                        font-size: 14px;
                        color: #2c3e50;
                        font-weight: 500;
                    }
                    .action-section {
                        background: linear-gradient(135deg, #e8f5e8 0%, #f0f9f0 100%);
                        padding: 25px;
                        border-radius: 12px;
                        text-align: center;
                        border: 1px solid #d4edda;
                    }
                    .action-text {
                        color: #155724;
                        font-size: 16px;
                        margin-bottom: 15px;
                        font-weight: 500;
                    }
                    .action-button {
                        display: inline-block;
                        background: #28a745;
                        color: white;
                        padding: 12px 30px;
                        text-decoration: none;
                        border-radius: 25px;
                        font-weight: 600;
                        transition: all 0.3s ease;
                        box-shadow: 0 4px 15px rgba(40, 167, 69, 0.3);
                    }
                    .action-button:hover {
                        background: #218838;
                        transform: translateY(-2px);
                        box-shadow: 0 6px 20px rgba(40, 167, 69, 0.4);
                    }
                    .footer {
                        background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
                        color: white;
                        padding: 30px;
                        text-align: center;
                    }
                    .footer-logo {
                        font-size: 24px;
                        font-weight: 700;
                        margin-bottom: 10px;
                        color: #4CAF50;
                    }
                    .footer-text {
                        font-size: 14px;
                        opacity: 0.8;
                        line-height: 1.6;
                    }
                    .footer-links {
                        margin-top: 20px;
                    }
                    .footer-links a {
                        color: #4CAF50;
                        text-decoration: none;
                        margin: 0 15px;
                        font-weight: 500;
                        transition: opacity 0.3s ease;
                    }
                    .footer-links a:hover {
                        opacity: 0.7;
                    }
                    @media (max-width: 600px) {
                        .container { margin: 10px; }
                        .info-grid { grid-template-columns: 1fr; }
                        .header { padding: 30px 20px; }
                        .content { padding: 30px 20px; }
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
                            <div class="notification-content">
                                %s
                            </div>

                            <div class="info-grid">
                                <div class="info-item">
                                    <div class="info-label">Destinatario</div>
                                    <div class="info-value">%s</div>
                                </div>
                                <div class="info-item">
                                    <div class="info-label">Rol</div>
                                    <div class="info-value">Estudiante</div>
                                </div>
                                <div class="info-item">
                                    <div class="info-label">Fecha y Hora</div>
                                    <div class="info-value">%s</div>
                                </div>
                                <div class="info-item">
                                    <div class="info-label">Categoría</div>
                                    <div class="info-value">Información Académica</div>
                                </div>
                            </div>

                            <div class="action-section">
                                <div class="action-text">
                                    📚 Esta notificación contiene información importante sobre tu horario académico
                                </div>
                                <a href="#" class="action-button">
                                    Acceder al Sistema
                                </a>
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
     * Plantilla HTML para maestros
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
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        min-height: 100vh;
                        padding: 20px;
                    }
                    .container {
                        max-width: 650px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 20px;
                        overflow: hidden;
                        box-shadow: 0 20px 40px rgba(0,0,0,0.1);
                        animation: slideIn 0.6s ease-out;
                    }
                    @keyframes slideIn {
                        from { transform: translateY(-30px); opacity: 0; }
                        to { transform: translateY(0); opacity: 1; }
                    }
                    .header {
                        background: linear-gradient(135deg, #2196F3 0%, #1976D2 100%);
                        color: white;
                        padding: 40px 30px;
                        text-align: center;
                        position: relative;
                        overflow: hidden;
                    }
                    .header::before {
                        content: '';
                        position: absolute;
                        top: -50%;
                        left: -50%;
                        width: 200%;
                        height: 200%;
                        background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="20" cy="20" r="2" fill="rgba(255,255,255,0.1)"/><circle cx="80" cy="80" r="2" fill="rgba(255,255,255,0.1)"/><circle cx="40" cy="60" r="1" fill="rgba(255,255,255,0.1)"/></svg>');
                        animation: float 6s ease-in-out infinite;
                    }
                    @keyframes float {
                        0%, 100% { transform: translateY(0px) rotate(0deg); }
                        50% { transform: translateY(-10px) rotate(180deg); }
                    }
                    .logo {
                        width: 80px;
                        height: 80px;
                        background: rgba(255,255,255,0.2);
                        border-radius: 50%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        margin: 0 auto 20px;
                        font-size: 36px;
                        backdrop-filter: blur(10px);
                        border: 2px solid rgba(255,255,255,0.3);
                    }
                    .header h1 {
                        font-size: 28px;
                        font-weight: 700;
                        margin-bottom: 10px;
                        text-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .header p {
                        font-size: 18px;
                        opacity: 0.9;
                        font-weight: 300;
                    }
                    .content {
                        padding: 40px 30px;
                        background: #fafafa;
                    }
                    .notification-card {
                        background: white;
                        border-radius: 15px;
                        padding: 30px;
                        margin-bottom: 30px;
                        border-left: 5px solid #2196F3;
                        box-shadow: 0 8px 25px rgba(0,0,0,0.08);
                        transition: transform 0.3s ease;
                    }
                    .notification-card:hover {
                        transform: translateY(-2px);
                    }
                    .notification-title {
                        color: #2c3e50;
                        font-size: 22px;
                        font-weight: 600;
                        margin-bottom: 20px;
                        display: flex;
                        align-items: center;
                    }
                    .notification-title::before {
                        content: '👨‍🏫';
                        margin-right: 10px;
                        font-size: 24px;
                    }
                    .notification-content {
                        color: #555;
                        line-height: 1.7;
                        font-size: 16px;
                        margin-bottom: 25px;
                    }
                    .info-grid {
                        display: grid;
                        grid-template-columns: 1fr 1fr;
                        gap: 20px;
                        margin-bottom: 25px;
                    }
                    .info-item {
                        background: #f8f9fa;
                        padding: 15px;
                        border-radius: 10px;
                        border-left: 3px solid #2196F3;
                    }
                    .info-label {
                        font-size: 12px;
                        color: #666;
                        text-transform: uppercase;
                        font-weight: 600;
                        letter-spacing: 0.5px;
                        margin-bottom: 5px;
                    }
                    .info-value {
                        font-size: 14px;
                        color: #2c3e50;
                        font-weight: 500;
                    }
                    .action-section {
                        background: linear-gradient(135deg, #e3f2fd 0%, #f0f8ff 100%);
                        padding: 25px;
                        border-radius: 12px;
                        text-align: center;
                        border: 1px solid #b3e5fc;
                    }
                    .action-text {
                        color: #0d47a1;
                        font-size: 16px;
                        margin-bottom: 15px;
                        font-weight: 500;
                    }
                    .action-button {
                        display: inline-block;
                        background: #1976D2;
                        color: white;
                        padding: 12px 30px;
                        text-decoration: none;
                        border-radius: 25px;
                        font-weight: 600;
                        transition: all 0.3s ease;
                        box-shadow: 0 4px 15px rgba(25, 118, 210, 0.3);
                    }
                    .action-button:hover {
                        background: #1565C0;
                        transform: translateY(-2px);
                        box-shadow: 0 6px 20px rgba(25, 118, 210, 0.4);
                    }
                    .footer {
                        background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
                        color: white;
                        padding: 30px;
                        text-align: center;
                    }
                    .footer-logo {
                        font-size: 24px;
                        font-weight: 700;
                        margin-bottom: 10px;
                        color: #2196F3;
                    }
                    .footer-text {
                        font-size: 14px;
                        opacity: 0.8;
                        line-height: 1.6;
                    }
                    .footer-links {
                        margin-top: 20px;
                    }
                    .footer-links a {
                        color: #2196F3;
                        text-decoration: none;
                        margin: 0 15px;
                        font-weight: 500;
                        transition: opacity 0.3s ease;
                    }
                    .footer-links a:hover {
                        opacity: 0.7;
                    }
                    @media (max-width: 600px) {
                        .container { margin: 10px; }
                        .info-grid { grid-template-columns: 1fr; }
                        .header { padding: 30px 20px; }
                        .content { padding: 30px 20px; }
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
                            <div class="notification-content">
                                Se le ha asignado un nuevo horario de clases. Consulte los detalles actualizados en su portal docente.
                            </div>

                            <div class="info-grid">
                                <div class="info-item">
                                    <div class="info-label">Destinatario</div>
                                    <div class="info-value">%s</div>
                                </div>
                                <div class="info-item">
                                    <div class="info-label">Rol</div>
                                    <div class="info-value">Docente</div>
                                </div>
                                <div class="info-item">
                                    <div class="info-label">Fecha y Hora</div>
                                    <div class="info-value">%s</div>
                                </div>
                                <div class="info-item">
                                    <div class="info-label">Categoría</div>
                                    <div class="info-value">Gestión Académica</div>
                                </div>
                            </div>

                            <div class="action-section">
                                <div class="action-text">
                                    📋 Esta notificación contiene información importante sobre tu gestión académica
                                </div>
                                <a href="#" class="action-button">
                                    Acceder al Sistema
                                </a>
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
     * Plantilla HTML para directores
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
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        min-height: 100vh;
                        padding: 20px;
                    }
                    .container {
                        max-width: 650px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 20px;
                        overflow: hidden;
                        box-shadow: 0 20px 40px rgba(0,0,0,0.1);
                        animation: slideIn 0.6s ease-out;
                    }
                    @keyframes slideIn {
                        from { transform: translateY(-30px); opacity: 0; }
                        to { transform: translateY(0); opacity: 1; }
                    }
                    .header {
                        background: linear-gradient(135deg, #9C27B0 0%, #7B1FA2 100%);
                        color: white;
                        padding: 40px 30px;
                        text-align: center;
                        position: relative;
                        overflow: hidden;
                    }
                    .header::before {
                        content: '';
                        position: absolute;
                        top: -50%;
                        left: -50%;
                        width: 200%;
                        height: 200%;
                        background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="20" cy="20" r="2" fill="rgba(255,255,255,0.1)"/><circle cx="80" cy="80" r="2" fill="rgba(255,255,255,0.1)"/><circle cx="40" cy="60" r="1" fill="rgba(255,255,255,0.1)"/></svg>');
                        animation: float 6s ease-in-out infinite;
                    }
                    @keyframes float {
                        0%, 100% { transform: translateY(0px) rotate(0deg); }
                        50% { transform: translateY(-10px) rotate(180deg); }
                    }
                    .logo {
                        width: 80px;
                        height: 80px;
                        background: rgba(255,255,255,0.2);
                        border-radius: 50%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        margin: 0 auto 20px;
                        font-size: 36px;
                        backdrop-filter: blur(10px);
                        border: 2px solid rgba(255,255,255,0.3);
                    }
                    .header h1 {
                        font-size: 28px;
                        font-weight: 700;
                        margin-bottom: 10px;
                        text-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .header p {
                        font-size: 18px;
                        opacity: 0.9;
                        font-weight: 300;
                    }
                    .content {
                        padding: 40px 30px;
                        background: #fafafa;
                    }
                    .notification-card {
                        background: white;
                        border-radius: 15px;
                        padding: 30px;
                        margin-bottom: 30px;
                        border-left: 5px solid #9C27B0;
                        box-shadow: 0 8px 25px rgba(0,0,0,0.08);
                        transition: transform 0.3s ease;
                    }
                    .notification-card:hover {
                        transform: translateY(-2px);
                    }
                    .notification-title {
                        color: #2c3e50;
                        font-size: 22px;
                        font-weight: 600;
                        margin-bottom: 20px;
                        display: flex;
                        align-items: center;
                    }
                    .notification-title::before {
                        content: '👔';
                        margin-right: 10px;
                        font-size: 24px;
                    }
                    .notification-content {
                        color: #555;
                        line-height: 1.7;
                        font-size: 16px;
                        margin-bottom: 25px;
                    }
                    .info-grid {
                        display: grid;
                        grid-template-columns: 1fr 1fr;
                        gap: 20px;
                        margin-bottom: 25px;
                    }
                    .info-item {
                        background: #f8f9fa;
                        padding: 15px;
                        border-radius: 10px;
                        border-left: 3px solid #9C27B0;
                    }
                    .info-label {
                        font-size: 12px;
                        color: #666;
                        text-transform: uppercase;
                        font-weight: 600;
                        letter-spacing: 0.5px;
                        margin-bottom: 5px;
                    }
                    .info-value {
                        font-size: 14px;
                        color: #2c3e50;
                        font-weight: 500;
                    }
                    .priority-badge {
                        display: inline-block;
                        background: linear-gradient(135deg, #FF5722 0%, #E64A19 100%);
                        color: white;
                        padding: 8px 16px;
                        border-radius: 20px;
                        font-size: 12px;
                        font-weight: 600;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        margin-bottom: 20px;
                    }
                    .action-section {
                        background: linear-gradient(135deg, #f3e5f5 0%, #faf0fb 100%);
                        padding: 25px;
                        border-radius: 12px;
                        text-align: center;
                        border: 1px solid #e1bee7;
                    }
                    .action-text {
                        color: #4a148c;
                        font-size: 16px;
                        margin-bottom: 15px;
                        font-weight: 500;
                    }
                    .action-button {
                        display: inline-block;
                        background: #7B1FA2;
                        color: white;
                        padding: 12px 30px;
                        text-decoration: none;
                        border-radius: 25px;
                        font-weight: 600;
                        transition: all 0.3s ease;
                        box-shadow: 0 4px 15px rgba(123, 31, 162, 0.3);
                    }
                    .action-button:hover {
                        background: #6A1B9A;
                        transform: translateY(-2px);
                        box-shadow: 0 6px 20px rgba(123, 31, 162, 0.4);
                    }
                    .footer {
                        background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
                        color: white;
                        padding: 30px;
                        text-align: center;
                    }
                    .footer-logo {
                        font-size: 24px;
                        font-weight: 700;
                        margin-bottom: 10px;
                        color: #9C27B0;
                    }
                    .footer-text {
                        font-size: 14px;
                        opacity: 0.8;
                        line-height: 1.6;
                    }
                    .footer-links {
                        margin-top: 20px;
                    }
                    .footer-links a {
                        color: #9C27B0;
                        text-decoration: none;
                        margin: 0 15px;
                        font-weight: 500;
                        transition: opacity 0.3s ease;
                    }
                    .footer-links a:hover {
                        opacity: 0.7;
                    }
                    @media (max-width: 600px) {
                        .container { margin: 10px; }
                        .info-grid { grid-template-columns: 1fr; }
                        .header { padding: 30px 20px; }
                        .content { padding: 30px 20px; }
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
                            <h2 class="notification-title">%s</h2>
                            <div class="notification-content">
                                %s
                            </div>

                            <div class="info-grid">
                                <div class="info-item">
                                    <div class="info-label">Destinatario</div>
                                    <div class="info-value">%s</div>
                                </div>
                                <div class="info-item">
                                    <div class="info-label">Rol</div>
                                    <div class="info-value">Director de Área</div>
                                </div>
                                <div class="info-item">
                                    <div class="info-label">Fecha y Hora</div>
                                    <div class="info-value">%s</div>
                                </div>
                                <div class="info-item">
                                    <div class="info-label">Tipo</div>
                                    <div class="info-value">Gestión Administrativa</div>
                                </div>
                            </div>

                            <div class="action-section">
                                <div class="action-text">
                                    🔧 Esta notificación requiere su atención inmediata para gestión administrativa
                                </div>
                                <a href="#" class="action-button">
                                    Panel de Control
                                </a>
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
     * Plantilla HTML para coordinadores
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
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        min-height: 100vh;
                        padding: 20px;
                    }
                    .container {
                        max-width: 650px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 20px;
                        overflow: hidden;
                        box-shadow: 0 20px 40px rgba(0,0,0,0.1);
                        animation: slideIn 0.6s ease-out;
                    }
                    @keyframes slideIn {
                        from { transform: translateY(-30px); opacity: 0; }
                        to { transform: translateY(0); opacity: 1; }
                    }
                    .header {
                        background: linear-gradient(135deg, #FF5722 0%, #E64A19 100%);
                        color: white;
                        padding: 40px 30px;
                        text-align: center;
                        position: relative;
                        overflow: hidden;
                    }
                    .header::before {
                        content: '';
                        position: absolute;
                        top: -50%;
                        left: -50%;
                        width: 200%;
                        height: 200%;
                        background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="20" cy="20" r="2" fill="rgba(255,255,255,0.1)"/><circle cx="80" cy="80" r="2" fill="rgba(255,255,255,0.1)"/><circle cx="40" cy="60" r="1" fill="rgba(255,255,255,0.1)"/></svg>');
                        animation: float 6s ease-in-out infinite;
                    }
                    @keyframes float {
                        0%, 100% { transform: translateY(0px) rotate(0deg); }
                        50% { transform: translateY(-10px) rotate(180deg); }
                    }
                    .logo {
                        width: 80px;
                        height: 80px;
                        background: rgba(255,255,255,0.2);
                        border-radius: 50%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        margin: 0 auto 20px;
                        font-size: 36px;
                        backdrop-filter: blur(10px);
                        border: 2px solid rgba(255,255,255,0.3);
                    }
                    .header h1 {
                        font-size: 28px;
                        font-weight: 700;
                        margin-bottom: 10px;
                        text-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .header p {
                        font-size: 18px;
                        opacity: 0.9;
                        font-weight: 300;
                    }
                    .content {
                        padding: 40px 30px;
                        background: #fafafa;
                    }
                    .notification-card {
                        background: white;
                        border-radius: 15px;
                        padding: 30px;
                        margin-bottom: 30px;
                        border-left: 5px solid #FF5722;
                        box-shadow: 0 8px 25px rgba(0,0,0,0.08);
                        transition: transform 0.3s ease;
                    }
                    .notification-card:hover {
                        transform: translateY(-2px);
                    }
                    .notification-title {
                        color: #2c3e50;
                        font-size: 22px;
                        font-weight: 600;
                        margin-bottom: 20px;
                        display: flex;
                        align-items: center;
                    }
                    .notification-title::before {
                        content: '⚙️';
                        margin-right: 10px;
                        font-size: 24px;
                    }
                    .notification-content {
                        color: #555;
                        line-height: 1.7;
                        font-size: 16px;
                        margin-bottom: 25px;
                    }
                    .info-grid {
                        display: grid;
                        grid-template-columns: 1fr 1fr;
                        gap: 20px;
                        margin-bottom: 25px;
                    }
                    .info-item {
                        background: #f8f9fa;
                        padding: 15px;
                        border-radius: 10px;
                        border-left: 3px solid #FF5722;
                    }
                    .info-label {
                        font-size: 12px;
                        color: #666;
                        text-transform: uppercase;
                        font-weight: 600;
                        letter-spacing: 0.5px;
                        margin-bottom: 5px;
                    }
                    .info-value {
                        font-size: 14px;
                        color: #2c3e50;
                        font-weight: 500;
                    }
                    .system-status {
                        background: linear-gradient(135deg, #fff3e0 0%, #fff8e1 100%);
                        border: 1px solid #ffe0b2;
                        border-radius: 10px;
                        padding: 15px;
                        margin-bottom: 20px;
                        text-align: center;
                    }
                    .status-indicator {
                        display: inline-block;
                        width: 12px;
                        height: 12px;
                        background: #FF5722;
                        border-radius: 50%;
                        margin-right: 8px;
                        animation: pulse 2s infinite;
                    }
                    @keyframes pulse {
                        0% { box-shadow: 0 0 0 0 rgba(255, 87, 34, 0.7); }
                        70% { box-shadow: 0 0 0 10px rgba(255, 87, 34, 0); }
                        100% { box-shadow: 0 0 0 0 rgba(255, 87, 34, 0); }
                    }
                    .action-section {
                        background: linear-gradient(135deg, #fff8e1 0%, #fff3e0 100%);
                        padding: 25px;
                        border-radius: 12px;
                        text-align: center;
                        border: 1px solid #ffe0b2;
                    }
                    .action-text {
                        color: #bf360c;
                        font-size: 16px;
                        margin-bottom: 15px;
                        font-weight: 500;
                    }
                    .action-button {
                        display: inline-block;
                        background: #E64A19;
                        color: white;
                        padding: 12px 30px;
                        text-decoration: none;
                        border-radius: 25px;
                        font-weight: 600;
                        transition: all 0.3s ease;
                        box-shadow: 0 4px 15px rgba(230, 74, 25, 0.3);
                    }
                    .action-button:hover {
                        background: #D84315;
                        transform: translateY(-2px);
                        box-shadow: 0 6px 20px rgba(230, 74, 25, 0.4);
                    }
                    .footer {
                        background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
                        color: white;
                        padding: 30px;
                        text-align: center;
                    }
                    .footer-logo {
                        font-size: 24px;
                        font-weight: 700;
                        margin-bottom: 10px;
                        color: #FF5722;
                    }
                    .footer-text {
                        font-size: 14px;
                        opacity: 0.8;
                        line-height: 1.6;
                    }
                    .footer-links {
                        margin-top: 20px;
                    }
                    .footer-links a {
                        color: #FF5722;
                        text-decoration: none;
                        margin: 0 15px;
                        font-weight: 500;
                        transition: opacity 0.3s ease;
                    }
                    .footer-links a:hover {
                        opacity: 0.7;
                    }
                    @media (max-width: 600px) {
                        .container { margin: 10px; }
                        .info-grid { grid-template-columns: 1fr; }
                        .header { padding: 30px 20px; }
                        .content { padding: 30px 20px; }
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
                            <h2 class="notification-title">%s</h2>
                            <div class="notification-content">
                                %s
                            </div>

                            <div class="info-grid">
                                <div class="info-item">
                                    <div class="info-label">Destinatario</div>
                                    <div class="info-value">%s</div>
                                </div>
                                <div class="info-item">
                                    <div class="info-label">Rol</div>
                                    <div class="info-value">Coordinador</div>
                                </div>
                                <div class="info-item">
                                    <div class="info-label">Fecha y Hora</div>
                                    <div class="info-value">%s</div>
                                </div>
                                <div class="info-item">
                                    <div class="info-label">Tipo</div>
                                    <div class="info-value">Administración del Sistema</div>
                                </div>
                            </div>

                            <div class="action-section">
                                <div class="action-text">
                                    🎛️ Accede al panel de administración para gestionar esta notificación del sistema
                                </div>
                                <a href="#" class="action-button">
                                    Panel de Administración
                                </a>
                            </div>
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
                <title>SGH - Notificación del Sistema</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        min-height: 100vh;
                        padding: 20px;
                    }
                    .container {
                        max-width: 650px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 20px;
                        overflow: hidden;
                        box-shadow: 0 20px 40px rgba(0,0,0,0.1);
                        animation: slideIn 0.6s ease-out;
                    }
                    @keyframes slideIn {
                        from { transform: translateY(-30px); opacity: 0; }
                        to { transform: translateY(0); opacity: 1; }
                    }
                    .header {
                        background: linear-gradient(135deg, #6c757d 0%, #495057 100%);
                        color: white;
                        padding: 40px 30px;
                        text-align: center;
                        position: relative;
                        overflow: hidden;
                    }
                    .header::before {
                        content: '';
                        position: absolute;
                        top: -50%;
                        left: -50%;
                        width: 200%;
                        height: 200%;
                        background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="20" cy="20" r="2" fill="rgba(255,255,255,0.1)"/><circle cx="80" cy="80" r="2" fill="rgba(255,255,255,0.1)"/><circle cx="40" cy="60" r="1" fill="rgba(255,255,255,0.1)"/></svg>');
                        animation: float 6s ease-in-out infinite;
                    }
                    @keyframes float {
                        0%, 100% { transform: translateY(0px) rotate(0deg); }
                        50% { transform: translateY(-10px) rotate(180deg); }
                    }
                    .logo {
                        width: 80px;
                        height: 80px;
                        background: rgba(255,255,255,0.2);
                        border-radius: 50%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        margin: 0 auto 20px;
                        font-size: 36px;
                        backdrop-filter: blur(10px);
                        border: 2px solid rgba(255,255,255,0.3);
                    }
                    .header h1 {
                        font-size: 28px;
                        font-weight: 700;
                        margin-bottom: 10px;
                        text-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .header p {
                        font-size: 18px;
                        opacity: 0.9;
                        font-weight: 300;
                    }
                    .content {
                        padding: 40px 30px;
                        background: #fafafa;
                    }
                    .notification-card {
                        background: white;
                        border-radius: 15px;
                        padding: 30px;
                        margin-bottom: 30px;
                        border-left: 5px solid #6c757d;
                        box-shadow: 0 8px 25px rgba(0,0,0,0.08);
                        transition: transform 0.3s ease;
                    }
                    .notification-card:hover {
                        transform: translateY(-2px);
                    }
                    .notification-title {
                        color: #2c3e50;
                        font-size: 22px;
                        font-weight: 600;
                        margin-bottom: 20px;
                        display: flex;
                        align-items: center;
                    }
                    .notification-title::before {
                        content: '📢';
                        margin-right: 10px;
                        font-size: 24px;
                    }
                    .notification-content {
                        color: #555;
                        line-height: 1.7;
                        font-size: 16px;
                        margin-bottom: 25px;
                    }
                    .info-grid {
                        display: grid;
                        grid-template-columns: 1fr 1fr;
                        gap: 20px;
                        margin-bottom: 25px;
                    }
                    .info-item {
                        background: #f8f9fa;
                        padding: 15px;
                        border-radius: 10px;
                        border-left: 3px solid #6c757d;
                    }
                    .info-label {
                        font-size: 12px;
                        color: #666;
                        text-transform: uppercase;
                        font-weight: 600;
                        letter-spacing: 0.5px;
                        margin-bottom: 5px;
                    }
                    .info-value {
                        font-size: 14px;
                        color: #2c3e50;
                        font-weight: 500;
                    }
                    .action-section {
                        background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
                        padding: 25px;
                        border-radius: 12px;
                        text-align: center;
                        border: 1px solid #dee2e6;
                    }
                    .action-text {
                        color: #495057;
                        font-size: 16px;
                        margin-bottom: 15px;
                        font-weight: 500;
                    }
                    .action-button {
                        display: inline-block;
                        background: #6c757d;
                        color: white;
                        padding: 12px 30px;
                        text-decoration: none;
                        border-radius: 25px;
                        font-weight: 600;
                        transition: all 0.3s ease;
                        box-shadow: 0 4px 15px rgba(108, 117, 125, 0.3);
                    }
                    .action-button:hover {
                        background: #5a6268;
                        transform: translateY(-2px);
                        box-shadow: 0 6px 20px rgba(108, 117, 125, 0.4);
                    }
                    .footer {
                        background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
                        color: white;
                        padding: 30px;
                        text-align: center;
                    }
                    .footer-logo {
                        font-size: 24px;
                        font-weight: 700;
                        margin-bottom: 10px;
                        color: #6c757d;
                    }
                    .footer-text {
                        font-size: 14px;
                        opacity: 0.8;
                        line-height: 1.6;
                    }
                    .footer-links {
                        margin-top: 20px;
                    }
                    .footer-links a {
                        color: #6c757d;
                        text-decoration: none;
                        margin: 0 15px;
                        font-weight: 500;
                        transition: opacity 0.3s ease;
                    }
                    .footer-links a:hover {
                        opacity: 0.7;
                    }
                    @media (max-width: 600px) {
                        .container { margin: 10px; }
                        .info-grid { grid-template-columns: 1fr; }
                        .header { padding: 30px 20px; }
                        .content { padding: 30px 20px; }
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
                            <h2 class="notification-title">%s</h2>
                            <div class="notification-content">
                                %s
                            </div>

                            <div class="info-grid">
                                <div class="info-item">
                                    <div class="info-label">Destinatario</div>
                                    <div class="info-value">%s</div>
                                </div>
                                <div class="info-item">
                                    <div class="info-label">Rol</div>
                                    <div class="info-value">%s</div>
                                </div>
                                <div class="info-item">
                                    <div class="info-label">Fecha y Hora</div>
                                    <div class="info-value">%s</div>
                                </div>
                                <div class="info-item">
                                    <div class="info-label">Categoría</div>
                                    <div class="info-value">Notificación General</div>
                                </div>
                            </div>

                            <div class="action-section">
                                <div class="action-text">
                                    📢 Esta notificación contiene información importante del sistema
                                </div>
                                <a href="#" class="action-button">
                                    Acceder al Sistema
                                </a>
                            </div>
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

    /**
     * Valida que el tipo de notificación sea válido para el rol especificado
     */
    private void validateNotificationTypeForRole(NotificationType notificationType, String recipientRole) {
        String[] allowedRoles = notificationType.getAllowedRoles();

        for (String allowedRole : allowedRoles) {
            if (allowedRole.equals(recipientRole)) {
                return; // Válido
            }
        }

        // Si llega aquí, el tipo no es válido para el rol
        throw new IllegalArgumentException(
            String.format("El tipo de notificación '%s' no está permitido para el rol '%s'. " +
                         "Tipos permitidos para %s: %s",
                         notificationType.name(),
                         recipientRole,
                         recipientRole,
                         String.join(", ", allowedRoles))
        );
    }
}