package com.horarios.SGH.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class PasswordResetRequestDTO {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe tener un formato válido")
    private String email;

    // Constructor vacío
    public PasswordResetRequestDTO() {
    }

    // Constructor con parámetros
    public PasswordResetRequestDTO(String email) {
        this.email = email;
    }

    // Getters y setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}