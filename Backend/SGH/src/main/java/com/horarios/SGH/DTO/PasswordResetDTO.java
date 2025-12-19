package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO para el proceso de restablecimiento de contraseña del sistema SGH.
 * Implementa validaciones de negocio específicas para el flujo de
 * recuperación de contraseñas con código de verificación.
 *
 * Proporciona métodos de utilidad para validación de seguridad
 * de contraseñas y construcción de instancias.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Datos para restablecer contraseña con código de verificación")
public class PasswordResetDTO extends AbstractDTO {

    /**
     * Dirección de correo electrónico del usuario que solicita el reset.
     */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    @Schema(description = "Correo electrónico del usuario", example = "usuario@universidad.edu")
    private String email;

    /**
     * Código de verificación de 6 dígitos enviado por email.
     */
    @NotBlank(message = "El código de verificación es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El código debe ser de 6 dígitos")
    @Schema(description = "Código de verificación de 6 dígitos", example = "123456")
    private String verificationCode;

    /**
     * Nueva contraseña que debe cumplir con políticas de seguridad.
     */
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
             message = "La contraseña debe contener al menos una letra minúscula, una mayúscula y un número")
    @Schema(description = "Nueva contraseña segura", example = "NuevaPass123")
    private String newPassword;

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
    public PasswordResetDTO() {
        super();
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor con parámetros para inicialización completa.
     *
     * @param email dirección de correo electrónico
     * @param verificationCode código de verificación
     * @param newPassword nueva contraseña
     */
    public PasswordResetDTO(String email, String verificationCode, String newPassword) {
        super();
        this.email = email;
        this.verificationCode = verificationCode;
        this.newPassword = newPassword;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Método Factory para crear una solicitud básica.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param email dirección de correo electrónico
     * @param verificationCode código de verificación
     * @param newPassword nueva contraseña
     * @return PasswordResetDTO configurado
     */
    public static PasswordResetDTO create(String email, String verificationCode, String newPassword) {
        return new PasswordResetDTO(email, verificationCode, newPassword);
    }

    /**
     * Método Factory para crear un PasswordResetDTO vacío.
     * Útil para formularios o inicialización.
     *
     * @return PasswordResetDTO con valores por defecto
     */
    public static PasswordResetDTO empty() {
        PasswordResetDTO dto = new PasswordResetDTO();
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Verifica si la nueva contraseña cumple con las políticas de seguridad.
     * Método de validación de negocio.
     *
     * @return true si la contraseña es segura
     */
    public boolean hasSecurePassword() {
        if (newPassword == null || newPassword.length() < 8) {
            return false;
        }

        boolean hasLower = newPassword.chars().anyMatch(Character::isLowerCase);
        boolean hasUpper = newPassword.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = newPassword.chars().anyMatch(Character::isDigit);

        return hasLower && hasUpper && hasDigit;
    }

    /**
     * Verifica si el código de verificación tiene el formato correcto.
     *
     * @return true si el código es válido
     */
    public boolean hasValidVerificationCode() {
        return verificationCode != null && verificationCode.matches("^\\d{6}$");
    }

    /**
     * Valida si el DTO tiene todos los campos obligatorios.
     * Método de validación de negocio.
     *
     * @return true si todos los campos obligatorios están presentes y válidos
     */
    @Override
    public boolean isValid() {
        return email != null && !email.trim().isEmpty() &&
               hasValidVerificationCode() &&
               newPassword != null && !newPassword.trim().isEmpty() &&
               hasSecurePassword();
    }

    /**
     * Obtiene una representación segura del DTO (sin mostrar la contraseña).
     * Formato: "Reset para [email] con código [***456]"
     *
     * @return Representación segura
     */
    @Override
    public String getSummary() {
        String maskedCode = verificationCode != null && verificationCode.length() >= 3
                ? "***" + verificationCode.substring(verificationCode.length() - 3)
                : "***";
        return String.format("Reset para %s con código %s",
                email != null ? email : "sin email",
                maskedCode);
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

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
