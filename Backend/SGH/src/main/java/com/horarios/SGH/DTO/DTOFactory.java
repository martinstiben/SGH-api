package com.horarios.SGH.DTO;

import java.time.LocalDateTime;

/**
 * Factory para creación de DTOs del sistema SGH.
 * Implementa patrón Factory Method para crear instancias de DTOs
 * de manera centralizada y consistente.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de crear DTOs
 * - DIP: No depende de implementaciones concretas
 *
 * @author Sistema SGH
 * @version 1.0
 */
/**
 * Factory para creación de DTOs del sistema SGH.
 * Implementa patrón Abstract Factory para crear instancias de DTOs
 * de manera centralizada y consistente.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de crear DTOs
 * - DIP: No depende de implementaciones concretas
 * - OCP: Abierto para extensión
 *
 * Patrones de diseño aplicados:
 * - Abstract Factory: Para creación de familias de DTOs
 * - Factory Method: Para creación de instancias individuales
 *
 * @author Sistema SGH
 * @version 1.0
 */
public class DTOFactory {

    /**
     * Crea un CourseDTO básico.
     *
     * @param courseId ID del curso
     * @param courseName Nombre del curso
     * @return CourseDTO configurado
     */
    /**
     * Crea un CourseDTO básico utilizando el patrón Abstract Factory.
     * Este método demuestra cómo el factory puede crear instancias
     * que extienden AbstractDTO con funcionalidad común.
     *
     * @param courseId ID del curso
     * @param courseName Nombre del curso
     * @return CourseDTO configurado que extiende AbstractDTO
     */
    public static CourseDTO createCourse(int courseId, String courseName) {
        CourseDTO dto = CourseDTO.empty();
        dto.setCourseId(courseId);
        dto.setCourseName(courseName);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un SubjectDTO básico.
     *
     * @param subjectId ID de la asignatura
     * @param subjectName Nombre de la asignatura
     * @return SubjectDTO configurado
     */
    /**
     * Crea un SubjectDTO básico utilizando el patrón Abstract Factory.
     * Este método demuestra la creación de DTOs que implementan
     * los métodos abstractos de validación y resumen.
     *
     * @param subjectId ID de la asignatura
     * @param subjectName Nombre de la asignatura
     * @return SubjectDTO configurado que extiende AbstractDTO
     */
    public static SubjectDTO createSubject(int subjectId, String subjectName) {
        SubjectDTO dto = SubjectDTO.empty();
        dto.setSubjectId(subjectId);
        dto.setSubjectName(subjectName);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un TeacherDTO básico.
     *
     * @param teacherId ID del docente
     * @param teacherName Nombre del docente
     * @return TeacherDTO configurado
     */
    /**
     * Crea un TeacherDTO básico utilizando el patrón Abstract Factory.
     * Este método demuestra la creación de DTOs con validación
     * de nombres y métodos de resumen implementados.
     *
     * @param teacherId ID del docente
     * @param teacherName Nombre del docente
     * @return TeacherDTO configurado que extiende AbstractDTO
     */
    public static TeacherDTO createTeacher(int teacherId, String teacherName) {
        TeacherDTO dto = TeacherDTO.empty();
        dto.setTeacherId(teacherId);
        dto.setTeacherName(teacherName);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un ScheduleDTO básico.
     *
     * @param courseId ID del curso
     * @param teacherId ID del profesor
     * @param subjectId ID de la materia
     * @param day Día
     * @param startTime Hora inicio
     * @param endTime Hora fin
     * @param scheduleName Nombre del horario
     * @return ScheduleDTO configurado
     */
    public static ScheduleDTO createSchedule(Integer courseId, Integer teacherId, Integer subjectId,
                                           String day, String startTime, String endTime, String scheduleName) {
        ScheduleDTO dto = new ScheduleDTO();
        dto.setCourseId(courseId);
        dto.setTeacherId(teacherId);
        dto.setSubjectId(subjectId);
        dto.setDay(day);
        dto.setStartTime(startTime);
        dto.setEndTime(endTime);
        dto.setScheduleName(scheduleName);
        return dto;
    }

    /**
     * Crea un UserDTO básico.
     *
     * @param userId ID del usuario
     * @param username Nombre de usuario
     * @param email Email
     * @param firstName Nombre
     * @param lastName Apellido
     * @return UserDTO configurado
     */
    public static UserDTO createUser(Long userId, String username, String email, String firstName, String lastName) {
        UserDTO dto = new UserDTO();
        dto.setUserId(userId);
        dto.setUsername(username);
        dto.setEmail(email);
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un usersDTO básico.
     *
     * @param userName Nombre de usuario
     * @param password Contraseña
     * @param role Rol
     * @return usersDTO configurado
     */
    public static usersDTO createUsers(String userName, String password, String role) {
        usersDTO dto = new usersDTO();
        dto.setUserName(userName);
        dto.setPassword(password);
        dto.setRole(role);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un RoleDTO básico.
     *
     * @param roleName Nombre del rol
     * @param description Descripción
     * @return RoleDTO configurado
     */
    public static RoleDTO createRole(String roleName, String description) {
        RoleDTO dto = RoleDTO.empty();
        dto.setRoleName(roleName);
        dto.setDescription(description);
        dto.setActive(true);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un LoginRequestDTO básico.
     *
     * @param email Email del usuario
     * @param password Contraseña
     * @return LoginRequestDTO configurado
     */
    public static LoginRequestDTO createLoginRequest(String email, String password) {
        LoginRequestDTO dto = LoginRequestDTO.create(email, password);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un LoginResponseDTO básico.
     *
     * @param token Token JWT
     * @return LoginResponseDTO configurado
     */
    public static LoginResponseDTO createLoginResponse(String token) {
        LoginResponseDTO dto = new LoginResponseDTO(token);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un RegisterRequestDTO básico.
     *
     * @param name Nombre completo
     * @param email Email
     * @param password Contraseña
     * @param role Rol
     * @return RegisterRequestDTO configurado
     */
    public static RegisterRequestDTO createRegisterRequest(String name, String email, String password, String role) {
        RegisterRequestDTO dto = new RegisterRequestDTO();
        dto.setName(name);
        dto.setEmail(email);
        dto.setPassword(password);
        dto.setRole(role);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un NotificationDTO básico.
     *
     * @param recipientEmail Email del destinatario
     * @param recipientName Nombre del destinatario
     * @param subject Asunto
     * @param content Contenido
     * @return NotificationDTO configurado
     */
    public static NotificationDTO createNotification(String recipientEmail, String recipientName, String subject, String content) {
        NotificationDTO dto = NotificationDTO.create(recipientEmail, recipientName, subject, content);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un PasswordResetDTO básico.
     *
     * @param email Email
     * @param verificationCode Código de verificación
     * @param newPassword Nueva contraseña
     * @return PasswordResetDTO configurado
     */
    public static PasswordResetDTO createPasswordReset(String email, String verificationCode, String newPassword) {
        PasswordResetDTO dto = PasswordResetDTO.create(email, verificationCode, newPassword);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un PasswordResetRequestDTO básico.
     *
     * @param email Email
     * @return PasswordResetRequestDTO configurado
     */
    public static PasswordResetRequestDTO createPasswordResetRequest(String email) {
        PasswordResetRequestDTO dto = PasswordResetRequestDTO.create(email);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un PermissionDTO básico.
     *
     * @param permissionName Nombre del permiso
     * @param description Descripción
     * @return PermissionDTO configurado
     */
    public static PermissionDTO createPermission(String permissionName, String description) {
        PermissionDTO dto = PermissionDTO.create(permissionName, description);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un responseDTO básico.
     *
     * @param status Estado
     * @param message Mensaje
     * @return responseDTO configurado
     */
    public static responseDTO createResponse(String status, String message) {
        responseDTO dto = new responseDTO(status, message);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un RevokedTokenDTO básico.
     *
     * @param token Token revocado
     * @param userId ID del usuario
     * @return RevokedTokenDTO configurado
     */
    public static RevokedTokenDTO createRevokedToken(String token, Long userId) {
        RevokedTokenDTO dto = RevokedTokenDTO.createForLogout(token, userId);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un TeacherAvailabilityDTO básico.
     *
     * @param teacherId ID del profesor
     * @param day Día de la semana
     * @return TeacherAvailabilityDTO configurado
     */
    public static TeacherAvailabilityDTO createTeacherAvailability(Integer teacherId, com.horarios.SGH.Model.Days day) {
        TeacherAvailabilityDTO dto = TeacherAvailabilityDTO.empty();
        dto.setTeacherId(teacherId);
        dto.setDay(day);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un UserCredentialsDTO básico.
     *
     * @param passwordHash Hash de la contraseña
     * @return UserCredentialsDTO configurado
     */
    public static UserCredentialsDTO createUserCredentials(String passwordHash) {
        UserCredentialsDTO dto = UserCredentialsDTO.create(passwordHash);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un UserSecurityDTO básico.
     *
     * @param securityId ID de seguridad
     * @return UserSecurityDTO configurado
     */
    public static UserSecurityDTO createUserSecurity(Long securityId) {
        UserSecurityDTO dto = new UserSecurityDTO(securityId);
        dto.setCreatedAt(LocalDateTime.now());
        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un VerifyCodeDTO básico.
     *
     * @param email Email
     * @param code Código de verificación
     * @return VerifyCodeDTO configurado
     */
    public static VerifyCodeDTO createVerifyCode(String email, String code) {
        VerifyCodeDTO dto = VerifyCodeDTO.create(email, code);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un InAppNotificationResponseDTO básico.
     *
     * @param notificationId ID de la notificación
     * @param userId ID del usuario
     * @param title Título
     * @param message Mensaje
     * @return InAppNotificationResponseDTO configurado
     */
    public static InAppNotificationResponseDTO createInAppNotificationResponse(Long notificationId, Long userId, String title, String message) {
        InAppNotificationResponseDTO dto = InAppNotificationResponseDTO.create(notificationId, userId, title, message);
        dto.setCreatedAt(LocalDateTime.now());
        return dto;
    }

    /**
     * Crea un DTO vacío genérico.
     * Método de conveniencia para inicialización.
     *
     * @param dtoClass Clase del DTO
     * @return DTO vacío
     */
    public static <T extends AbstractDTO> T createEmpty(Class<T> dtoClass) {
        try {
            T dto = dtoClass.getDeclaredConstructor().newInstance();
            dto.setCreatedAt(LocalDateTime.now());
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Error creando DTO vacío", e);
        }
    }

    /**
     * Método Factory genérico que delega a los métodos específicos.
     * Implementa patrón Abstract Factory con dispatch dinámico.
     *
     * @param dtoType Tipo de DTO a crear
     * @param params Parámetros para la creación
     * @return DTO creado según el tipo especificado
     * @throws IllegalArgumentException si el tipo no es soportado
     */
    @SuppressWarnings("unchecked")
    public static <T extends AbstractDTO> T createDTO(String dtoType, Object... params) {
        switch (dtoType.toLowerCase()) {
            case "course":
                if (params.length >= 2) {
                    return (T) createCourse((Integer) params[0], (String) params[1]);
                }
                break;
            case "subject":
                if (params.length >= 2) {
                    return (T) createSubject((Integer) params[0], (String) params[1]);
                }
                break;
            case "teacher":
                if (params.length >= 2) {
                    return (T) createTeacher((Integer) params[0], (String) params[1]);
                }
                break;
            case "user":
                if (params.length >= 5) {
                    return (T) createUser((Long) params[0], (String) params[1], (String) params[2],
                                         (String) params[3], (String) params[4]);
                }
                break;
            case "users":
                if (params.length >= 3) {
                    return (T) createUsers((String) params[0], (String) params[1], (String) params[2]);
                }
                break;
            case "role":
                if (params.length >= 2) {
                    return (T) createRole((String) params[0], (String) params[1]);
                }
                break;
            case "login_request":
                if (params.length >= 2) {
                    return (T) createLoginRequest((String) params[0], (String) params[1]);
                }
                break;
            case "login_response":
                if (params.length >= 1) {
                    return (T) createLoginResponse((String) params[0]);
                }
                break;
            case "register_request":
                if (params.length >= 4) {
                    return (T) createRegisterRequest((String) params[0], (String) params[1], 
                                                   (String) params[2], (String) params[3]);
                }
                break;
            case "notification":
                if (params.length >= 4) {
                    return (T) createNotification((String) params[0], (String) params[1], 
                                                (String) params[2], (String) params[3]);
                }
                break;
            case "password_reset":
                if (params.length >= 3) {
                    return (T) createPasswordReset((String) params[0], (String) params[1], 
                                                 (String) params[2]);
                }
                break;
            case "password_reset_request":
                if (params.length >= 1) {
                    return (T) createPasswordResetRequest((String) params[0]);
                }
                break;
            case "permission":
                if (params.length >= 2) {
                    return (T) createPermission((String) params[0], (String) params[1]);
                }
                break;
            case "response":
                if (params.length >= 2) {
                    return (T) createResponse((String) params[0], (String) params[1]);
                }
                break;
            case "revoked_token":
                if (params.length >= 2) {
                    return (T) createRevokedToken((String) params[0], (Long) params[1]);
                }
                break;
            case "teacher_availability":
                if (params.length >= 2) {
                    return (T) createTeacherAvailability((Integer) params[0], (com.horarios.SGH.Model.Days) params[1]);
                }
                break;
            case "user_credentials":
                if (params.length >= 1) {
                    return (T) createUserCredentials((String) params[0]);
                }
                break;
            case "user_security":
                if (params.length >= 1) {
                    return (T) createUserSecurity((Long) params[0]);
                }
                break;
            case "verify_code":
                if (params.length >= 2) {
                    return (T) createVerifyCode((String) params[0], (String) params[1]);
                }
                break;
            case "in_app_notification_response":
                if (params.length >= 4) {
                    return (T) createInAppNotificationResponse((Long) params[0], (Long) params[1], 
                                                             (String) params[2], (String) params[3]);
                }
                break;
        }
        throw new IllegalArgumentException("Tipo de DTO no soportado o parámetros insuficientes: " + dtoType);
    }
}