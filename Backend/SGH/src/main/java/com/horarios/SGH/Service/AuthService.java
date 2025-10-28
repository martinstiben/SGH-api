package com.horarios.SGH.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
import com.horarios.SGH.Model.teachers;
import com.horarios.SGH.Model.subjects;
import com.horarios.SGH.Model.TeacherSubject;
import com.horarios.SGH.Repository.Iusers;
import com.horarios.SGH.Repository.IPeopleRepository;
import com.horarios.SGH.Repository.IRolesRepository;
import com.horarios.SGH.Repository.Iteachers;
import com.horarios.SGH.Repository.Isubjects;
import com.horarios.SGH.Repository.TeacherSubjectRepository;
import com.horarios.SGH.DTO.LoginRequestDTO;
import com.horarios.SGH.DTO.LoginResponseDTO;
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

    @Autowired
    private JavaMailSender mailSender;

    public AuthService(Iusers repo,
                          IPeopleRepository peopleRepo,
                          IRolesRepository rolesRepo,
                          Iteachers teacherRepo,
                          Isubjects subjectRepo,
                          TeacherSubjectRepository teacherSubjectRepo,
                          PasswordEncoder encoder,
                          AuthenticationManager authManager,
                          JwtTokenProvider jwtTokenProvider) {
        this.repo = repo;
        this.peopleRepo = peopleRepo;
        this.rolesRepo = rolesRepo;
        this.teacherRepo = teacherRepo;
        this.subjectRepo = subjectRepo;
        this.teacherSubjectRepo = teacherSubjectRepo;
        this.encoder = encoder;
        this.authManager = authManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public String register(String name, String email, String rawPassword, Role role) {
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

        // Crear y guardar el nuevo usuario
        users newUser = new users(person, userRole, encoder.encode(rawPassword));
        repo.save(newUser);

        return "Usuario registrado correctamente";
    }


    /**
     * Inicia el proceso de login verificando credenciales y enviando código 2FA.
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
     * Envía el código de verificación por email usando JavaMailSender.
     *
     * @param email Dirección de email del destinatario
     * @param code Código de verificación
     */
    private void sendVerificationEmail(String email, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Código de Verificación - SGH");
            message.setText("Hola,\n\n" +
                          "Tu código de verificación para SGH es: " + code + "\n\n" +
                          "Este código expira en 10 minutos.\n\n" +
                          "Si no solicitaste este código, ignora este mensaje.\n\n" +
                          "Saludos,\n" +
                          "Equipo SGH");

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
}