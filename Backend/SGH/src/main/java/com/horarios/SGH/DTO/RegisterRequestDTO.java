package com.horarios.SGH.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * DTO para solicitud de registro de nuevos usuarios del sistema SGH.
 * Implementa el patrón Builder para construcción flexible y validaciones
 * de negocio específicas para registro de usuarios con roles condicionales.
 *
 * Proporciona validaciones condicionales basadas en el rol seleccionado
 * y métodos de utilidad para verificación de integridad de datos.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO para solicitud de registro de usuario")
public class RegisterRequestDTO extends AbstractDTO {

    /**
     * Nombre completo del usuario a registrar.
     * Debe contener nombre y apellido separados por espacio.
     */
    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    @Schema(description = "Nombre completo del usuario", example = "Juan Pérez", required = true)
    private String name;

    /**
     * Dirección de correo electrónico única del usuario.
     * Debe ser válida y no estar registrada previamente.
     */
    @NotBlank(message = "El correo electrónico no puede estar vacío")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "El correo electrónico debe tener un formato válido")
    @Schema(description = "Correo electrónico del usuario", example = "usuario@ejemplo.com", required = true)
    private String email;

    /**
     * Contraseña segura para el usuario.
     * Debe cumplir con políticas de seguridad mínimas.
     */
    @NotBlank(message = "La contraseña no puede estar vacía")
    @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = "La contraseña debe contener al menos una letra minúscula, una mayúscula y un número")
    @Schema(description = "Contraseña (debe contener minúscula, mayúscula y número)", example = "Password123", required = true)
    private String password;

    /**
     * Rol del usuario en el sistema.
     * Determina qué campos adicionales son requeridos.
     */
    @NotNull(message = "El rol no puede ser nulo")
    @Schema(description = "Rol del usuario", example = "MAESTRO", required = true, allowableValues = {"MAESTRO", "ESTUDIANTE"})
    private String role;

    /**
     * ID de la asignatura para docentes.
     * Obligatorio solo cuando el rol es MAESTRO.
     */
    @Schema(description = "ID de la materia (requerido solo para maestros)", example = "1", required = false)
    private Integer subjectId;

    /**
     * ID del curso para estudiantes.
     * Obligatorio solo cuando el rol es ESTUDIANTE.
     */
    @Schema(description = "ID del curso (requerido solo para estudiantes). Los cursos disponibles se obtienen desde GET /courses. Ejemplos: 1=1A, 2=2B, 3=3C", example = "1", required = false)
    private Integer courseId;

    /**
     * Timestamp de creación de la solicitud.
     */
    private LocalDateTime createdAt;

    // Getters y Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    /**
     * Validación condicional: subjectId es obligatorio para MAESTROS.
     * Implementa validación de negocio personalizada.
     */
    @AssertTrue(message = "Los maestros deben tener una materia asignada")
    private boolean isSubjectValidForTeacher() {
        if ("MAESTRO".equals(role)) {
            return subjectId != null && subjectId > 0;
        }
        return true;
    }

    /**
     * Validación condicional: courseId es obligatorio para ESTUDIANTES.
     * Implementa validación de negocio personalizada.
     */
    @AssertTrue(message = "Los estudiantes deben tener un curso asignado")
    private boolean isCourseValidForStudent() {
        if ("ESTUDIANTE".equals(role)) {
            return courseId != null && courseId > 0;
        }
        return true;
    }

    /**
     * Constructor por defecto.
     */
    public RegisterRequestDTO() {
        super();
    }

    /**
     * Constructor con parámetros principales.
     *
     * @param name Nombre completo
     * @param email Correo electrónico
     * @param password Contraseña
     * @param role Rol del usuario
     */
    public RegisterRequestDTO(String name, String email, String password, String role) {
        super();
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor completo para maestros.
     *
     * @param name Nombre completo
     * @param email Correo electrónico
     * @param password Contraseña
     * @param role Rol del usuario
     * @param subjectId ID de la asignatura
     */
    public RegisterRequestDTO(String name, String email, String password, String role, Integer subjectId) {
        super();
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.subjectId = subjectId;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor completo.
     *
     * @param name Nombre completo
     * @param email Correo electrónico
     * @param password Contraseña
     * @param role Rol del usuario
     * @param subjectId ID de la asignatura (para maestros)
     * @param courseId ID del curso (para estudiantes)
     */
    public RegisterRequestDTO(String name, String email, String password, String role, Integer subjectId, Integer courseId) {
        super();
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.subjectId = subjectId;
        this.courseId = courseId;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Método Factory para crear un RegisterRequestDTO para docente.
     * Implementa patrón Factory Method para instancias especializadas.
     *
     * @param name Nombre completo
     * @param email Correo electrónico
     * @param password Contraseña
     * @param subjectId ID de la asignatura
     * @return RegisterRequestDTO configurado para docente
     */
    public static RegisterRequestDTO createTeacher(String name, String email, String password, Integer subjectId) {
        return new RegisterRequestDTO(name, email, password, "MAESTRO", subjectId);
    }

    /**
     * Método Factory para crear un RegisterRequestDTO para estudiante.
     *
     * @param name Nombre completo
     * @param email Correo electrónico
     * @param password Contraseña
     * @param courseId ID del curso
     * @return RegisterRequestDTO configurado para estudiante
     */
    public static RegisterRequestDTO createStudent(String name, String email, String password, Integer courseId) {
        return new RegisterRequestDTO(name, email, password, "ESTUDIANTE", courseId);
    }

    /**
     * Método Factory para crear un RegisterRequestDTO vacío.
     * Implementa patrón Factory Method para instancias comunes.
     *
     * @return RegisterRequestDTO con valores por defecto
     */
    public static RegisterRequestDTO empty() {
        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Obtiene el nombre del usuario (primera parte).
     *
     * @return Nombre, o cadena vacía si no está disponible
     */
    public String getFirstName() {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        String[] parts = name.trim().split("\\s+", 2);
        return parts[0];
    }

    /**
     * Obtiene el apellido del usuario (segunda parte).
     *
     * @return Apellido, o cadena vacía si no está disponible
     */
    public String getLastName() {
        if (name == null || name.trim().isEmpty()) {
            return "";
        }
        String[] parts = name.trim().split("\\s+", 2);
        return parts.length > 1 ? parts[1] : "";
    }

    /**
     * Verifica si el rol es válido.
     *
     * @return true si el rol está en la lista de roles permitidos
     */
    public boolean hasValidRole() {
        List<String> validRoles = Arrays.asList("MAESTRO", "ESTUDIANTE", "COORDINADOR", "DIRECTOR_DE_AREA");
        return role != null && validRoles.contains(role.toUpperCase());
    }

    /**
     * Valida si el DTO tiene todos los campos obligatorios según el rol.
     * Método de validación de negocio completa.
     *
     * @return true si todos los campos requeridos están presentes y válidos
     */
    @Override
    public boolean isValid() {
        if (name == null || name.trim().isEmpty() ||
            email == null || !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$") ||
            password == null || !password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$") ||
            !hasValidRole()) {
            return false;
        }

        // Validaciones específicas por rol
        if ("MAESTRO".equals(role)) {
            return subjectId != null && subjectId > 0;
        } else if ("ESTUDIANTE".equals(role)) {
            return courseId != null && courseId > 0;
        }

        return true;
    }

    /**
     * Sanitiza los datos de entrada.
     * Método de utilidad para normalización de datos.
     */
    public void sanitize() {
        if (name != null) {
            name = name.trim();
        }
        if (email != null) {
            email = email.trim().toLowerCase();
        }
        if (role != null) {
            role = role.toUpperCase();
        }
    }

    /**
     * Obtiene una representación resumida del registro.
     * Formato: "[role]: [name] ([email])"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        return String.format("%s: %s (%s)",
                role != null ? role : "Sin rol",
                name != null ? name : "Sin nombre",
                email != null ? email : "Sin email");
    }
}