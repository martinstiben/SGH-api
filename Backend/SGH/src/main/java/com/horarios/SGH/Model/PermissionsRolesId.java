package com.horarios.SGH.Model;

import java.io.Serializable;
import java.util.Objects;

public class PermissionsRolesId implements Serializable {
    private int role;
    private int permission;

    // Constructor vacío
    public PermissionsRolesId() {}

    // Constructor con parámetros
    public PermissionsRolesId(int role, int permission) {
        this.role = role;
        this.permission = permission;
    }

    // Getters y setters
    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public int getPermission() {
        return permission;
    }

    public void setPermission(int permission) {
        this.permission = permission;
    }

    // equals y hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PermissionsRolesId that = (PermissionsRolesId) o;
        return role == that.role && permission == that.permission;
    }

    @Override
    public int hashCode() {
        return Objects.hash(role, permission);
    }
}