package com.horarios.SGH.Service;

import org.springframework.beans.factory.annotation.Autowired; 
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.horarios.SGH.Model.Role;
import com.horarios.SGH.Model.Roles;
import com.horarios.SGH.Model.users;
import com.horarios.SGH.Model.People;
import com.horarios.SGH.Model.courses;
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

    @Autowired
    private JavaMailSender mailSender;

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
                             InAppNotificationService inAppNotificationService) {
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
    }

    public String register(String name, String email, String rawPassword, Role role, Integer courseId) {
        try {
            // Validar entradas usando ValidationUtils
            ValidationUtils.validateName(name);
            ValidationUtils.validateEmail(email);
            ValidationUtils.validatePassword(rawPassword);

            if (role == null) {
                throw new IllegalArgumentException("El rol no puede ser nulo");
            }

            // Validar courseId para estudiantes
            if (role == Role.ESTUDIANTE) {
                if (courseId == null) {
                    throw new IllegalArgumentException("Los estudiantes deben seleccionar un curso");
                }
                // Verificar que el curso existe
                courseRepo.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("El curso seleccionado no existe"));
            } else {
                if (courseId != null) {
                    throw new IllegalArgumentException("Solo los estudiantes pueden seleccionar un curso");
                }
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

            // Asignar curso si es estudiante
            if (role == Role.ESTUDIANTE) {
                courses selectedCourse = courseRepo.findById(courseId).get();
                newUser.setCourse(selectedCourse);
            }

            users savedUser = repo.save(newUser);

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

        // Enviar código por email (simulado)
        sendVerificationEmail(user.getPerson().getEmail(), verificationCode);

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

    /**
     * Envía el código de verificación por email usando JavaMailSender con formato HTML.
     *
     * @param email Dirección de email del destinatario
     * @param code Código de verificación
     */
    private void sendVerificationEmail(String email, String code) {
    try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(email);
        helper.setSubject("Código de Verificación - SGH");

        String htmlContent = "<!DOCTYPE html>" +
            "<html lang='es'>" +
            "<head>" +
            "<meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<title>Verificación de Seguridad - SGH</title>" +
            "</head>" +
            "<body style='margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif; background: linear-gradient(135deg, #e8eaed 0%, #d1d5db 100%); min-height: 100vh; padding: 40px 20px;'>" +
            "<table cellpadding='0' cellspacing='0' border='0' width='100%' style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 16px; box-shadow: 0 8px 32px rgba(0,0,0,0.12); overflow: hidden; border: 1px solid #e5e7eb;'>" +
            "<tr>" +
            "<td style='background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%); padding: 55px 40px; text-align: center;'>" +
            "<div style='margin: 0 auto 28px;'>" +
            "<div style='width: 120px; height: 120px; background: #ffffff; border-radius: 28px; margin: 0 auto; box-shadow: 0 12px 35px rgba(0,0,0,0.35); display: table;'>" +
            "<div style='display: table-cell; vertical-align: middle; text-align: center; padding: 20px;'>" +
            "<span style='font-size: 52px; font-weight: 700; color: #2c3e50; font-family: \"Segoe UI\", -apple-system, BlinkMacSystemFont, Arial, sans-serif; letter-spacing: 6px; line-height: 1; text-transform: uppercase;'>SGH</span>" +
            "</div>" +
            "</div>" +
            "</div>" +
            "<h1 style='margin: 0 0 10px 0; font-size: 34px; font-weight: 700; color: #ffffff; text-shadow: 0 2px 10px rgba(0,0,0,0.4); letter-spacing: -0.5px;'>Verificación de Seguridad</h1>" +
            "<p style='margin: 0; font-size: 17px; color: rgba(255,255,255,0.92); font-weight: 500; letter-spacing: 0.3px;'>Sistema de Gestión de Horarios</p>" +
            "</td>" +
            "</tr>" +
            "<tr>" +
            "<td style='padding: 50px 40px; text-align: center; background: #f9fafb;'>" +
            "<div style='margin-bottom: 15px;'>" +
            "<span style='display: inline-block; font-size: 48px;'>👋</span>" +
            "</div>" +
            "<h2 style='font-size: 26px; color: #1f2937; margin: 0 0 12px 0; font-weight: 700;'>¡Hola!</h2>" +
            "<p style='font-size: 16px; color: #4b5563; line-height: 1.7; margin: 0 0 35px 0;'>" +
            "Para proteger tu cuenta, hemos generado un código de verificación.<br>" +
            "<strong style='color: #1f2937;'>Ingresa este código en la aplicación</strong> para completar tu inicio de sesión de forma segura." +
            "</p>" +
            "<div style='background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%); border-radius: 16px; padding: 40px 30px; margin: 35px 0; box-shadow: 0 8px 24px rgba(44, 62, 80, 0.3);'>" +
            "<p style='color: rgba(255,255,255,0.95); font-size: 12px; margin: 0 0 18px 0; text-transform: uppercase; letter-spacing: 2px; font-weight: 700;'>Tu código de verificación</p>" +
            "<div style='background: rgba(255,255,255,0.15); border-radius: 12px; padding: 18px;'>" +
            "<div style='font-size: 48px; font-weight: 900; color: #ffffff; letter-spacing: 12px; font-family: \"Courier New\", Courier, monospace; text-shadow: 0 2px 8px rgba(0,0,0,0.2);'>" +
            code +
            "</div>" +
            "</div>" +
            "</div>" +
            "<div style='display: inline-block; background: #fef3c7; border: 2px solid #f59e0b; border-radius: 50px; padding: 12px 24px; margin: 25px 0; box-shadow: 0 2px 8px rgba(245, 158, 11, 0.2);'>" +
            "<span style='font-size: 14px;'>⏱️</span>" +
            "<span style='color: #92400e; font-weight: 700; font-size: 14px; margin-left: 8px;'>Expira en 10 minutos</span>" +
            "</div>" +
            "<div style='background: #fee; border: 2px solid #fca5a5; border-radius: 12px; padding: 20px; margin: 25px 0; text-align: left;'>" +
            "<p style='margin: 0; color: #7f1d1d; font-size: 14px; line-height: 1.6;'>" +
            "<span style='font-size: 18px; margin-right: 8px;'>🔒</span>" +
            "<strong>Importante:</strong> Si no solicitaste este código, alguien podría estar intentando acceder a tu cuenta. Por favor, <strong>ignora este mensaje</strong> y contacta inmediatamente con el administrador del sistema." +
            "</p>" +
            "</div>" +
            "<div style='background: #f0f9ff; border-left: 4px solid #0ea5e9; border-radius: 8px; padding: 20px; margin: 25px 0; text-align: left;'>" +
            "<p style='margin: 0 0 12px 0; color: #075985; font-size: 15px; font-weight: 700;'>" +
            "💡 Consejos de seguridad:" +
            "</p>" +
            "<ul style='margin: 0; padding-left: 20px; color: #075985; font-size: 14px; line-height: 1.8;'>" +
            "<li>Nunca compartas este código con nadie</li>" +
            "<li>SGH nunca te pedirá tu código por teléfono o correo</li>" +
            "<li>Usa contraseñas seguras y cámbialas regularmente</li>" +
            "</ul>" +
            "</div>" +
            "</td>" +
            "</tr>" +
            "<tr>" +
            "<td style='background: #f3f4f6; padding: 40px; text-align: center; border-top: 1px solid #e5e7eb;'>" +
            "<p style='color: #6b7280; font-size: 14px; line-height: 1.6; margin: 0 0 20px 0;'>" +
            "Este es un mensaje automático generado por el sistema SGH.<br>" +
            "<strong style='color: #374151;'>Por seguridad, no respondas a este correo electrónico.</strong><br>" +
            "Si necesitas ayuda, contacta al equipo de soporte técnico." +
            "</p>" +
            "<div style='margin-top: 25px; padding-top: 25px; border-top: 1px solid #e5e7eb;'>" +
            "<div style='margin-bottom: 16px;'>" +
            "<div style='width: 64px; height: 64px; background: #2c3e50; border-radius: 16px; margin: 0 auto; box-shadow: 0 6px 16px rgba(44, 62, 80, 0.4); display: table;'>" +
            "<div style='display: table-cell; vertical-align: middle; text-align: center; padding: 12px;'>" +
            "<span style='font-size: 24px; font-weight: 700; color: #ffffff; font-family: \"Segoe UI\", -apple-system, BlinkMacSystemFont, Arial, sans-serif; letter-spacing: 3px; line-height: 1; text-transform: uppercase;'>SGH</span>" +
            "</div>" +
            "</div>" +
            "</div>" +
            "<p style='color: #1f2937; font-weight: 700; font-size: 16px; margin: 0 0 6px 0;'>" +
            "Sistema de Gestión de Horarios" +
            "</p>" +
            "<p style='color: #6b7280; font-size: 13px; margin: 0;'>" +
            "Tu sistema de confianza para la gestión académica" +
            "</p>" +
            "</div>" +
            "</td>" +
            "</tr>" +
            "</table>" +
            "<div style='text-align: center; padding-top: 25px;'>" +
            "<p style='color: #94a3b8; font-size: 13px; margin: 0;'>" +
            "© 2025 Sistema de Gestión de Horarios. Todos los derechos reservados." +
            "</p>" +
            "</div>" +
            "</body>" +
            "</html>";

        helper.setText(htmlContent, true);

        mailSender.send(message);

        System.out.println("=== EMAIL ENVIADO ===");
        System.out.println("Destinatario: " + email);
        System.out.println("Código: " + code);
        System.out.println("====================");

    } catch (Exception e) {
        System.err.println("Error enviando email: " + e.getMessage());
        System.out.println("=== CÓDIGO DE VERIFICACIÓN SGH (FALLBACK) ===");
        System.out.println("Email: " + email);
        System.out.println("Código: " + code);
        System.out.println("Este código expira en 10 minutos");
        System.out.println("===========================================");
    }
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
            System.out.println("Buscando coordinadores para notificar...");
            java.util.List<users> coordinators = repo.findByRoleNameWithDetails("COORDINADOR");
            System.out.println("Encontrados " + coordinators.size() + " coordinadores");

            for (users coordinator : coordinators) {
                System.out.println("Enviando notificación al coordinador: " + coordinator.getUserId());
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
                notification.setActionUrl("/admin/users/pending");
                notification.setActionText("Revisar solicitudes");

                // Enviar notificación usando el servicio
                inAppNotificationService.sendInAppNotificationAsync(notification)
                    .exceptionally(ex -> {
                        System.err.println("Error enviando notificación al coordinador " + coordinator.getUserId() + ": " + ex.getMessage());
                        return null;
                    });
            }
        } catch (Exception e) {
            // Log error but don't fail registration
            System.err.println("Error notificando coordinadores: " + e.getMessage());
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
        } catch (Exception e) {
            System.err.println("Error notificando aprobación al usuario: " + e.getMessage());
        }
    }

    /**
     * Notifica al usuario que su registro fue rechazado
     */
    private void notifyUserRejection(users user, String reason) {
        try {
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

            // Enviar código por email
            sendPasswordResetEmail(user.getPerson().getEmail(), resetCode, user.getPerson().getFullName());

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


    /**
     * Envía el código de verificación para restablecimiento de contraseña por email
     *
     * @param email Email del destinatario
     * @param verificationCode Código de verificación
     * @param userName Nombre del usuario
     */
    private void sendPasswordResetEmail(String email, String verificationCode, String userName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Código de Verificación - Restablecimiento de Contraseña - SGH");

            String htmlContent = "<!DOCTYPE html>" +
                "<html lang='es'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>Restablecimiento de Contraseña - SGH</title>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif; background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%); min-height: 100vh; padding: 40px 20px;'>" +
                "<table cellpadding='0' cellspacing='0' border='0' width='100%' style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 16px; box-shadow: 0 8px 32px rgba(0,0,0,0.12); overflow: hidden; border: 1px solid #e5e7eb;'>" +
                "<tr>" +
                "<td style='background: linear-gradient(135deg, #6b7280 0%, #4b5563 100%); padding: 55px 40px; text-align: center;'>" +
                "<div style='margin: 0 auto 28px;'>" +
                "<div style='width: 120px; height: 120px; background: #ffffff; border-radius: 28px; margin: 0 auto; box-shadow: 0 12px 35px rgba(0,0,0,0.35); display: table;'>" +
                "<div style='display: table-cell; vertical-align: middle; text-align: center; padding: 20px;'>" +
                "<span style='font-size: 52px; font-weight: 700; color: #6b7280; font-family: \"Segoe UI\", -apple-system, BlinkMacSystemFont, Arial, sans-serif; letter-spacing: 6px; line-height: 1; text-transform: uppercase;'>🔐</span>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "<h1 style='margin: 0 0 10px 0; font-size: 34px; font-weight: 700; color: #ffffff; text-shadow: 0 2px 10px rgba(0,0,0,0.4); letter-spacing: -0.5px;'>Restablecimiento de Contraseña</h1>" +
                "<p style='margin: 0; font-size: 17px; color: rgba(255,255,255,0.92); font-weight: 500; letter-spacing: 0.3px;'>Sistema de Gestión de Horarios</p>" +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style='padding: 50px 40px; text-align: center; background: #f9fafb;'>" +
                "<div style='margin-bottom: 15px;'>" +
                "<span style='display: inline-block; font-size: 48px;'>👋</span>" +
                "</div>" +
                "<h2 style='font-size: 26px; color: #1f2937; margin: 0 0 12px 0; font-weight: 700;'>¡Hola, " + userName + "!</h2>" +
                "<p style='font-size: 16px; color: #4b5563; line-height: 1.7; margin: 0 0 35px 0;'>" +
                "Hemos recibido una solicitud para restablecer la contraseña de tu cuenta.<br>" +
                "<strong style='color: #1f2937;'>Ingresa este código en la aplicación</strong> para crear una nueva contraseña y recuperar el acceso de forma segura." +
                "</p>" +
                "<div style='background: linear-gradient(135deg, #6b7280 0%, #4b5563 100%); border-radius: 16px; padding: 40px 30px; margin: 35px 0; box-shadow: 0 8px 24px rgba(107, 114, 128, 0.3);'>" +
                "<p style='color: rgba(255,255,255,0.95); font-size: 12px; margin: 0 0 18px 0; text-transform: uppercase; letter-spacing: 2px; font-weight: 700;'>Tu código de verificación</p>" +
                "<div style='background: rgba(255,255,255,0.15); border-radius: 12px; padding: 18px;'>" +
                "<div style='font-size: 48px; font-weight: 900; color: #ffffff; letter-spacing: 12px; font-family: \"Courier New\", Courier, monospace; text-shadow: 0 2px 8px rgba(0,0,0,0.2);'>" +
                verificationCode +
                "</div>" +
                "</div>" +
                "</div>" +
                "<div style='display: inline-block; background: #fef3c7; border: 2px solid #f59e0b; border-radius: 50px; padding: 12px 24px; margin: 25px 0; box-shadow: 0 2px 8px rgba(245, 158, 11, 0.2);'>" +
                "<span style='font-size: 14px;'>⏱️</span>" +
                "<span style='color: #92400e; font-weight: 700; font-size: 14px; margin-left: 8px;'>Expira en 10 minutos</span>" +
                "</div>" +
                "<div style='background: #fee; border: 2px solid #fca5a5; border-radius: 12px; padding: 20px; margin: 25px 0; text-align: left;'>" +
                "<p style='margin: 0; color: #7f1d1d; font-size: 14px; line-height: 1.6;'>" +
                "<span style='font-size: 18px; margin-right: 8px;'>🛡️</span>" +
                "<strong>Importante:</strong> Si no solicitaste este restablecimiento, alguien podría estar intentando acceder a tu cuenta. Por favor, <strong>ignora este mensaje</strong> y contacta inmediatamente con el administrador del sistema." +
                "</p>" +
                "</div>" +
                "<div style='background: #f0f9ff; border-left: 4px solid #0ea5e9; border-radius: 8px; padding: 20px; margin: 25px 0; text-align: left;'>" +
                "<p style='margin: 0 0 12px 0; color: #075985; font-size: 15px; font-weight: 700;'>" +
                "💡 Consejos de seguridad:" +
                "</p>" +
                "<ul style='margin: 0; padding-left: 20px; color: #075985; font-size: 14px; line-height: 1.8;'>" +
                "<li>Nunca compartas este código con nadie</li>" +
                "<li>SGH nunca te pedirá tu código por teléfono o correo</li>" +
                "<li>Usa contraseñas seguras y cámbialas regularmente</li>" +
                "</ul>" +
                "</div>" +
                "</td>" +
                "</tr>" +
                "<tr>" +
                "<td style='background: #f3f4f6; padding: 40px; text-align: center; border-top: 1px solid #e5e7eb;'>" +
                "<p style='color: #6b7280; font-size: 14px; line-height: 1.6; margin: 0 0 20px 0;'>" +
                "Este es un mensaje automático generado por el sistema SGH.<br>" +
                "<strong style='color: #374151;'>Por seguridad, no respondas a este correo electrónico.</strong><br>" +
                "Si necesitas ayuda, contacta al equipo de soporte técnico." +
                "</p>" +
                "<div style='margin-top: 25px; padding-top: 25px; border-top: 1px solid #e5e7eb;'>" +
                "<div style='margin-bottom: 16px;'>" +
                "<div style='width: 64px; height: 64px; background: #6b7280; border-radius: 16px; margin: 0 auto; box-shadow: 0 6px 16px rgba(107, 114, 128, 0.4); display: table;'>" +
                "<div style='display: table-cell; vertical-align: middle; text-align: center; padding: 12px;'>" +
                "<span style='font-size: 24px; font-weight: 700; color: #ffffff; font-family: \"Segoe UI\", -apple-system, BlinkMacSystemFont, Arial, sans-serif; letter-spacing: 3px; line-height: 1; text-transform: uppercase;'>SGH</span>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "<p style='color: #1f2937; font-weight: 700; font-size: 16px; margin: 0 0 6px 0;'>" +
                "Sistema de Gestión de Horarios" +
                "</p>" +
                "<p style='color: #6b7280; font-size: 13px; margin: 0;'>" +
                "Tu sistema de confianza para la gestión académica" +
                "</p>" +
                "</div>" +
                "</td>" +
                "</tr>" +
                "</table>" +
                "<div style='text-align: center; padding-top: 25px;'>" +
                "<p style='color: #94a3b8; font-size: 13px; margin: 0;'>" +
                "© 2025 Sistema de Gestión de Horarios. Todos los derechos reservados." +
                "</p>" +
                "</div>" +
                "</body>" +
                "</html>";

            helper.setText(htmlContent, true);

            mailSender.send(message);

            System.out.println("=== EMAIL DE RESET ENVIADO ===");
            System.out.println("Destinatario: " + email);
            System.out.println("Código: " + verificationCode);
            System.out.println("============================");

        } catch (Exception e) {
            System.err.println("Error enviando email de reset: " + e.getMessage());
            System.out.println("=== CÓDIGO DE RESET SGH (FALLBACK) ===");
            System.out.println("Email: " + email);
            System.out.println("Código: " + verificationCode);
            System.out.println("Este código expira en 10 minutos");
            System.out.println("=====================================");
        }
    }

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
}