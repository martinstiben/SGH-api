package com.horarios.SGH.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para verificación de código 2FA")
public class VerifyCodeDTO {
    @NotBlank(message = "El correo electrónico no puede estar vacío")
    @Email(message = "El correo electrónico debe tener un formato válido")
    @Size(max = 100, message = "El correo electrónico no puede exceder los 100 caracteres")
    @Schema(description = "Correo electrónico del usuario", example = "usuario@example.com")
    private String email;

    @NotBlank(message = "El código de verificación no puede estar vacío")
    @Pattern(regexp = "^\\d{6}$", message = "El código debe ser de 6 dígitos")
    @Schema(description = "Código de verificación de 6 dígitos", example = "123456")
    private String code;

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

}