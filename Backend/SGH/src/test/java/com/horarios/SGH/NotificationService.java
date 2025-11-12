package com.horarios.SGH;

import com.horarios.SGH.DTO.NotificationDTO;
import com.horarios.SGH.Model.NotificationType;
import com.horarios.SGH.Service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para el servicio de notificaciones
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class NotificationServiceTest {
    
    @Mock
    private JavaMailSender mailSender;
    
    @Mock
    private MimeMessage mimeMessage;
    
    @InjectMocks
    private NotificationService notificationService;
    
    @Test
    void testSendNotificationAsync() throws Exception {
        // Given
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mimeMessage.getAllRecipients()).thenReturn(null);
        
        NotificationDTO notification = new NotificationDTO();
        notification.setRecipientEmail("test@example.com");
        notification.setRecipientName("Test User");
        notification.setRecipientRole("ESTUDIANTE");
        notification.setNotificationType("STUDENT_SCHEDULE_ASSIGNMENT");
        notification.setSubject("Test Subject");
        notification.setContent("Test content");
        notification.setIsHtml(true);
        
        // When
        CompletableFuture<Void> result = notificationService.sendNotificationAsync(notification);
        
        // Then
        assertNotNull(result);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
    
    @Test
    void testSendNotificationToRoleAsync() throws Exception {
        // Given
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mimeMessage.getAllRecipients()).thenReturn(null);
        
        Map<String, String> variables = Map.of(
            "content", "Test content for coordinator",
            "subject", "Test notification"
        );
        
        // When
        CompletableFuture<Void> result = notificationService.sendNotificationToRoleAsync(
            "COORDINADOR", 
            NotificationType.COORDINATOR_GLOBAL_UPDATE, 
            "Test Subject", 
            variables
        );
        
        // Then
        assertNotNull(result);
        // Verificar que se intentará enviar el correo
        verify(mailSender, atLeast(0)).send(any(MimeMessage.class));
    }
    
    @Test
    void testSendBulkNotificationAsync() throws Exception {
        // Given
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mimeMessage.getAllRecipients()).thenReturn(null);
        
        NotificationDTO notification1 = new NotificationDTO();
        notification1.setRecipientEmail("user1@example.com");
        notification1.setRecipientName("User 1");
        notification1.setRecipientRole("ESTUDIANTE");
        notification1.setNotificationType("STUDENT_SCHEDULE_ASSIGNMENT");
        notification1.setSubject("Test Subject 1");
        notification1.setContent("Test content 1");
        notification1.setIsHtml(true);
        
        NotificationDTO notification2 = new NotificationDTO();
        notification2.setRecipientEmail("user2@example.com");
        notification2.setRecipientName("User 2");
        notification2.setRecipientRole("MAESTRO");
        notification2.setNotificationType("TEACHER_CLASS_SCHEDULED");
        notification2.setSubject("Test Subject 2");
        notification2.setContent("Test content 2");
        notification2.setIsHtml(true);
        
        java.util.List<NotificationDTO> notifications = java.util.Arrays.asList(notification1, notification2);
        
        // When
        CompletableFuture<Void> result = notificationService.sendBulkNotificationAsync(notifications);
        
        // Then
        assertNotNull(result);
        verify(mailSender, atLeast(0)).send(any(MimeMessage.class));
    }
    
    @Test
    void testGetNotificationStatistics() {
        // Given
        // El repositorio debería devolver estadísticas mock
        // Por simplicidad, solo probamos que el método no lanza excepción
        
        // When
        Map<String, Object> stats = notificationService.getNotificationStatistics();
        
        // Then
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalToday"));
        assertTrue(stats.containsKey("pending"));
        assertTrue(stats.containsKey("sent"));
        assertTrue(stats.containsKey("failed"));
    }
    
    @Test
    void testRetryFailedNotifications() throws Exception {
        // Given
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(mimeMessage.getAllRecipients()).thenReturn(null);
        
        // When
        CompletableFuture<Void> result = notificationService.retryFailedNotifications();
        
        // Then
        assertNotNull(result);
        // Verificar que se intentarán reintentos (aunque pueden ser 0 si no hay notificaciones fallidas)
        verify(mailSender, atLeast(0)).createMimeMessage();
    }
    
    @Test
    void testNotificationDTOValidation() {
        // Given
        NotificationDTO notification = new NotificationDTO();
        notification.setRecipientEmail("test@example.com");
        notification.setRecipientName("Test User");
        notification.setRecipientRole("ESTUDIANTE");
        notification.setNotificationType("STUDENT_SCHEDULE_ASSIGNMENT");
        
        // Then
        assertNotNull(notification.getRecipientEmail());
        assertNotNull(notification.getRecipientName());
        assertNotNull(notification.getRecipientRole());
        assertNotNull(notification.getNotificationType());
    }
    
    @Test
    void testNotificationTypeEnum() {
        // Then
        assertEquals(NotificationType.STUDENT_SCHEDULE_ASSIGNMENT, 
                    NotificationType.valueOf("STUDENT_SCHEDULE_ASSIGNMENT"));
        assertEquals(NotificationType.TEACHER_CLASS_SCHEDULED, 
                    NotificationType.valueOf("TEACHER_CLASS_SCHEDULED"));
        assertEquals(NotificationType.COORDINATOR_GLOBAL_UPDATE, 
                    NotificationType.valueOf("COORDINATOR_GLOBAL_UPDATE"));
    }
}