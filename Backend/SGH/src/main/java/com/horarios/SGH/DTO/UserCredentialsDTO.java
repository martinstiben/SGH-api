package com.horarios.SGH.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserCredentialsDTO {
    private Long credentialId;

    @NotNull(message = "El hash de la contraseña es obligatorio")
    @Size(min = 60, max = 255, message = "El hash de la contraseña debe tener entre 60 y 255 caracteres")
    private String passwordHash;

    @Size(max = 255, message = "El salt debe tener máximo 255 caracteres")
    private String passwordSalt;

    @Size(max = 50, message = "El algoritmo debe tener máximo 50 caracteres")
    private String passwordAlgorithm = "BCrypt";

    private LocalDateTime passwordChangedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}