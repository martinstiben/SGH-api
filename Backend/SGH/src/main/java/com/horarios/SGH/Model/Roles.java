package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Entity(name = "roles")
@Data
public class Roles {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private int roleId;

    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    @NotNull(message = "El nombre del rol es obligatorio")
    @Size(min = 1, max = 50, message = "El nombre del rol debe tener entre 1 y 50 caracteres")
    private String roleName;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<users> users;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PermissionsRoles> permissionsRoles;

    // Constructor vacío
    public Roles() {}

    // Constructor con parámetros principales
    public Roles(String roleName) {
        this.roleName = roleName;
    }
}