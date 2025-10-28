package com.horarios.SGH.DTO;

import com.horarios.SGH.Model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO para solicitud de registro de usuario")
public class RegisterRequestDTO {
    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez", required = true)
    private String name;

    @NotBlank(message = "El correo electrónico no puede estar vacío")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "El correo electrónico debe tener un formato válido")
    @Schema(description = "Correo electrónico del usuario", example = "usuario@ejemplo.com", required = true)
    private String email;

    @NotBlank(message = "La contraseña no puede estar vacía")
    @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "La contraseña debe contener al menos una letra minúscula, una mayúscula y un número")
    @Schema(description = "Contraseña (debe contener minúscula, mayúscula y número)", example = "Password123", required = true)
    private String password;

    @NotNull(message = "El rol no puede ser nulo")
    @Schema(description = "Rol del usuario", example = "MAESTRO", required = true, allowableValues = {"MAESTRO", "ESTUDIANTE"})
    private Role role;

    @Schema(description = "ID de la materia (requerido solo para maestros)", example = "1", required = false)
    private Integer subjectId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }
}