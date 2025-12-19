package com.horarios.SGH.Service;

import java.util.regex.Pattern;

/**
 * Utilidades para validación de datos en el sistema SGH.
 * Implementa el patrón Strategy para diferentes estrategias de validación.
 * Contiene métodos estáticos para validar diferentes tipos de datos aplicando
 * principios SOLID y patrones de diseño.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de validación de datos
 * - OCP: Abierto para extensión mediante nuevas estrategias de validación
 * - DIP: Depende de abstracciones (patrones regex)
 *
 * Patrones de diseño utilizados:
 * - Strategy: Para diferentes estrategias de validación
 * - Factory: Para creación de validadores
 *
 * @author Sistema SGH
 * @version 2.0 - Refactorizado con patrones de diseño
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

    /**
     * Interfaz Strategy para estrategias de validación.
     */
    @FunctionalInterface
    public interface ValidationStrategy {
        /**
         * Ejecuta la validación específica.
         *
         * @param value El valor a validar
         * @throws IllegalArgumentException si la validación falla
         */
        void validate(String value);
    }

    /**
     * Factory para crear estrategias de validación.
     */
    public static class ValidationStrategyFactory {
        /**
         * Crea una estrategia de validación para nombres de curso.
         *
         * @return Estrategia de validación para cursos
         */
        public static ValidationStrategy createCourseNameValidator() {
            return ValidationUtils::validateCourseName;
        }

        /**
         * Crea una estrategia de validación para emails.
         *
         * @return Estrategia de validación para emails
         */
        public static ValidationStrategy createEmailValidator() {
            return ValidationUtils::validateEmail;
        }

        /**
         * Crea una estrategia de validación para contraseñas.
         *
         * @return Estrategia de validación para contraseñas
         */
        public static ValidationStrategy createPasswordValidator() {
            return ValidationUtils::validatePassword;
        }

        /**
         * Crea una estrategia de validación para nombres.
         *
         * @return Estrategia de validación para nombres
         */
        public static ValidationStrategy createNameValidator() {
            return ValidationUtils::validateName;
        }
    }

    private ValidationUtils() {
        // Constructor privado para prevenir instanciación
        throw new UnsupportedOperationException("Esta clase no puede ser instanciada");
    }

    /**
     * Valida el nombre de un curso aplicando el patrón Strategy.
     * Verifica formato, longitud y caracteres permitidos.
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
     * Valida un email aplicando el patrón Strategy.
     * Verifica formato básico de email usando expresiones regulares.
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
     * Valida una contraseña aplicando el patrón Strategy.
     * Verifica longitud mínima, máxima y complejidad (mayúscula, minúscula, número).
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
     * Valida un nombre de usuario aplicando el patrón Strategy.
     * Verifica que no esté vacío y no exceda la longitud máxima.
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