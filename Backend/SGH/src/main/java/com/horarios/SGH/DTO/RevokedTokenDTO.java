package com.horarios.SGH.DTO;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RevokedTokenDTO {
    private Long tokenId;

    @NotNull(message = "El token es obligatorio")
    @Size(min = 10, max = 512, message = "El token debe tener entre 10 y 512 caracteres")
    private String token;

    @NotNull(message = "El ID de usuario es obligatorio")
    private Long userId;

    private LocalDateTime revokedAt;
    private LocalDateTime expiresAt;
    private boolean isRefreshToken = false;
}