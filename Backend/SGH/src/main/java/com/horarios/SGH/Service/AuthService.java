package com.horarios.SGH.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.horarios.SGH.Model.Role;
import com.horarios.SGH.Model.Roles;
import com.horarios.SGH.Model.TeacherSubject;
import com.horarios.SGH.Model.courses;
import com.horarios.SGH.Model.subjects;
import com.horarios.SGH.Model.teachers;
import com.horarios.SGH.Model.users;
import com.horarios.SGH.Model.People;
import com.horarios.SGH.Model.AccountStatus;
import com.horarios.SGH.Model.NotificationType;
import com.horarios.SGH.Repository.Iusers;
import com.horarios.SGH.Repository.IPeopleRepository;
import com.horarios.SGH.Repository.IRolesRepository;
import com.horarios.SGH.Repository.Icourses;
import com.horarios.SGH.Repository.Iteachers;
import com.horarios.SGH.Repository.Isubjects;
import com.horarios.SGH.Repository.TeacherSubjectRepository;
import com.horarios.SGH.DTO.LoginRequestDTO;
import com.horarios.SGH.DTO.LoginResponseDTO;
import com.horarios.SGH.DTO.InAppNotificationDTO;
import com.horarios.SGH.jwt.JwtTokenProvider;


/**
 * Servicio de autenticación para el sistema SGH.
 * Maneja registro de usuarios, login con 2FA y gestión de tokens JWT.
 */
@Service
public class AuthService {

    private final Iusers repo;
    private final IPeopleRepository peopleRepo;
    private final IRolesRepository rolesRepo;
    private final Icourses courseRepo;
    private final Iteachers teacherRepo;
    private final Isubjects subjectRepo;
    private final TeacherSubjectRepository teacherSubjectRepo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final InAppNotificationService inAppNotificationService;
    private final EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    public AuthService(Iusers repo,
                              IPeopleRepository peopleRepo,
                              IRolesRepository rolesRepo,
                              Icourses courseRepo,
                              Iteachers teacherRepo,
                              Isubjects subjectRepo,
                              TeacherSubjectRepository teacherSubjectRepo,
                              PasswordEncoder encoder,
                              AuthenticationManager authManager,
                              JwtTokenProvider jwtTokenProvider,
                              InAppNotificationService inAppNotificationService,
                              EmailService emailService) {
        this.repo = repo;
        this.peopleRepo = peopleRepo;
        this.rolesRepo = rolesRepo;
        this.courseRepo = courseRepo;
        this.teacherRepo = teacherRepo;
        this.subjectRepo = subjectRepo;
        this.teacherSubjectRepo = teacherSubjectRepo;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.inAppNotificationService = inAppNotificationService;
        this.emailService = emailService;
    }

    public String register(String name, String email, String rawPassword, Role role, Integer subjectId, Integer courseId) {
        try {
            // Validar entradas usando ValidationUtils
            ValidationUtils.validateName(name);
            ValidationUtils.validateEmail(email);
            ValidationUtils.validatePassword(rawPassword);

            if (role == null) {
                throw new IllegalArgumentException("El rol no puede ser nulo");
            }

            // Validar subjectId para maestros
            if (role == Role.MAESTRO && subjectId == null) {
                throw new IllegalArgumentException("Los maestros deben tener una materia asignada");
            }

            // Validar courseId para estudiantes
            if (role == Role.ESTUDIANTE && courseId == null) {
                throw new IllegalArgumentException("Los estudiantes deben tener un curso asignado");
            }

            // Verificar que la materia existe si se proporciona
            if (subjectId != null) {
                subjectRepo.findById(subjectId)
                    .orElseThrow(() -> new IllegalArgumentException("La materia especificada no existe"));
            }

            // Verificar que el curso existe si se proporciona
            if (courseId != null) {
                courseRepo.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("El curso especificado no existe"));
            }

            // Verificar que el email no esté en uso
            peopleRepo.findByEmail(email).ifPresent(p -> {
                throw new IllegalStateException("El correo electrónico ya está en uso");
            });

            // Obtener o crear persona
            People person = peopleRepo.findByEmail(email).orElseGet(() -> {
                People newPerson = new People(name.trim(), email.trim().toLowerCase());
                return peopleRepo.save(newPerson);
            });

            // Obtener rol
            Roles userRole = rolesRepo.findByRoleName(role.name())
                .orElseThrow(() -> new IllegalStateException("Rol no encontrado: " + role.name()));

            // Crear y guardar el nuevo usuario con estado pendiente de aprobación
            users newUser = new users(person, userRole, encoder.encode(rawPassword));
            newUser.setAccountStatus(AccountStatus.PENDING_APPROVAL);

            // Asignar curso para estudiantes
            if (role == Role.ESTUDIANTE && courseId != null) {
                courses course = courseRepo.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("El curso especificado no existe"));
                newUser.setCourse(course);
            }

