package com.horarios.SGH.DTO;

import com.horarios.SGH.Model.Role;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class usersDTO {
    private int userId;

    @NotBlank(message = "El nombre de usuario no puede estar vacío")
    @Size(max = 50, message = "El nombre de usuario no puede exceder los 50 caracteres")
    @Pattern(regexp = "^[a-z]*$", message = "El nombre de usuario solo puede contener letras minúsculas")
    private String userName;

    @NotBlank(message = "La contraseña no puede estar vacía")
    private String password;

    @NotNull(message = "El rol no puede ser nulo")
    private Role role;

    // Información de foto de perfil
    private byte[] photoData;
    private String photoContentType;
    private String photoFileName;
}