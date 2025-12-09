package com.horarios.SGH.Service;

import org.springframework.stereotype.Component;

@Component
public class HtmlTemplateService {

    public String verificationTemplate(String code) {
        // Simple, maintainable template. Keep style lightweight.
        return "<html><body><h1>Verificación de Seguridad</h1><p>Tu código es: <strong>" + code + "</strong></p><p>Expira en 10 minutos.</p></body></html>";
    }

    public String passwordResetTemplate(String code, String userName) {
        return "<html><body><h1>Restablecimiento de Contraseña</h1><p>Hola " + userName + ",</p><p>Tu código de restablecimiento es: <strong>" + code + "</strong></p><p>Expira en 10 minutos.</p></body></html>";
    }

    public String approvalTemplate(String userName) {
        return "<html><body><h1>Registro Aprobado</h1><p>Hola " + userName + ",</p><p>Tu registro ha sido aprobado.</p></body></html>";
    }

    public String rejectionTemplate(String userName, String reason) {
        String reasonText = (reason == null || reason.trim().isEmpty()) ? "" : "<p>Motivo: " + reason + "</p>";
        return "<html><body><h1>Registro No Aprobado</h1><p>Hola " + userName + ",</p>" + reasonText + "</body></html>";
    }
}
