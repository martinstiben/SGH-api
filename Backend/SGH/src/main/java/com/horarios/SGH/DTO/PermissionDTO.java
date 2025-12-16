package com.horarios.SGH.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PermissionDTO {
    private Long permissionId;

    @NotNull(message = "El nombre del permiso es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre del permiso debe tener entre 3 y 100 caracteres")
    private String permissionName;

    @Size(max = 255, message = "La descripción debe tener máximo 255 caracteres")
    private String description;

    private boolean isActive = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}