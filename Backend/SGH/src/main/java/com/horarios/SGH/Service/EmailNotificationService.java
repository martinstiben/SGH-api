package com.horarios.SGH.Service;

import com.horarios.SGH.Model.User;
import org.springframework.stereotype.Service;

/**
 * Servicio para envío de notificaciones por email.
 * Maneja emails de bienvenida, confirmación y notificaciones del sistema.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Service
public class EmailNotificationService {

    /**
     * Envía email de bienvenida a un nuevo usuario.
     *
     * @param user Usuario recién registrado
     */
    public void sendWelcomeEmail(User user) {
        // TODO: Implementar envío de email de bienvenida
        System.out.println("Enviando email de bienvenida a: " + user.getEmail());
    }

    /**
     * Envía email de confirmación de registro.
     *
     * @param user Usuario a confirmar
     * @param verificationCode Código de verificación
     */
    public void sendVerificationEmail(User user, String verificationCode) {
        // TODO: Implementar envío de email de verificación
        System.out.println("Enviando email de verificación a: " + user.getEmail());
    }

    /**
     * Envía email de restablecimiento de contraseña.
     *
     * @param user Usuario que solicita reset
     * @param verificationCode Código de verificación
     */
    public void sendPasswordResetEmail(User user, String verificationCode) {
        // TODO: Implementar envío de email de reset
        System.out.println("Enviando email de reset de contraseña a: " + user.getEmail());
    }
}