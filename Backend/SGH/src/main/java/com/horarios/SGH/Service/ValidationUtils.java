package com.horarios.SGH.Service;

import java.util.regex.Pattern;

/**
 * Utilidades para validación de datos en el sistema SGH.
 * Contiene métodos estáticos para validar diferentes tipos de datos.
 */
public final class ValidationUtils {

    // Patrones de validación como constantes para mejor mantenibilidad
    private static final Pattern COURSE_NAME_PATTERN = Pattern.compile("^[a-zA-ZÀ-ÿ0-9\\s]+$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$");

    // Constantes de longitud
    private static final int COURSE_NAME_MIN_LENGTH = 1;
    private static final int COURSE_NAME_MAX_LENGTH = 2;
    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final int PASSWORD_MAX_LENGTH = 100;
    private static final int NAME_MAX_LENGTH = 100;

    private ValidationUtils() {
        // Constructor privado para prevenir instanciación
        throw new UnsupportedOperationException("Esta clase no puede ser instanciada");
    }

    /**
     * Valida el nombre de un curso.
     *
     * @param courseName El nombre del curso a validar
     * @throws IllegalArgumentException si el nombre no cumple con las reglas de validación
     */
    public static void validateCourseName(String courseName) {
        if (courseName == null || courseName.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del curso no puede estar vacío o contener solo espacios");
        }

        String trimmedName = courseName.trim();

        if (trimmedName.length() < COURSE_NAME_MIN_LENGTH) {
            throw new IllegalArgumentException("El nombre del curso debe tener al menos " + COURSE_NAME_MIN_LENGTH + " caracter");
        }

        if (trimmedName.length() > COURSE_NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("El nombre del curso solo puede tener " + COURSE_NAME_MAX_LENGTH + " caracteres, ejemplo: 1A");
        }

        if (!COURSE_NAME_PATTERN.matcher(trimmedName).matches()) {
            throw new IllegalArgumentException("El nombre del curso solo puede contener letras, números y espacios");
        }
    }

    /**
     * Valida un email.
     *
     * @param email El email a validar
     * @throws IllegalArgumentException si el email no es válido
     */
    public static void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El correo electrónico no puede estar vacío");
        }

        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("El correo electrónico debe tener un formato válido");
        }
    }

    /**
     * Valida una contraseña.
     *
     * @param password La contraseña a validar
     * @throws IllegalArgumentException si la contraseña no cumple con las reglas
     */
    public static void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }

        String trimmedPassword = password.trim();

        if (trimmedPassword.length() < PASSWORD_MIN_LENGTH) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + PASSWORD_MIN_LENGTH + " caracteres");
        }

        if (trimmedPassword.length() > PASSWORD_MAX_LENGTH) {
            throw new IllegalArgumentException("La contraseña no puede exceder los " + PASSWORD_MAX_LENGTH + " caracteres");
        }

        if (!PASSWORD_PATTERN.matcher(trimmedPassword).matches()) {
            throw new IllegalArgumentException("La contraseña debe contener al menos una letra minúscula, una mayúscula y un número");
        }
    }

    /**
     * Valida un nombre de usuario.
     *
     * @param name El nombre a validar
     * @throws IllegalArgumentException si el nombre no es válido
     */
    public static void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }

        if (name.trim().length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("El nombre no puede exceder los " + NAME_MAX_LENGTH + " caracteres");
        }
    }
}