package com.horarios.SGH.Service;

import com.horarios.SGH.Model.*;
import com.horarios.SGH.Repository.IPeopleRepository;
import com.horarios.SGH.Repository.IUserRepository;
import com.horarios.SGH.Repository.Icourses;
import com.horarios.SGH.Repository.Isubjects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio encargado del registro de usuarios en el sistema SGH.
 * Maneja la creación de cuentas de usuario, validación de datos y asignación de roles.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Service
public class UserRegistrationService {

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IPeopleRepository peopleRepository;

    @Autowired
    private Icourses courseRepository;

    @Autowired
    private Isubjects subjectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailNotificationService emailNotificationService;

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * @param name Nombre completo del usuario
     * @param email Correo electrónico único
     * @param rawPassword Contraseña en texto plano
     * @param role Rol asignado al usuario
     * @param subjectId ID de la materia (opcional para docentes)
     * @param courseId ID del curso (opcional para estudiantes)
     * @return Mensaje de confirmación del registro
     * @throws IllegalArgumentException si los datos son inválidos
     */
    @Transactional
    public String registerUser(String name, String email, String rawPassword, String role,
                              Integer subjectId, Integer courseId) {
        // Validar datos básicos
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email es obligatorio");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }

        // Verificar si el email ya existe
        if (peopleRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        // Crear persona
        People person = new People(name, email);
        person = peopleRepository.save(person);

        // Crear usuario
        User user = new User(person);
        user.setUsername(email); // Usar email como username inicialmente
        user.setEmail(email);

        // Asignar rol según el tipo
        assignRoleToUser(user, role, subjectId, courseId);

        // Guardar usuario
        user = userRepository.save(user);

        // Enviar notificación de bienvenida
        emailNotificationService.sendWelcomeEmail(user);

        return "Usuario registrado exitosamente con ID: " + user.getUserId();
    }

    /**
     * Asigna el rol apropiado al usuario basado en el tipo especificado.
     *
     * @param user Usuario al que asignar el rol
     * @param role Tipo de rol
     * @param subjectId ID de materia (para docentes)
     * @param courseId ID de curso (para estudiantes)
     */
    private void assignRoleToUser(User user, String role, Integer subjectId, Integer courseId) {
        switch (role.toUpperCase()) {
            case "ESTUDIANTE":
                if (courseId == null) {
                    throw new IllegalArgumentException("Se requiere un curso para estudiantes");
                }
                // Lógica para asignar rol estudiante
                break;
            case "DOCENTE":
                if (subjectId == null) {
                    throw new IllegalArgumentException("Se requiere una materia para docentes");
                }
                // Lógica para asignar rol docente
                break;
            case "DIRECTOR":
                // Lógica para director
                break;
            case "COORDINADOR":
                // Lógica para coordinador
                break;
            default:
                throw new IllegalArgumentException("Rol no válido: " + role);
        }
    }
}