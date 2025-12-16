package com.horarios.SGH.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long userId;

    @NotNull(message = "El nombre de usuario es obligatorio")
    @Size(min = 4, max = 50, message = "El nombre de usuario debe tener entre 4 y 50 caracteres")
    private String username;

    @NotNull(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    @Size(max = 254, message = "El email debe tener máximo 254 caracteres")
    private String email;

    @NotNull(message = "El nombre es obligatorio")
    @Size(min = 1, max = 100, message = "El nombre debe tener entre 1 y 100 caracteres")
    private String firstName;

    @NotNull(message = "El apellido es obligatorio")
    @Size(min = 1, max = 100, message = "El apellido debe tener entre 1 y 100 caracteres")
    private String lastName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}