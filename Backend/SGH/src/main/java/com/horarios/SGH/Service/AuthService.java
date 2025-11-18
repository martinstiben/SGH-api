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
import com.horarios.SGH.Model.AccountStatus;
import com.horarios.SGH.Model.NotificationType;
import com.horarios.SGH.Model.NotificationPriority;
import com.horarios.SGH.Repository.Iusers;
import com.horarios.SGH.Repository.IPeopleRepository;
import com.horarios.SGH.Repository.IRolesRepository;
import com.horarios.SGH.Repository.Iteachers;
import com.horarios.SGH.Repository.Isubjects;
import com.horarios.SGH.Repository.TeacherSubjectRepository;
import com.horarios.SGH.Service.usersService;
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
        this.teacherRepo = teacherRepo;
        this.subjectRepo = subjectRepo;
        this.teacherSubjectRepo = teacherSubjectRepo;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.inAppNotificationService = inAppNotificationService;
    }

    public String register(String name, String email, String rawPassword, Role role) {
        try {
            // Validar entradas usando ValidationUtils
            ValidationUtils.validateName(name);
            ValidationUtils.validateEmail(email);
            ValidationUtils.validatePassword(rawPassword);

            if (role == null) {
                throw new IllegalArgumentException("El rol no puede ser nulo");
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
                "<style>" +
                "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #0066cc 0%, #004d99 100%); margin: 0; padding: 20px; }" +
                ".container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 16px; box-shadow: 0 20px 40px rgba(0,102,204,0.15); overflow: hidden; border: 2px solid #e6f3ff; }" +
                ".header { background: linear-gradient(135deg, #0066cc 0%, #004d99 100%); color: #ffffff; padding: 40px 30px; text-align: center; position: relative; }" +
                ".header::before { content: ''; position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: url('data:image/svg+xml,<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\"><circle cx=\"50\" cy=\"50\" r=\"2\" fill=\"rgba(255,255,255,0.1)\"/></svg>') repeat; opacity: 0.1; }" +
                ".logo { width: 80px; height: 80px; background: #ffffff; border-radius: 50%; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 12px rgba(0,0,0,0.2); position: relative; }" +
                ".logo-text { font-size: 28px; font-weight: bold; color: #0066cc; line-height: 1; margin: 0; padding: 0; position: relative; top: 2px; }" +
                ".header h1 { margin: 0; font-size: 28px; font-weight: 700; text-shadow: 0 2px 4px rgba(0,0,0,0.3); }" +
                ".header p { margin: 10px 0 0 0; font-size: 16px; opacity: 0.9; }" +
                ".content { padding: 40px 30px; text-align: center; background: #fafbfc; }" +
                ".welcome { font-size: 20px; color: #0066cc; margin-bottom: 10px; font-weight: 600; }" +
                ".message { font-size: 16px; color: #4a5568; line-height: 1.6; margin-bottom: 30px; }" +
                ".code-container { background: linear-gradient(135deg, #0066cc 0%, #004d99 100%); border-radius: 12px; padding: 30px; margin: 30px 0; box-shadow: 0 8px 25px rgba(0,102,204,0.3); }" +
                ".code-label { color: #ffffff; font-size: 14px; margin-bottom: 10px; opacity: 0.9; }" +
                ".code { font-size: 36px; font-weight: 900; color: #ffffff; letter-spacing: 8px; font-family: 'Courier New', monospace; text-shadow: 0 2px 4px rgba(0,0,0,0.3); }" +
                ".timer { background: #e6f3ff; border: 2px solid #0066cc; border-radius: 20px; padding: 8px 16px; display: inline-block; margin: 20px 0; }" +
                ".timer-text { color: #0066cc; font-weight: 600; font-size: 14px; }" +
                ".warning { background: linear-gradient(135deg, #ffe6e6 0%, #ffcccc 100%); border: 2px solid #cc0000; border-radius: 8px; padding: 15px; margin: 20px 0; }" +
                ".warning-text { color: #990000; font-size: 14px; margin: 0; font-weight: 500; }" +
                ".footer { background: linear-gradient(135deg, #f0f8ff 0%, #e6f3ff 100%); padding: 30px; text-align: center; border-top: 3px solid #0066cc; }" +
                ".footer-text { color: #004d99; font-size: 14px; line-height: 1.5; }" +
                ".footer-brand { color: #0066cc; font-weight: 700; margin-top: 10px; font-size: 16px; }" +
                "@media (max-width: 600px) { .container { margin: 10px; } .header { padding: 30px 20px; } .content { padding: 30px 20px; } .code { font-size: 28px; letter-spacing: 4px; } .logo { width: 60px; height: 60px; font-size: 28px; } }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<div class='logo'>" +
                "<span class='logo-text'>SGH</span>" +
                "</div>" +
                "<h1>Verificación de Seguridad</h1>" +
                "<p>Sistema de Gestión de Horarios</p>" +
                "</div>" +
                "<div class='content'>" +
                "<div class='welcome'>¡Hola!</div>" +
                "<div class='message'>Para proteger tu cuenta, hemos enviado este código de verificación de 6 dígitos. Ingresa este código en la aplicación para completar tu inicio de sesión.</div>" +
                "<div class='code-container'>" +
                "<div class='code-label'>Tu código de verificación:</div>" +
                "<div class='code'>" + code + "</div>" +
                "</div>" +
                "<div class='timer'>" +
                "<span class='timer-text'>Expira en 10 minutos</span>" +
                "</div>" +
                "<div class='warning'>" +
                "<p class='warning-text'>Importante: Si no solicitaste este código, alguien podría estar intentando acceder a tu cuenta. Por favor, ignora este mensaje y contacta inmediatamente con el administrador del sistema.</p>" +
                "</div>" +
                "</div>" +
                "<div class='footer'>" +
                "<div class='footer-text'>" +
                "Este es un mensaje automático generado por el sistema SGH.<br>" +
                "Por seguridad, no respondas a este correo electrónico.<br>" +
                "Si necesitas ayuda, contacta al equipo de soporte técnico." +
                "</div>" +
                "<div class='footer-brand'>SGH - Tu Sistema de Confianza</div>" +
                "</div>" +
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
            // Fallback: mostrar en consola si falla el email
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