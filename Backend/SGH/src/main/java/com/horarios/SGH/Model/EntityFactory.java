package com.horarios.SGH.Model;

import java.time.LocalDateTime;

/**
 * Factory para creación de entidades del sistema SGH.
 * Implementa patrón Factory Method para crear instancias de entidades
 * de manera centralizada y consistente.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de crear entidades
 * - DIP: No depende de implementaciones concretas
 *
 * @author Sistema SGH
 * @version 1.0
 */
public class EntityFactory {

    /**
     * Crea un curso básico.
     *
     * @param courseName nombre del curso
     * @param academicYear año académico
     * @return courses configurado
     */
    public static courses createCourse(String courseName, String academicYear) {
        courses course = new courses();
        course.setCourseName(courseName);
        course.setAcademicYear(academicYear);
        course.setCreatedAt(LocalDateTime.now());
        return course;
    }

    /**
     * Crea un token revocado básico.
     *
     * @param token token JWT a revocar
     * @param userId ID del usuario propietario
     * @param isRefreshToken si es un refresh token
     * @return RevokedToken configurado
     */
    public static RevokedToken createRevokedToken(String token, Long userId, boolean isRefreshToken) {
        RevokedToken revokedToken = new RevokedToken();
        revokedToken.setToken(token);
        revokedToken.setUserId(userId);
        revokedToken.setRefreshToken(isRefreshToken);
        revokedToken.setCreatedAt(LocalDateTime.now());
        return revokedToken;
    }



    /**
     * Crea una materia básica.
     *
     * @param subjectName nombre de la materia
     * @return subjects configurado
     */
    public static subjects createSubject(String subjectName) {
        subjects subject = new subjects();
        subject.setSubjectName(subjectName);
        return subject;
    }

    /**
     * Crea un docente básico.
     *
     * @param teacherName nombre del docente
     * @return teachers configurado
     */
    public static teachers createTeacher(String teacherName) {
        teachers teacher = new teachers();
        teacher.setTeacherName(teacherName);
        return teacher;
    }

    /**
     * Crea un horario básico.
     *
     * @param course curso asociado
     * @param teacher profesor asignado
     * @param subject materia impartida
     * @param day día de la semana
     * @param startTime hora de inicio
     * @param endTime hora de fin
     * @param scheduleName nombre del horario
     * @return schedule configurado
     */
    public static schedule createSchedule(courses course, teachers teacher, subjects subject,
                                        Days day, java.time.LocalTime startTime, java.time.LocalTime endTime, String scheduleName) {
        schedule s = new schedule();
        s.setCourseId(course);
        s.setTeacherId(teacher);
        s.setSubjectId(subject);
        s.setDay(day);
        s.setStartTime(startTime);
        s.setEndTime(endTime);
        s.setScheduleName(scheduleName);
        return s;
    }

    /**
     * Crea una persona básica.
     *
     * @param fullName nombre completo
     * @param email email personal
     * @return People configurado
     */
    public static People createPerson(String fullName, String email) {
        People person = new People(fullName, email);
        return person;
    }

    /**
     * Crea un usuario básico.
     *
     * @param username nombre de usuario
     * @param email email
     * @param firstName nombre
     * @param lastName apellido
     * @param person persona asociada
     * @return User configurado
     */
    public static User createUser(String username, String email, String firstName, String lastName, People person) {
        User user = new User(username, email, firstName, lastName, person);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    /**
     * Crea un rol básico.
     *
     * @param roleName nombre del rol
     * @param description descripción
     * @return Role configurado
     */
    public static Role createRole(String roleName, String description) {
        Role role = new Role(roleName, description);
        role.setCreatedAt(LocalDateTime.now());
        return role;
    }

    /**
     * Crea un permiso básico.
     *
     * @param permissionName nombre del permiso
     * @param description descripción
     * @return Permission configurado
     */
    public static Permission createPermission(String permissionName, String description) {
        Permission permission = new Permission(permissionName, description);
        permission.setCreatedAt(LocalDateTime.now());
        return permission;
    }

    /**
     * Crea una notificación In-App básica.
     *
     * @param userId ID del usuario
     * @param notificationType tipo de notificación
     * @param title título
     * @param message mensaje
     * @return InAppNotification configurado
     */
    public static InAppNotification createInAppNotification(Long userId, NotificationType notificationType, String title, String message) {
        InAppNotification notification = new InAppNotification(userId, notificationType, title, message);
        notification.setCreatedAt(LocalDateTime.now());
        return notification;
    }

    /**
     * Crea una disponibilidad de profesor básica.
     *
     * @param teacher profesor asociado
     * @param day día de la semana
     * @param amStart hora de inicio mañana
     * @param amEnd hora de fin mañana
     * @param pmStart hora de inicio tarde
     * @param pmEnd hora de fin tarde
     * @return TeacherAvailability configurado
     */
    public static TeacherAvailability createTeacherAvailability(teachers teacher, Days day,
                                                               java.time.LocalTime amStart, java.time.LocalTime amEnd,
                                                               java.time.LocalTime pmStart, java.time.LocalTime pmEnd) {
        TeacherAvailability availability = new TeacherAvailability(teacher, day, amStart, amEnd, pmStart, pmEnd);
        return availability;
    }

    /**
     * Crea una relación docente-materia básica.
     *
     * @param teacher docente
     * @param subject materia
     * @return TeacherSubject configurado
     */
    public static TeacherSubject createTeacherSubject(teachers teacher, subjects subject) {
        TeacherSubject teacherSubject = new TeacherSubject(teacher, subject);
        return teacherSubject;
    }

    /**
     * Crea credenciales de usuario básicas.
     *
     * @param user usuario propietario
     * @param passwordHash hash de la contraseña
     * @return UserCredentials configurado
     */
    public static UserCredentials createUserCredentials(User user, String passwordHash) {
        UserCredentials credentials = new UserCredentials(user, passwordHash);
        return credentials;
    }

    /**
     * Crea una relación usuario-rol básica.
     *
     * @param user usuario
     * @param role rol
     * @return UserRole configurado
     */
    public static UserRole createUserRole(User user, Role role) {
        UserRole userRole = new UserRole(user, role);
        return userRole;
    }

    /**
     * Crea configuración de seguridad de usuario básica.
     *
     * @param user usuario
     * @return UserSecurity configurado
     */
    public static UserSecurity createUserSecurity(User user) {
        UserSecurity security = new UserSecurity(user);
        return security;
    }

    /**
     * Crea una entidad vacía genérica.
     * Método de conveniencia para inicialización.
     *
     * @param entityClass clase de la entidad
     * @return entidad vacía
     */
    @SuppressWarnings("unchecked")
    public static <T extends AbstractEntity> T createEmpty(Class<T> entityClass) {
        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            entity.setCreatedAt(LocalDateTime.now());
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Error creando entidad vacía", e);
        }
    }
}