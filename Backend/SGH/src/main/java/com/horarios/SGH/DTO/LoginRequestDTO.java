package com.horarios.SGH.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para solicitud de login")
public class LoginRequestDTO {
    @NotBlank(message = "El correo electrónico no puede estar vacío")
    @Email(message = "El correo electrónico debe tener un formato válido")
    @Size(max = 100, message = "El correo electrónico no puede exceder los 100 caracteres")
    @Schema(description = "Correo electrónico del usuario", example = "usuario@example.com")
    private String email;

    @NotBlank(message = "La contraseña no puede estar vacía")
    @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "La contraseña debe contener al menos una letra minúscula, una mayúscula y un número")
    @Schema(description = "Contraseña (debe contener minúscula, mayúscula y número)", example = "Password123")
    private String password;

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

}