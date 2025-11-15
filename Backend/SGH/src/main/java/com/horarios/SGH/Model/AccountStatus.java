package com.horarios.SGH.Model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estados de cuenta disponibles en el sistema")
public enum AccountStatus {
    @Schema(description = "Cuenta activa") ACTIVE,
    @Schema(description = "Cuenta bloqueada") BLOCKED,
    @Schema(description = "Cuenta inactiva") INACTIVE,
    @Schema(description = "Cuenta pendiente de aprobación") PENDING_APPROVAL
}