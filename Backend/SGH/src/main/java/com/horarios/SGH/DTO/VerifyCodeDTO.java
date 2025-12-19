package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO para verificación de código 2FA del sistema SGH.
 * Implementa validaciones de negocio específicas para códigos de verificación
 * y métodos de utilidad para autenticación de dos factores.
 *
 * Proporciona métodos Factory para crear verificaciones
 * y validaciones de formato de código.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO para verificación de código 2FA")
public class VerifyCodeDTO extends AbstractDTO {
    @NotBlank(message = "El correo electrónico no puede estar vacío")
    @Email(message = "El correo electrónico debe tener un formato válido")
    @Size(max = 100, message = "El correo electrónico no puede exceder los 100 caracteres")
    @Schema(description = "Correo electrónico del usuario", example = "usuario@example.com")
    private String email;

    @NotBlank(message = "El código de verificación no puede estar vacío")
    @Pattern(regexp = "^\\d{6}$", message = "El código debe ser de 6 dígitos")
    @Schema(description = "Código de verificación de 6 dígitos", example = "123456")
    private String code;

    /**
     * Timestamp de creación del registro.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización.
     */
    private LocalDateTime updatedAt;

    /**
     * Constructor por defecto.
     */
    public VerifyCodeDTO() {
        super();
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor con parámetros principales.
     *
     * @param email correo electrónico
     * @param code código de verificación
     */
    public VerifyCodeDTO(String email, String code) {
        super();
        this.email = email;
        this.code = code;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Método Factory para crear una verificación básica.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @param email correo electrónico
     * @param code código de verificación
     * @return VerifyCodeDTO configurado
     */
    public static VerifyCodeDTO create(String email, String code) {
        VerifyCodeDTO dto = new VerifyCodeDTO();
        dto.setEmail(email);
        dto.setCode(code);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Método Factory para crear un VerifyCodeDTO vacío.
     * Útil para inicialización o pruebas.
     *
     * @return VerifyCodeDTO con valores por defecto
     */
    public static VerifyCodeDTO empty() {
        VerifyCodeDTO dto = new VerifyCodeDTO();
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Verifica si el código tiene el formato correcto (6 dígitos).
     *
     * @return true si el formato es válido
     */
    public boolean hasValidCodeFormat() {
        return code != null && code.matches("^\\d{6}$");
    }

    /**
     * Valida si el DTO tiene todos los campos obligatorios.
     * Método de validación de negocio.
     *
     * @return true si todos los campos obligatorios están presentes y válidos
     */
    @Override
    public boolean isValid() {
        return email != null && !email.trim().isEmpty() && hasValidCodeFormat();
    }

    /**
     * Obtiene una representación resumida de la verificación.
     * Formato: "Verificación para [email] - Código: [***456]"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        return getSecureSummary();
    }

    /**
     * Obtiene una representación segura del DTO (sin mostrar el código completo).
     * Formato: "Verificación para [email] - Código: [***456]"
     *
     * @return Representación segura
     */
    public String getSecureSummary() {
        String maskedCode = code != null && code.length() >= 3
                ? "***" + code.substring(code.length() - 3)
                : "***";
        return String.format("Verificación para %s - Código: %s",
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
}