            users savedUser = repo.save(newUser);

            // Crear profesor si el rol es MAESTRO
            if (role == Role.MAESTRO && subjectId != null) {
                teachers newTeacher = new teachers();
                newTeacher.setTeacherName(person.getFullName());
                teachers savedTeacher = teacherRepo.save(newTeacher);

                // Crear relación profesor-materia
                subjects subject = subjectRepo.findById(subjectId)
                    .orElseThrow(() -> new IllegalArgumentException("Materia no encontrada"));
                TeacherSubject teacherSubject = new TeacherSubject();
                teacherSubject.setTeacher(savedTeacher);
                teacherSubject.setSubject(subject);
                teacherSubjectRepo.save(teacherSubject);

                System.out.println("Profesor creado exitosamente: " + savedTeacher.getId());
            }

            System.out.println("Usuario registrado exitosamente: " + savedUser.getUserId());

            // Enviar notificación a todos los coordinadores (no bloquear el registro si falla)
            try {
                notifyCoordinatorsOfNewUser(savedUser);
            } catch (Exception e) {
                System.err.println("Error notificando coordinadores, pero registro exitoso: " + e.getMessage());
            }

            return "Usuario registrado correctamente. Pendiente de aprobación por el coordinador.";
        } catch (Exception e) {
            System.err.println("Error en registro: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }


    /**
     * Login directo con email y contraseña, devuelve token JWT.
     *
     * @param req DTO con email y contraseña
     * @return DTO con token JWT
     */
    public LoginResponseDTO login(LoginRequestDTO req) {
        // Validar entrada
        ValidationUtils.validateEmail(req.getEmail());
        ValidationUtils.validatePassword(req.getPassword());

        // Verificar credenciales con Spring Security
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        // Generar token JWT
        String token = jwtTokenProvider.generateToken(req.getEmail());
        return new LoginResponseDTO(token);
    }

    /**
     * Inicia el proceso de login verificando credenciales y enviando código 2FA (opcional).
     *
     * @param req DTO con email y contraseña
     * @return Mensaje de confirmación
     */
    public String initiateLogin(LoginRequestDTO req) {
        // Validar entrada
        ValidationUtils.validateEmail(req.getEmail());
        ValidationUtils.validatePassword(req.getPassword());

        // Verificar credenciales con Spring Security
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        // Generar y guardar código de verificación
        String verificationCode = generateVerificationCode();
        users user = repo.findByUserName(req.getEmail())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setVerificationCode(verificationCode);
        user.setCodeExpiration(java.time.LocalDateTime.now().plusMinutes(10));
        repo.save(user);

        // Enviar código por email usando EmailService
        try {
            emailService.sendVerificationEmail(user.getPerson().getEmail(), verificationCode);
        } catch (Exception e) {
            System.err.println("Error delegando envío de verificación: " + e.getMessage());
        }

        return "Código de verificación enviado al correo electrónico";
    }

    /**
     * Verifica el código 2FA y genera token JWT si es válido.
     *
     * @param email Email del usuario
     * @param code Código de verificación
     * @return DTO con token JWT
     */
    public LoginResponseDTO verifyCode(String email, String code) {
        // Validar entrada
        ValidationUtils.validateEmail(email);
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("El código de verificación es obligatorio");
        }

        // Buscar usuario
        users user = repo.findByUserName(email)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Validar código
        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code.trim())) {
            throw new RuntimeException("Código de verificación inválido");
        }

        // Verificar expiración
        if (user.getCodeExpiration().isBefore(java.time.LocalDateTime.now())) {
            throw new RuntimeException("Código de verificación expirado");
        }

        // Verificar que la cuenta esté activa (aprobada)
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Su cuenta está pendiente de aprobación por el coordinador");
        }

        // Limpiar código usado y generar token
        user.setVerificationCode(null);
        user.setCodeExpiration(null);
        repo.save(user);

        String token = jwtTokenProvider.generateToken(email);
        return new LoginResponseDTO(token);
    }

    /**
     * Genera un código de verificación de 6 dígitos.
     *
     * @return Código de verificación como String
     */
    private String generateVerificationCode() {
        java.util.Random random = new java.util.Random();
        int code = 100000 + random.nextInt(900000); // Código de 6 dígitos
        return String.valueOf(code);
    }

    public users getProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return repo.findByUserName(email).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    public void updateUserName(String newName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        users user = repo.findByUserName(email).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        user.getPerson().setFullName(newName);
        peopleRepo.save(user.getPerson());
    }

    public void updateUserEmail(String newEmail) {
        ValidationUtils.validateEmail(newEmail);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentEmail = authentication.getName();
        users user = repo.findByUserName(currentEmail).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar que el nuevo email no esté en uso por otro usuario
        peopleRepo.findByEmail(newEmail).ifPresent(p -> {
            if (p.getPersonId() != user.getPerson().getPersonId()) {
                throw new IllegalStateException("El correo electrónico ya está en uso");
            }
        });

        user.getPerson().setEmail(newEmail.trim().toLowerCase());
        peopleRepo.save(user.getPerson());
    }

    /**
     * Envía notificación de bienvenida al nuevo usuario registrado
     */
    private void sendWelcomeNotification(users newUser) {
        try {
            // Crear DTO de notificación de bienvenida
            com.horarios.SGH.DTO.NotificationDTO welcomeNotification = new com.horarios.SGH.DTO.NotificationDTO();
            welcomeNotification.setRecipientEmail(newUser.getPerson().getEmail());
            welcomeNotification.setRecipientName(newUser.getPerson().getFullName());
            welcomeNotification.setRecipientRole(newUser.getRole().getRoleName());
            welcomeNotification.setNotificationType("GENERAL_SYSTEM_NOTIFICATION");
            welcomeNotification.setSubject("¡Bienvenido a SGH - Sistema de Gestión de Horarios!");
            welcomeNotification.setContent("Tu cuenta ha sido creada exitosamente. Ya puedes acceder a todas las funcionalidades disponibles para tu rol en el sistema.");
            welcomeNotification.setSenderName("Sistema SGH");
            welcomeNotification.setIsHtml(true);

            // Enviar notificación de forma asíncrona
            notificationService.validateAndPrepareNotification(welcomeNotification);
            notificationService.sendNotificationAsync(welcomeNotification);

            System.out.println("Notificación de bienvenida enviada a: " + newUser.getPerson().getEmail());

        } catch (Exception e) {
            System.err.println("Error enviando notificación de bienvenida: " + e.getMessage());
            // No lanzar excepción para no fallar el registro
        }
    }

    /**
     * Notifica a todos los coordinadores sobre un nuevo usuario pendiente de aprobación
     */
    private void notifyCoordinatorsOfNewUser(users newUser) {
        try {
            System.out.println("=== NOTIFICANDO COORDINADORES ===");
            System.out.println("Nuevo usuario: " + newUser.getUserId() + " - " +
                             (newUser.getPerson() != null ? newUser.getPerson().getFullName() : "Sin nombre"));

            // Buscar coordinadores
            System.out.println("Buscando coordinadores en la base de datos...");
            java.util.List<users> coordinators = repo.findByRoleNameWithDetails("COORDINADOR");
            System.out.println("Encontrados " + coordinators.size() + " coordinadores");

            if (coordinators.isEmpty()) {
                System.out.println("⚠️ No se encontraron coordinadores en la base de datos");
                System.out.println("Creando notificación de prueba para usuario ID 1 (admin por defecto)");
                // Crear notificación de prueba para el usuario 1 (admin por defecto)
                InAppNotificationDTO testNotification = new InAppNotificationDTO();
                testNotification.setUserId(1); // ID por defecto para testing
                testNotification.setNotificationType(NotificationType.COORDINATOR_USER_REGISTRATION_PENDING.name());
                testNotification.setTitle("Nuevo usuario pendiente de aprobación");
                testNotification.setMessage(String.format(
                    "El usuario %s (%s) con rol %s solicita registro en el sistema.",
                    newUser.getPerson() != null ? newUser.getPerson().getFullName() : "N/A",
                    newUser.getPerson() != null ? newUser.getPerson().getEmail() : "N/A",
                    newUser.getRole() != null ? newUser.getRole().getRoleName() : "N/A"
                ));
                testNotification.setPriority("HIGH");
                testNotification.setCategory("user_registration");
                testNotification.setActionUrl("/dashboard/users/pending");
                testNotification.setActionText("Revisar solicitudes");

                inAppNotificationService.sendInAppNotificationAsync(testNotification)
                    .thenAccept(notification -> {
                        System.out.println("✅ Notificación de prueba creada exitosamente para admin");
                    })
                    .exceptionally(ex -> {
                        System.err.println("❌ Error creando notificación de prueba: " + ex.getMessage());
                        return null;
                    });
                return;
            }

            // Enviar notificación a cada coordinador encontrado
            for (users coordinator : coordinators) {
                System.out.println("Enviando notificación al coordinador: " + coordinator.getUserId() +
                                 " - " + (coordinator.getPerson() != null ? coordinator.getPerson().getFullName() : "Sin nombre"));

                InAppNotificationDTO notification = new InAppNotificationDTO();
                notification.setUserId(coordinator.getUserId());
                notification.setNotificationType(NotificationType.COORDINATOR_USER_REGISTRATION_PENDING.name());
                notification.setTitle("Nuevo usuario pendiente de aprobación");
                notification.setMessage(String.format(
                    "El usuario %s (%s) con rol %s solicita registro en el sistema.",
                    newUser.getPerson() != null ? newUser.getPerson().getFullName() : "N/A",
                    newUser.getPerson() != null ? newUser.getPerson().getEmail() : "N/A",
                    newUser.getRole() != null ? newUser.getRole().getRoleName() : "N/A"
                ));
                notification.setPriority("HIGH");
                notification.setCategory("user_registration");
                notification.setActionUrl("/dashboard/users/pending");
                notification.setActionText("Revisar solicitudes");

                // Enviar notificación usando el servicio
                final int coordinatorId = coordinator.getUserId();
                inAppNotificationService.sendInAppNotificationAsync(notification)
                    .thenAccept(savedNotification -> {
                        System.out.println("✅ Notificación enviada exitosamente al coordinador " + coordinatorId);
                    })
                    .exceptionally(ex -> {
                        System.err.println("❌ Error enviando notificación al coordinador " + coordinatorId + ": " + ex.getMessage());
                        return null;
                    });
            }

            System.out.println("=== FIN NOTIFICACIÓN COORDINADORES ===");

        } catch (Exception e) {
            System.err.println("❌ Error notificando coordinadores: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Aprueba un usuario pendiente de aprobación
     */
    public String approveUser(int userId) {
        users user = repo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (user.getAccountStatus() != AccountStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("El usuario no está pendiente de aprobación");
        }

        user.setAccountStatus(AccountStatus.ACTIVE);
        repo.save(user);

        // Notificar al usuario
        notifyUserApproval(user);

        return "Usuario aprobado exitosamente";
    }

    /**
     * Rechaza un usuario pendiente de aprobación
     */
    public String rejectUser(int userId, String reason) {
        users user = repo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (user.getAccountStatus() != AccountStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("El usuario no está pendiente de aprobación");
        }

        user.setAccountStatus(AccountStatus.INACTIVE);
        repo.save(user);

        // Notificar al usuario
        notifyUserRejection(user, reason);

        return "Usuario rechazado";
    }

    /**
     * Notifica al usuario que su registro fue aprobado
     */
    private void notifyUserApproval(users user) {
        try {
            // Notificación in-app
            InAppNotificationDTO notification = new InAppNotificationDTO();
            notification.setUserId(user.getUserId());
            notification.setNotificationType(NotificationType.USER_REGISTRATION_APPROVED.name());
            notification.setTitle("¡Registro aprobado!");
            notification.setMessage("Su solicitud de registro ha sido aprobada. Ya puede iniciar sesión en el sistema.");
            notification.setPriority("HIGH");
            notification.setCategory("user_registration");

            inAppNotificationService.sendInAppNotificationAsync(notification)
                .exceptionally(ex -> {
                    System.err.println("Error notificando aprobación al usuario " + user.getUserId() + ": " + ex.getMessage());
                    return null;
                });

            // Enviar email de aprobación usando EmailService
            try {
                emailService.sendApprovalEmail(user.getPerson().getEmail(), user.getPerson().getFullName());
            } catch (Exception e) {
                System.err.println("Error delegando envío de email de aprobación: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Error notificando aprobación al usuario: " + e.getMessage());
        }
    }

    /**
     * Notifica al usuario que su registro fue rechazado
     */
    private void notifyUserRejection(users user, String reason) {
        try {
            // Notificación in-app
            InAppNotificationDTO notification = new InAppNotificationDTO();
            notification.setUserId(user.getUserId());
            notification.setNotificationType(NotificationType.USER_REGISTRATION_REJECTED.name());
            notification.setTitle("Registro rechazado");
            notification.setMessage(String.format(
                "Su solicitud de registro ha sido rechazada.%s",
                reason != null && !reason.trim().isEmpty() ? " Motivo: " + reason : ""
            ));
            notification.setPriority("MEDIUM");
            notification.setCategory("user_registration");

            inAppNotificationService.sendInAppNotificationAsync(notification)
                .exceptionally(ex -> {
                    System.err.println("Error notificando rechazo al usuario " + user.getUserId() + ": " + ex.getMessage());
                    return null;
                });

            // Enviar email de rechazo usando EmailService
            try {
                emailService.sendRejectionEmail(user.getPerson().getEmail(), user.getPerson().getFullName(), reason);
            } catch (Exception e) {
                System.err.println("Error delegando envío de email de rechazo: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Error notificando rechazo al usuario: " + e.getMessage());
        }
    }

    /**
     * Solicita el restablecimiento de contraseña enviando un código de verificación por email
     *
     * @param email Email del usuario
     * @return Mensaje de confirmación
     */
    public String requestPasswordReset(String email) {
        try {
            ValidationUtils.validateEmail(email);

            // Buscar usuario por email
            users user = repo.findByUserName(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró una cuenta con este email"));

            // Verificar que la cuenta esté activa
            if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                throw new IllegalStateException("La cuenta no está activa. Contacte al administrador.");
            }

            // Generar y guardar código de reset de contraseña
            String resetCode = generateVerificationCode();
            user.setPasswordResetCode(resetCode);
            user.setPasswordResetExpiration(java.time.LocalDateTime.now().plusMinutes(10)); // 10 minutos para reset
            repo.save(user);

            // Enviar código por email usando EmailService
            try {
                emailService.sendPasswordResetEmail(user.getPerson().getEmail(), resetCode, user.getPerson().getFullName());
            } catch (Exception e) {
                System.err.println("Error delegando envío de reset: " + e.getMessage());
            }

            return "Se ha enviado un código de verificación a su email para restablecer la contraseña";
        } catch (Exception e) {
            System.err.println("Error solicitando reset de contraseña: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Restablece la contraseña usando el código de verificación enviado por email
     *
     * @param email Email del usuario
     * @param verificationCode Código de verificación
     * @param newPassword Nueva contraseña
     * @return Mensaje de confirmación
     */
    public String resetPassword(String email, String verificationCode, String newPassword) {
        try {
            ValidationUtils.validateEmail(email);
            ValidationUtils.validatePassword(newPassword);

            // Buscar usuario
            users user = repo.findByUserName(email.trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            // Validar código de reset
            if (user.getPasswordResetCode() == null || !user.getPasswordResetCode().equals(verificationCode.trim())) {
                throw new RuntimeException("Código de verificación inválido");
            }

            // Verificar expiración
            if (user.getPasswordResetExpiration().isBefore(java.time.LocalDateTime.now())) {
                throw new RuntimeException("Código de verificación expirado");
            }

            // Verificar que la cuenta esté activa
            if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                throw new RuntimeException("La cuenta no está activa");
            }

            // Actualizar contraseña
            user.setPasswordHash(encoder.encode(newPassword));
            user.setPasswordResetCode(null); // Limpiar código usado
            user.setPasswordResetExpiration(null);
            repo.save(user);

            return "Contraseña restablecida exitosamente";
        } catch (Exception e) {
            System.err.println("Error restableciendo contraseña: " + e.getMessage());
            throw e;
        }
    }

    // Email templates and sending moved to `EmailService` and `HtmlTemplateService`.
    // Private inline email helper methods were removed to keep AuthService single-responsibility.

    /**
     * Obtiene lista de usuarios pendientes de aprobación
     */
    public java.util.List<users> getPendingUsers() {
        try {
            System.out.println("Buscando usuarios pendientes de aprobación...");
            java.util.List<users> pendingUsers = repo.findByAccountStatusWithDetails(AccountStatus.PENDING_APPROVAL);
            System.out.println("Encontrados " + pendingUsers.size() + " usuarios pendientes");
            return pendingUsers;
        } catch (Exception e) {
            System.err.println("Error obteniendo usuarios pendientes: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    // Approval email sending moved to EmailService; helper removed from AuthService.

    // Private inline email helper methods removed. Email sending delegated to `EmailService`.
}