package com.horarios.SGH.Model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Entity(name = "permissions")
@Data
public class Permissions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private int permissionId;

    @Column(name = "permission_name", nullable = false, unique = true, length = 100)
    @NotNull(message = "El nombre del permiso es obligatorio")
    @Size(min = 1, max = 100, message = "El nombre del permiso debe tener entre 1 y 100 caracteres")
    private String permissionName;

    @Column(name = "description", length = 255)
    @Size(max = 255, message = "La descripción debe tener máximo 255 caracteres")
    private String description;

    @OneToMany(mappedBy = "permission", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PermissionsRoles> permissionsRoles;

    // Constructor vacío
    public Permissions() {}

    // Constructor con parámetros principales
    public Permissions(String permissionName, String description) {
        this.permissionName = permissionName;
        this.description = description;
    }
}