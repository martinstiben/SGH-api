package com.horarios.SGH.Service;

import com.horarios.SGH.DTO.InAppNotificationDTO;
import com.horarios.SGH.Model.NotificationType;
import com.horarios.SGH.Model.User;
import com.horarios.SGH.Repository.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio de diagnóstico para debuggear problemas de notificaciones
 */
@Service
public class DebugNotificationService {

    @Autowired
    private IUserRepository usersRepository;

    @Autowired
    private InAppNotificationService inAppNotificationService;

    /**
     * Diagnostica por qué no están llegando las notificaciones
     */
    public String diagnoseNotificationIssue() {
        StringBuilder diagnosis = new StringBuilder();
        
        diagnosis.append("=== DIAGNÓSTICO DE NOTIFICACIONES ===\n\n");
        
        // 1. Verificar coordinadores en la base de datos
        diagnosis.append("1. COORDINADORES EN BD:\n");
        try {
            List<User> coordinators = usersRepository.findByRoleNameWithDetails("COORDINADOR");
            diagnosis.append("   ✓ Total coordinadores: " + coordinators.size() + "\n");
            for (User user : coordinators) {
                diagnosis.append("   ✓ Coordinador encontrado: ID=" + user.getUserId() +
                               ", Email=" + (user.getPerson() != null ? user.getPerson().getEmail() : "N/A") +
                               ", Status=" + user.getAccountStatus() + "\n");
            }
        } catch (Exception e) {
            diagnosis.append("   ❌ Error consultando usuarios: " + e.getMessage() + "\n");
        }

        // 2. Verificar método findByRoleNameWithDetails
        diagnosis.append("\n2. MÉTODO findByRoleNameWithDetails:\n");
        try {
            List<User> coordinators = usersRepository.findByRoleNameWithDetails("COORDINADOR");
            diagnosis.append("   ✓ Método funciona, encontró: " + coordinators.size() + " coordinadores\n");
        } catch (Exception e) {
            diagnosis.append("   ❌ Error en findByRoleNameWithDetails: " + e.getMessage() + "\n");
        }
        
        // 3. Verificar servicio de notificaciones
        diagnosis.append("\n3. SERVICIO DE NOTIFICACIONES:\n");
        try {
            // Crear notificación de prueba
            InAppNotificationDTO testNotification = new InAppNotificationDTO();
            testNotification.setUserId(1L); // Usuario admin por defecto
            testNotification.setNotificationType(NotificationType.SYSTEM_NOTIFICATION.name());
            testNotification.setTitle("Notificación de Diagnóstico");
            testNotification.setMessage("Esta es una notificación de prueba para verificar el funcionamiento");
            testNotification.setPriority("MEDIUM");
            testNotification.setCategory("debug");
            
            inAppNotificationService.sendInAppNotificationAsync(testNotification);
            diagnosis.append("   ✓ Servicio de notificaciones responde correctamente\n");
        } catch (Exception e) {
            diagnosis.append("   ❌ Error en servicio de notificaciones: " + e.getMessage() + "\n");
        }
        
        diagnosis.append("\n=== FIN DIAGNÓSTICO ===");
        return diagnosis.toString();
    }

    /**
     * Crea una notificación de prueba para el usuario actual
     */
    public void createTestNotificationForCurrentUser() {
        try {
            InAppNotificationDTO testNotification = new InAppNotificationDTO();
            testNotification.setUserId(1L); // Asumiendo ID 1 para testing
            testNotification.setNotificationType(NotificationType.SYSTEM_NOTIFICATION.name());
            testNotification.setTitle("🔔 Notificación de Prueba");
            testNotification.setMessage("Esta es una notificación de prueba. Si la ves, el sistema funciona correctamente.");
            testNotification.setPriority("HIGH");
            testNotification.setCategory("test");
            testNotification.setActionUrl("/dashboard");
            testNotification.setActionText("Ir al Dashboard");
            
            inAppNotificationService.sendInAppNotificationAsync(testNotification);
            System.out.println("✅ Notificación de prueba creada exitosamente");
        } catch (Exception e) {
            System.err.println("❌ Error creando notificación de prueba: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
