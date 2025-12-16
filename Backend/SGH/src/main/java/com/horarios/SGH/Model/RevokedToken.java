package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Entity(name = "revoked_tokens")
@Data
public class RevokedToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    @Column(name = "token", nullable = false, unique = true, length = 512)
    @NotNull(message = "El token es obligatorio")
    @Size(min = 10, max = 512, message = "El token debe tener entre 10 y 512 caracteres")
    private String token;

    @Column(name = "user_id", nullable = false)
    @NotNull(message = "El ID de usuario es obligatorio")
    private Long userId;

    @Column(name = "revoked_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime revokedAt;

    @Column(name = "expires_at", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime expiresAt;

    @Column(name = "is_refresh_token", nullable = false)
    private boolean isRefreshToken = false;

    public RevokedToken() {
        this.revokedAt = LocalDateTime.now();
    }

    public RevokedToken(String token, Long userId, LocalDateTime expiresAt, boolean isRefreshToken) {
        this();
        this.token = token;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.isRefreshToken = isRefreshToken;
    }
}