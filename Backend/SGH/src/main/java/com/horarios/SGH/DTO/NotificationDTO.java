package com.horarios.SGH.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * DTO para el envío de notificaciones por correo electrónico
 * Permite enviar notificaciones personalizadas según el rol del usuario
 */
@Data
public class NotificationDTO {
    
    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 255, message = "El asunto no puede exceder 255 caracteres")
    private String subject;
    
    @NotBlank(message = "El contenido es obligatorio")
    @Size(max = 5000, message = "El contenido no puede exceder 5000 caracteres")
    private String content;
    
    @Email(message = "El email debe tener un formato válido")
    @NotBlank(message = "El email de destino es obligatorio")
    private String recipientEmail;
    
    @NotBlank(message = "El nombre completo del destinatario es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String recipientName;
    
    private String recipientRole;
    
    private String notificationType;
    
    private String senderName;
    
    private Boolean isHtml = false;
    
    private String templatePath;
    
    @NotEmpty(message = "Debe especificar al menos una variable de plantilla")
    private List<NotificationVariable> templateVariables;
}

/**
 * Variables para las plantillas HTML de notificaciones
 */
@Data
class NotificationVariable {
    private String key;
    private String value;
    
    public NotificationVariable(String key, String value) {
        this.key = key;
        this.value = value;
    }
    
    public NotificationVariable() {}
}