package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * DTO para solicitud de restablecimiento de contraseña del sistema SGH.
 * Implementa validaciones de negocio específicas para solicitudes de reset
 * y métodos de utilidad para gestión de recuperación de contraseñas.
 *
 * Proporciona métodos Factory para crear solicitudes
 * y validaciones de formato de email.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO para solicitud de restablecimiento de contraseña")
public class PasswordResetRequestDTO extends AbstractDTO {

    /**
     * Dirección de correo electrónico del usuario que solicita el reset.
     */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    @Schema(description = "Correo electrónico del usuario", example = "usuario@universidad.edu")
    private String email;

    /**
     * Timestamp de creación de la solicitud.
     */
    @Schema(description = "Fecha de creación de la solicitud", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización.
     */
    @Schema(description = "Fecha de última actualización", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    /**
     * Constructor por defecto.
     */
    public PasswordResetRequestDTO() {
        super();
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor con parámetros para inicialización completa.
     *
     * @param email dirección de correo electrónico
     */
    public PasswordResetRequestDTO(String email) {
        super();
        this.email = email;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Método Factory para crear una solicitud de reset.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param email dirección de correo electrónico
     * @return PasswordResetRequestDTO configurado
     */
    public static PasswordResetRequestDTO create(String email) {
        return new PasswordResetRequestDTO(email);
    }

    /**
     * Método Factory para crear un PasswordResetRequestDTO vacío.
     * Útil para formularios o inicialización.
     *
     * @return PasswordResetRequestDTO con valores por defecto
     */
    public static PasswordResetRequestDTO empty() {
        PasswordResetRequestDTO dto = new PasswordResetRequestDTO();
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Verifica si el email tiene formato válido.
     *
     * @return true si el formato es válido
     */
    public boolean hasValidEmailFormat() {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    }

    /**
     * Verifica si la solicitud es reciente (últimos 5 minutos).
     * Las solicitudes de reset deberían procesarse rápidamente.
     *
     * @return true si la solicitud es reciente
     */
    public boolean isRecentRequest() {
        if (createdAt == null) {
            return false;
        }

        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        return createdAt.isAfter(fiveMinutesAgo);
    }

    /**
     * Valida si el DTO tiene todos los campos obligatorios.
     * Método de validación de negocio.
     *
     * @return true si todos los campos obligatorios están presentes y válidos
     */
    @Override
    public boolean isValid() {
        return email != null && !email.trim().isEmpty() && hasValidEmailFormat();
    }

    /**
     * Obtiene una representación segura del DTO.
     * Formato: "Solicitud de reset para [email]"
     *
     * @return Representación segura
     */
    @Override
    public String getSummary() {
        return String.format("Solicitud de reset para %s",
                email != null ? email : "sin email");
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Getters y setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
