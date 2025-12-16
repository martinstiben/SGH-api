package com.horarios.SGH.DTO;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UserSecurityDTO {
    private Long securityId;
    private boolean enabled = true;
    private boolean locked = false;
    private boolean credentialsExpired = false;
    private boolean accountExpired = false;
    private LocalDateTime lastLogin;
    private int failedAttempts = 0;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}