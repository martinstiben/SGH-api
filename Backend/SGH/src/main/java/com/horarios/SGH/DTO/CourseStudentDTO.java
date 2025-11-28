package com.horarios.SGH.DTO;

import com.horarios.SGH.Model.AccountStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseStudentDTO {
    @NotNull(message = "El ID del usuario es obligatorio")
    private int userId;

    @NotNull(message = "El nombre completo es obligatorio")
    @Size(min = 1, max = 100, message = "El nombre completo debe tener entre 1 y 100 caracteres")
    private String fullName;

    @NotNull(message = "El email es obligatorio")
    @Size(min = 1, max = 150, message = "El email debe tener entre 1 y 150 caracteres")
    private String email;

    @NotNull(message = "El nombre del rol es obligatorio")
    @Size(min = 1, max = 50, message = "El nombre del rol debe tener entre 1 y 50 caracteres")
    private String roleName;

    @NotNull(message = "El estado de la cuenta es obligatorio")
    private AccountStatus accountStatus;

    private boolean isVerified;
}