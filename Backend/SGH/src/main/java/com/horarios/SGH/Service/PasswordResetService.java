package com.horarios.SGH.Service;

import com.horarios.SGH.Model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para gestión de restablecimiento de contraseñas.
 * Maneja solicitudes de reset, envío de códigos y actualización de contraseñas.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Service
public class PasswordResetService {

    @Autowired
    private usersService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailNotificationService emailNotificationService;

    /**
     * Solicita un restablecimiento de contraseña para el email especificado.
     *
     * @param email Email del usuario
     * @return Mensaje de confirmación
     */
    @Transactional
    public String requestPasswordReset(String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        String verificationCode = generateVerificationCode();
        // TODO: Guardar código en BD con expiración

        emailNotificationService.sendPasswordResetEmail(user, verificationCode);

        return "Se ha enviado un código de verificación al email proporcionado";
    }

    /**
     * Verifica el código y actualiza la contraseña.
     *
     * @param email Email del usuario
     * @param verificationCode Código de verificación
     * @param newPassword Nueva contraseña
     * @return Mensaje de confirmación
     */
    @Transactional
    public String resetPassword(String email, String verificationCode, String newPassword) {
        // TODO: Verificar código en BD
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        // Actualizar contraseña
        user.getUserCredentials().setPasswordHash(passwordEncoder.encode(newPassword));
        // TODO: Guardar cambios

        return "Contraseña actualizada exitosamente";
    }

    /**
     * Genera un código de verificación aleatorio.
     *
     * @return Código de 6 dígitos
     */
    private String generateVerificationCode() {
        return String.valueOf((int)(Math.random() * 900000) + 100000);
    }
}