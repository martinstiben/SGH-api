package com.horarios.SGH.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RoleDTO {
    private Long roleId;

    @NotNull(message = "El nombre del rol es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre del rol debe tener entre 3 y 50 caracteres")
    private String roleName;

    @Size(max = 255, message = "La descripción debe tener máximo 255 caracteres")
    private String description;

    private boolean isActive = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}