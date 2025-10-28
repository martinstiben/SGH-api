package com.horarios.SGH.Model;

import jakarta.persistence.*;
import lombok.Data;

@Entity(name = "permissions_roles")
@Data
@IdClass(PermissionsRolesId.class)
public class PermissionsRoles {
    @Id
    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Roles role;

    @Id
    @ManyToOne
    @JoinColumn(name = "permission_id", nullable = false)
    private Permissions permission;

    // Constructor vacío
    public PermissionsRoles() {}

    // Constructor con parámetros
    public PermissionsRoles(Roles role, Permissions permission) {
        this.role = role;
        this.permission = permission;
    }
}