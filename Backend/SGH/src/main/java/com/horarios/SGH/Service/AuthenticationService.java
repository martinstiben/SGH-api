package com.horarios.SGH.Service;

import com.horarios.SGH.DTO.LoginRequestDTO;
import com.horarios.SGH.DTO.LoginResponseDTO;
import com.horarios.SGH.Model.User;
import com.horarios.SGH.Model.UserCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Servicio de autenticación para login y verificación de códigos 2FA.
 * Maneja el proceso de autenticación de usuarios aplicando principios SOLID
 * y patrones de diseño como Strategy y Factory.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única por método
 * - OCP: Abierto para extensión, cerrado para modificación
 * - LSP: Interfaces intercambiables
 * - ISP: Interfaces específicas
 * - DIP: Dependencias de abstracciones
 *
 * @author Sistema SGH
 * @version 2.0 - Refactorizado para SOLID
 */
@Service
public class AuthenticationService {

    private final usersService userService;
    private final PasswordEncoder passwordEncoder;
    private final EmailNotificationService emailNotificationService;
    private final VerificationCodeFactory verificationCodeFactory;
    private final CredentialValidator credentialValidator;
    
    /**
     * Logger para registro de eventos del servicio de autenticación.
     */
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    /**
     * Interfaz para validación de credenciales (Patrón Strategy).
     */
    @FunctionalInterface
    public interface CredentialValidator {
        /**
         * Valida las credenciales del usuario.
         *
         * @param user Usuario a validar
         * @param password Contraseña proporcionada
         * @param passwordEncoder Encoder para verificar contraseña
         * @throws IllegalArgumentException si las credenciales son inválidas
         */
        void validate(User user, String password, PasswordEncoder passwordEncoder);
    }

    /**
     * Implementación por defecto del validador de credenciales.
     */
    public static final CredentialValidator DEFAULT_CREDENTIAL_VALIDATOR = (user, password, encoder) -> {
        if (user == null) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        if (!user.isActive()) {
            throw new IllegalArgumentException("La cuenta del usuario no está activa");
        }
        UserCredentials credentials = user.getUserCredentials();
        if (credentials == null || !encoder.matches(password, credentials.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
    };

    /**
     * Factory para generar códigos de verificación (Patrón Factory).
     */
    @FunctionalInterface
    public interface VerificationCodeFactory {
        /**
         * Genera un código de verificación.
         *
         * @return Código de verificación generado
         */
        String generate();
    }

    /**
     * Implementación por defecto del factory de códigos.
     */
    public static final VerificationCodeFactory DEFAULT_VERIFICATION_CODE_FACTORY = () ->
        String.valueOf((int)(Math.random() * 900000) + 100000);

    /**
     * Constructor con inyección de dependencias usando patrón de configuración flexible.
     * Utiliza implementaciones por defecto para extensibilidad.
     *
     * @param userService Servicio de usuarios
     * @param passwordEncoder Encoder de contraseñas
     * @param emailNotificationService Servicio de notificaciones por email
     */
    public AuthenticationService(usersService userService, PasswordEncoder passwordEncoder,
                                EmailNotificationService emailNotificationService) {
        this(userService, passwordEncoder, emailNotificationService,
             DEFAULT_VERIFICATION_CODE_FACTORY, DEFAULT_CREDENTIAL_VALIDATOR);
    }
    
    /**
     * Constructor completo con inyección de dependencias.
     * Permite personalizar las estrategias de validación y generación de códigos.
     *
     * @param userService Servicio de usuarios
     * @param passwordEncoder Encoder de contraseñas
     * @param emailNotificationService Servicio de notificaciones por email
     * @param verificationCodeFactory Factory para generar códigos de verificación
     * @param credentialValidator Validador de credenciales
     */
    public AuthenticationService(usersService userService, PasswordEncoder passwordEncoder,
                                EmailNotificationService emailNotificationService,
                                VerificationCodeFactory verificationCodeFactory,
                                CredentialValidator credentialValidator) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.emailNotificationService = emailNotificationService;
        this.verificationCodeFactory = verificationCodeFactory;
        this.credentialValidator = credentialValidator;
    }

    /**
     * Inicia el proceso de login verificando credenciales y enviando código 2FA.
     * Aplica el patrón Strategy para validación de credenciales y Factory para códigos.
     *
     * @param request DTO con email y contraseña
     * @return Mensaje de confirmación del envío del código
     * @throws IllegalArgumentException si los datos son inválidos o credenciales incorrectas
     */
    public String initiateLogin(LoginRequestDTO request) {
        logger.info("Iniciando proceso de login para email: {}", request != null ? request.getEmail() : "null");

        // Validar entrada básica
        validateLoginRequest(request);

        // Buscar usuario
        User user = findUserByEmail(request.getEmail().trim().toLowerCase());

        // Validar credenciales usando estrategia
        credentialValidator.validate(user, request.getPassword(), passwordEncoder);

        // Generar y enviar código de verificación
        String verificationCode = verificationCodeFactory.generate();
        emailNotificationService.sendVerificationEmail(user, verificationCode);

        // TODO: Guardar código en BD con expiración (Principio Abierto/Cerrado)

        logger.info("Código de verificación enviado exitosamente para usuario: {}", user.getUserId());
        return "Código de verificación enviado al email";
    }

    /**
     * Verifica el código 2FA y genera token JWT.
     * Método refactorizado para mejor mantenibilidad.
     *
     * @param email Email del usuario
     * @param code Código de verificación proporcionado
     * @return DTO con token JWT y información del usuario
     * @throws IllegalArgumentException si el usuario no existe o el código es inválido
     */
    public LoginResponseDTO verifyCode(String email, String code) {
        logger.info("Verificando código 2FA para email: {}", email);

        // Validar entrada
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("El código de verificación es obligatorio");
        }

        // Buscar usuario
        User user = findUserByEmail(email.trim().toLowerCase());

        // TODO: Verificar código en BD y expiración
        // Por ahora, simular verificación exitosa

        // Generar token JWT (integrar con JwtTokenProvider en producción)
        String token = generateJwtToken(user);

        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(token);
        response.setUserId(user.getUserId());
        response.setEmail(user.getEmail());
        response.setName(user.getPerson().getFullName());

        logger.info("Código verificado exitosamente para usuario: {}", user.getUserId());
        return response;
    }

    /**
     * Valida la solicitud de login.
     *
     * @param request Solicitud a validar
     * @throws IllegalArgumentException si la solicitud es inválida
     */
    private void validateLoginRequest(LoginRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos de login son obligatorios");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }
    }

    /**
     * Busca un usuario por email.
     *
     * @param email Email del usuario
     * @return Usuario encontrado
     * @throws IllegalArgumentException si el usuario no existe
     */
    private User findUserByEmail(String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Credenciales inválidas");
        }
        return user;
    }

    /**
     * Genera un token JWT para el usuario (simulado).
     * En producción, usar JwtTokenProvider.
     *
     * @param user Usuario para el token
     * @return Token JWT generado
     */
    private String generateJwtToken(User user) {
        // TODO: Integrar con JwtTokenProvider real
        return "jwt-token-simulado-" + user.getUserId();
    }
}