package com.horarios.SGH.Model;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeración que define los posibles estados de una cuenta de usuario en el sistema SGH.
 * Los estados determinan el nivel de acceso y funcionalidad disponible para el usuario.
 *
 * Esta enumeración es fundamental para el control de acceso y gestión de usuarios,
 * permitiendo activar, bloquear o marcar cuentas como pendientes de aprobación.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Schema(description = "Estados de cuenta disponibles en el sistema")
public enum AccountStatus {

    /**
     * Estado activo: La cuenta está completamente funcional y el usuario tiene acceso total.
     * Este es el estado normal para usuarios aprobados y en buen estado.
     */
    @Schema(description = "Cuenta activa - Usuario con acceso completo") ACTIVE,

    /**
     * Estado bloqueado: La cuenta ha sido temporal o permanentemente suspendida.
     * El usuario no puede acceder al sistema hasta que sea desbloqueada por un administrador.
     */
    @Schema(description = "Cuenta bloqueada - Acceso denegado") BLOCKED,

    /**
     * Estado inactivo: La cuenta existe pero no está disponible para uso.
     * Puede deberse a inactividad prolongada o desactivación voluntaria.
     */
    @Schema(description = "Cuenta inactiva - Temporalmente fuera de servicio") INACTIVE,

    /**
     * Estado pendiente de aprobación: La cuenta fue creada pero requiere aprobación administrativa.
     * Común en sistemas donde los nuevos usuarios deben ser validados antes de obtener acceso.
     */
    @Schema(description = "Cuenta pendiente de aprobación - Esperando validación") PENDING_APPROVAL;

    /**
     * Verifica si el estado permite acceso al sistema.
     *
     * @return true si el estado es ACTIVE
     */
    public boolean allowsAccess() {
        return this == ACTIVE;
    }

    /**
     * Verifica si el estado requiere intervención administrativa.
     *
     * @return true si el estado es BLOCKED o PENDING_APPROVAL
     */
    public boolean requiresAdminAction() {
        return this == BLOCKED || this == PENDING_APPROVAL;
    }

    /**
     * Obtiene una descripción legible del estado.
     *
     * @return descripción en español del estado
     */
    public String getDescription() {
        switch (this) {
            case ACTIVE:
                return "Cuenta activa";
            case BLOCKED:
                return "Cuenta bloqueada";
            case INACTIVE:
                return "Cuenta inactiva";
            case PENDING_APPROVAL:
                return "Pendiente de aprobación";
            default:
                return "Estado desconocido";
        }
    }
}