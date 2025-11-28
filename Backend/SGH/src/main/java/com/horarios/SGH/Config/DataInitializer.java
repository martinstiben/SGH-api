package com.horarios.SGH.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.horarios.SGH.Model.users;
import com.horarios.SGH.Model.subjects;
import com.horarios.SGH.Model.teachers;
import com.horarios.SGH.Model.courses;
import com.horarios.SGH.Model.TeacherSubject;
import com.horarios.SGH.Model.TeacherAvailability;
import com.horarios.SGH.Model.Days;
import com.horarios.SGH.Repository.Iusers;
import com.horarios.SGH.Repository.Isubjects;
import com.horarios.SGH.Repository.Iteachers;
import com.horarios.SGH.Repository.Icourses;
import com.horarios.SGH.Repository.TeacherSubjectRepository;
import com.horarios.SGH.Repository.ITeacherAvailabilityRepository;
import com.horarios.SGH.Repository.IPeopleRepository;
import com.horarios.SGH.Repository.IRolesRepository;
import com.horarios.SGH.Model.People;
import com.horarios.SGH.Model.Roles;

import org.springframework.boot.CommandLineRunner;
import java.time.LocalTime;

@Configuration
public class DataInitializer {

    @Value("${app.master.name:master}")
    private String masterUsername;

    @Value("${app.master.password:Master$2025!}")
    private String masterPassword;

    @Bean
    public CommandLineRunner seedRolesAndMasterUser(Iusers repo, PasswordEncoder encoder, IPeopleRepository peopleRepo, IRolesRepository rolesRepo) {
        return args -> {
            // Primero crear los roles si no existen
            if (rolesRepo.count() == 0) {
                rolesRepo.save(new Roles("MAESTRO"));
                rolesRepo.save(new Roles("ESTUDIANTE"));
                rolesRepo.save(new Roles("COORDINADOR"));
                rolesRepo.save(new Roles("DIRECTOR_DE_AREA"));
                System.out.println(">> Roles iniciales creados");
            } else {
                System.out.println(">> Roles ya existen");
            }
            
            // Ahora crear el usuario master
            if (!repo.existsByUserName(masterUsername)) {
                // Verificar si ya existe una persona con este email
                if (peopleRepo.findByEmail(masterUsername).isPresent()) {
                    System.out.println(">> Persona con email master ya existe, saltando creación");
                    return;
                }

                // Crear persona para el usuario master
                People masterPerson = new People("Master User", masterUsername);
                masterPerson = peopleRepo.save(masterPerson);

                // Obtener rol MAESTRO - ahora debería existir
                Roles maestroRole = rolesRepo.findByRoleName("MAESTRO")
                    .orElseGet(() -> {
                        System.out.println(">> Rol MAESTRO no encontrado, creando...");
                        return rolesRepo.save(new Roles("MAESTRO"));
                    });

                users u = new users(masterPerson, maestroRole, encoder.encode(masterPassword));
                repo.save(u);
                System.out.println(">> Master creado: " + masterUsername);
            } else {
                System.out.println(">> Master ya existe: " + masterUsername);
            }

            long total = repo.count();
            System.out.println(">> Usuarios totales: " + total + " (sin límite)");
        };
    }

    @Bean
    public CommandLineRunner seedInitialData(Isubjects subjectRepo, Iteachers teacherRepo, Icourses courseRepo, TeacherSubjectRepository teacherSubjectRepo, ITeacherAvailabilityRepository availabilityRepo, Iusers userRepo, PasswordEncoder encoder, IPeopleRepository peopleRepo, IRolesRepository rolesRepo) {
        return args -> {
            // Declarar variables de materias para usar en todo el método
            subjects math = null, physics = null, chemistry = null, biology = null, ethics = null, history = null, literature = null, english = null;

            // Crear materias si no existen
            if (subjectRepo.count() == 0) {
                math = new subjects();
                math.setSubjectName("Matemáticas");
                subjectRepo.save(math);

                physics = new subjects();
                physics.setSubjectName("Física");
                subjectRepo.save(physics);

                chemistry = new subjects();
                chemistry.setSubjectName("Química");
                subjectRepo.save(chemistry);

                biology = new subjects();
                biology.setSubjectName("Biología");
                subjectRepo.save(biology);

                ethics = new subjects();
                ethics.setSubjectName("Ética");
                subjectRepo.save(ethics);

                history = new subjects();
                history.setSubjectName("Historia");
                subjectRepo.save(history);

                literature = new subjects();
                literature.setSubjectName("Literatura");
                subjectRepo.save(literature);

                english = new subjects();
                english.setSubjectName("Inglés");
                subjectRepo.save(english);

                System.out.println(">> Materias iniciales creadas");
            } else {
                // Si ya existen, cargarlas
                math = subjectRepo.findBySubjectName("Matemáticas");
                physics = subjectRepo.findBySubjectName("Física");
                chemistry = subjectRepo.findBySubjectName("Química");
                biology = subjectRepo.findBySubjectName("Biología");
                ethics = subjectRepo.findBySubjectName("Ética");
                history = subjectRepo.findBySubjectName("Historia");
                literature = subjectRepo.findBySubjectName("Literatura");
                english = subjectRepo.findBySubjectName("Inglés");
            }

            // Crear profesores adicionales si no existen específicamente
            int teachersCreated = 0;
            int availabilitiesCreated = 0;

            // Buscar profesores existentes o crearlos si no existen
            teachers teacher1 = teacherRepo.findAll().stream().filter(t -> t.getTeacherName().equals("Juan Pérez")).findFirst().orElse(null);
            if (teacher1 == null) {
                teacher1 = new teachers(); teacher1.setTeacherName("Juan Pérez"); teacher1 = teacherRepo.save(teacher1); teachersCreated++;
            }

            teachers teacher2 = teacherRepo.findAll().stream().filter(t -> t.getTeacherName().equals("María García")).findFirst().orElse(null);
            if (teacher2 == null) {
                teacher2 = new teachers(); teacher2.setTeacherName("María García"); teacher2 = teacherRepo.save(teacher2); teachersCreated++;
            }

            teachers teacher3 = teacherRepo.findAll().stream().filter(t -> t.getTeacherName().equals("Carlos López")).findFirst().orElse(null);
            if (teacher3 == null) {
                teacher3 = new teachers(); teacher3.setTeacherName("Carlos López"); teacher3 = teacherRepo.save(teacher3); teachersCreated++;
            }

            teachers teacher4 = teacherRepo.findAll().stream().filter(t -> t.getTeacherName().equals("Ana Rodríguez")).findFirst().orElse(null);
            if (teacher4 == null) {
                teacher4 = new teachers(); teacher4.setTeacherName("Ana Rodríguez"); teacher4 = teacherRepo.save(teacher4); teachersCreated++;
            }

            teachers teacher5 = teacherRepo.findAll().stream().filter(t -> t.getTeacherName().equals("Pedro Martínez")).findFirst().orElse(null);
            if (teacher5 == null) {
                teacher5 = new teachers(); teacher5.setTeacherName("Pedro Martínez"); teacher5 = teacherRepo.save(teacher5); teachersCreated++;
            }

            teachers teacher6 = teacherRepo.findAll().stream().filter(t -> t.getTeacherName().equals("Laura Sánchez")).findFirst().orElse(null);
            if (teacher6 == null) {
                teacher6 = new teachers(); teacher6.setTeacherName("Laura Sánchez"); teacher6 = teacherRepo.save(teacher6); teachersCreated++;
            }

            teachers teacher7 = teacherRepo.findAll().stream().filter(t -> t.getTeacherName().equals("Miguel Torres")).findFirst().orElse(null);
            if (teacher7 == null) {
                teacher7 = new teachers(); teacher7.setTeacherName("Miguel Torres"); teacher7 = teacherRepo.save(teacher7); teachersCreated++;
            }

            teachers teacher8 = teacherRepo.findAll().stream().filter(t -> t.getTeacherName().equals("Sofia Ramírez")).findFirst().orElse(null);
            if (teacher8 == null) {
                teacher8 = new teachers(); teacher8.setTeacherName("Sofia Ramírez"); teacher8 = teacherRepo.save(teacher8); teachersCreated++;
            }

            if (teacher1 != null && math != null && teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher1.getId(), math.getId()).isEmpty()) {
                TeacherSubject ts = new TeacherSubject(); ts.setTeacher(teacher1); ts.setSubject(math); teacherSubjectRepo.save(ts);
            }
            if (teacher2 != null && physics != null && teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher2.getId(), physics.getId()).isEmpty()) {
                TeacherSubject ts = new TeacherSubject(); ts.setTeacher(teacher2); ts.setSubject(physics); teacherSubjectRepo.save(ts);
            }
            if (teacher3 != null && chemistry != null && teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher3.getId(), chemistry.getId()).isEmpty()) {
                TeacherSubject ts = new TeacherSubject(); ts.setTeacher(teacher3); ts.setSubject(chemistry); teacherSubjectRepo.save(ts);
            }
            if (teacher4 != null && biology != null && teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher4.getId(), biology.getId()).isEmpty()) {
                TeacherSubject ts = new TeacherSubject(); ts.setTeacher(teacher4); ts.setSubject(biology); teacherSubjectRepo.save(ts);
            }
            if (teacher5 != null && ethics != null && teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher5.getId(), ethics.getId()).isEmpty()) {
                TeacherSubject ts = new TeacherSubject(); ts.setTeacher(teacher5); ts.setSubject(ethics); teacherSubjectRepo.save(ts);
            }
            if (teacher6 != null && history != null && teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher6.getId(), history.getId()).isEmpty()) {
                TeacherSubject ts = new TeacherSubject(); ts.setTeacher(teacher6); ts.setSubject(history); teacherSubjectRepo.save(ts);
            }
            if (teacher7 != null && literature != null && teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher7.getId(), literature.getId()).isEmpty()) {
                TeacherSubject ts = new TeacherSubject(); ts.setTeacher(teacher7); ts.setSubject(literature); teacherSubjectRepo.save(ts);
            }
            if (teacher8 != null && english != null && teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher8.getId(), english.getId()).isEmpty()) {
                TeacherSubject ts = new TeacherSubject(); ts.setTeacher(teacher8); ts.setSubject(english); teacherSubjectRepo.save(ts);
            }

            // Crear disponibilidades si no existen
            if (teacher1 != null && availabilityRepo.findByTeacher_Id(teacher1.getId()).isEmpty()) {
                TeacherAvailability avail1 = new TeacherAvailability(); avail1.setTeacher(teacher1); avail1.setDay(Days.Lunes);
                avail1.setAmStart(LocalTime.of(8, 0)); avail1.setAmEnd(LocalTime.of(12, 0)); avail1.setPmStart(LocalTime.of(14, 0)); avail1.setPmEnd(LocalTime.of(18, 0));
                availabilityRepo.save(avail1); availabilitiesCreated++;
                TeacherAvailability avail2 = new TeacherAvailability(); avail2.setTeacher(teacher1); avail2.setDay(Days.Miércoles);
                avail2.setAmStart(LocalTime.of(8, 0)); avail2.setAmEnd(LocalTime.of(12, 0)); availabilityRepo.save(avail2); availabilitiesCreated++;
            }

            if (teacher2 != null && availabilityRepo.findByTeacher_Id(teacher2.getId()).isEmpty()) {
                TeacherAvailability avail3 = new TeacherAvailability(); avail3.setTeacher(teacher2); avail3.setDay(Days.Martes);
                avail3.setAmStart(LocalTime.of(9, 0)); avail3.setAmEnd(LocalTime.of(13, 0)); availabilityRepo.save(avail3); availabilitiesCreated++;
                TeacherAvailability avail4 = new TeacherAvailability(); avail4.setTeacher(teacher2); avail4.setDay(Days.Jueves);
                avail4.setAmStart(LocalTime.of(9, 0)); avail4.setAmEnd(LocalTime.of(13, 0)); availabilityRepo.save(avail4); availabilitiesCreated++;
            }

            if (teacher3 != null && availabilityRepo.findByTeacher_Id(teacher3.getId()).isEmpty()) {
                TeacherAvailability avail5 = new TeacherAvailability(); avail5.setTeacher(teacher3); avail5.setDay(Days.Viernes);
                avail5.setAmStart(LocalTime.of(10, 0)); avail5.setAmEnd(LocalTime.of(14, 0)); avail5.setPmStart(LocalTime.of(15, 0)); avail5.setPmEnd(LocalTime.of(19, 0));
                availabilityRepo.save(avail5); availabilitiesCreated++;
            }

            if (teacher4 != null && availabilityRepo.findByTeacher_Id(teacher4.getId()).isEmpty()) {
                TeacherAvailability avail6 = new TeacherAvailability(); avail6.setTeacher(teacher4); avail6.setDay(Days.Lunes);
                avail6.setAmStart(LocalTime.of(10, 0)); avail6.setAmEnd(LocalTime.of(14, 0)); availabilityRepo.save(avail6); availabilitiesCreated++;
                TeacherAvailability avail7 = new TeacherAvailability(); avail7.setTeacher(teacher4); avail7.setDay(Days.Jueves);
                avail7.setAmStart(LocalTime.of(10, 0)); avail7.setAmEnd(LocalTime.of(14, 0)); availabilityRepo.save(avail7); availabilitiesCreated++;
            }

            if (teacher5 != null && availabilityRepo.findByTeacher_Id(teacher5.getId()).isEmpty()) {
                TeacherAvailability avail8 = new TeacherAvailability(); avail8.setTeacher(teacher5); avail8.setDay(Days.Martes);
                avail8.setAmStart(LocalTime.of(11, 0)); avail8.setAmEnd(LocalTime.of(15, 0)); availabilityRepo.save(avail8); availabilitiesCreated++;
                TeacherAvailability avail9 = new TeacherAvailability(); avail9.setTeacher(teacher5); avail9.setDay(Days.Viernes);
                avail9.setAmStart(LocalTime.of(11, 0)); avail9.setAmEnd(LocalTime.of(15, 0)); availabilityRepo.save(avail9); availabilitiesCreated++;
            }

            if (teacher6 != null && availabilityRepo.findByTeacher_Id(teacher6.getId()).isEmpty()) {
                TeacherAvailability avail10 = new TeacherAvailability(); avail10.setTeacher(teacher6); avail10.setDay(Days.Miércoles);
                avail10.setAmStart(LocalTime.of(9, 0)); avail10.setAmEnd(LocalTime.of(13, 0)); availabilityRepo.save(avail10); availabilitiesCreated++;
                TeacherAvailability avail11 = new TeacherAvailability(); avail11.setTeacher(teacher6); avail11.setDay(Days.Viernes);
                avail11.setAmStart(LocalTime.of(9, 0)); avail11.setAmEnd(LocalTime.of(13, 0)); availabilityRepo.save(avail11); availabilitiesCreated++;
            }

            if (teacher7 != null && availabilityRepo.findByTeacher_Id(teacher7.getId()).isEmpty()) {
                TeacherAvailability avail12 = new TeacherAvailability(); avail12.setTeacher(teacher7); avail12.setDay(Days.Lunes);
                avail12.setAmStart(LocalTime.of(13, 0)); avail12.setAmEnd(LocalTime.of(17, 0)); availabilityRepo.save(avail12); availabilitiesCreated++;
                TeacherAvailability avail13 = new TeacherAvailability(); avail13.setTeacher(teacher7); avail13.setDay(Days.Miércoles);
                avail13.setAmStart(LocalTime.of(13, 0)); avail13.setAmEnd(LocalTime.of(17, 0)); availabilityRepo.save(avail13); availabilitiesCreated++;
            }

            if (teacher8 != null && availabilityRepo.findByTeacher_Id(teacher8.getId()).isEmpty()) {
                TeacherAvailability avail14 = new TeacherAvailability(); avail14.setTeacher(teacher8); avail14.setDay(Days.Martes);
                avail14.setAmStart(LocalTime.of(14, 0)); avail14.setAmEnd(LocalTime.of(18, 0)); availabilityRepo.save(avail14); availabilitiesCreated++;
                TeacherAvailability avail15 = new TeacherAvailability(); avail15.setTeacher(teacher8); avail15.setDay(Days.Jueves);
                avail15.setAmStart(LocalTime.of(14, 0)); avail15.setAmEnd(LocalTime.of(18, 0)); availabilityRepo.save(avail15); availabilitiesCreated++;
            }

            if (teachersCreated > 0 || availabilitiesCreated > 0) {
                System.out.println(">> Profesores adicionales creados: " + teachersCreated + ", disponibilidades: " + availabilitiesCreated);
            }

            // Crear cursos adicionales si no existen específicamente
            // Usar las variables de profesores y materias ya declaradas arriba

            // Crear cursos con profesores asignados si no existen
            int assignedCreated = 0;

            // Asegurarse de que las materias estén cargadas
            if (math == null) math = subjectRepo.findBySubjectName("Matemáticas");
            if (physics == null) physics = subjectRepo.findBySubjectName("Física");
            if (chemistry == null) chemistry = subjectRepo.findBySubjectName("Química");
            if (biology == null) biology = subjectRepo.findBySubjectName("Biología");
            if (ethics == null) ethics = subjectRepo.findBySubjectName("Ética");
            if (history == null) history = subjectRepo.findBySubjectName("Historia");
            if (literature == null) literature = subjectRepo.findBySubjectName("Literatura");
            if (english == null) english = subjectRepo.findBySubjectName("Inglés");

            // Cursos básicos (1A-4B)
            if (courseRepo.findAll().stream().noneMatch(c -> c.getCourseName().equals("1A")) && teacher1 != null && math != null) {
                TeacherSubject ts = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher1.getId(), math.getId()).orElse(null);
                if (ts != null) { courses c = new courses(); c.setCourseName("1A"); c.setTeacherSubject(ts); courseRepo.save(c); assignedCreated++; }
            }
            if (courseRepo.findAll().stream().noneMatch(c -> c.getCourseName().equals("1B")) && teacher2 != null && physics != null) {
                TeacherSubject ts = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher2.getId(), physics.getId()).orElse(null);
                if (ts != null) { courses c = new courses(); c.setCourseName("1B"); c.setTeacherSubject(ts); courseRepo.save(c); assignedCreated++; }
            }
            if (courseRepo.findAll().stream().noneMatch(c -> c.getCourseName().equals("2A")) && teacher3 != null && chemistry != null) {
                TeacherSubject ts = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher3.getId(), chemistry.getId()).orElse(null);
                if (ts != null) { courses c = new courses(); c.setCourseName("2A"); c.setTeacherSubject(ts); courseRepo.save(c); assignedCreated++; }
            }
            if (courseRepo.findAll().stream().noneMatch(c -> c.getCourseName().equals("2B")) && teacher4 != null && biology != null) {
                TeacherSubject ts = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher4.getId(), biology.getId()).orElse(null);
                if (ts != null) { courses c = new courses(); c.setCourseName("2B"); c.setTeacherSubject(ts); courseRepo.save(c); assignedCreated++; }
            }
            if (courseRepo.findAll().stream().noneMatch(c -> c.getCourseName().equals("3A")) && teacher5 != null && ethics != null) {
                TeacherSubject ts = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher5.getId(), ethics.getId()).orElse(null);
                if (ts != null) { courses c = new courses(); c.setCourseName("3A"); c.setTeacherSubject(ts); courseRepo.save(c); assignedCreated++; }
            }
            if (courseRepo.findAll().stream().noneMatch(c -> c.getCourseName().equals("3B")) && teacher6 != null && history != null) {
                TeacherSubject ts = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher6.getId(), history.getId()).orElse(null);
                if (ts != null) { courses c = new courses(); c.setCourseName("3B"); c.setTeacherSubject(ts); courseRepo.save(c); assignedCreated++; }
            }
            if (courseRepo.findAll().stream().noneMatch(c -> c.getCourseName().equals("4A")) && teacher7 != null && literature != null) {
                TeacherSubject ts = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher7.getId(), literature.getId()).orElse(null);
                if (ts != null) { courses c = new courses(); c.setCourseName("4A"); c.setTeacherSubject(ts); courseRepo.save(c); assignedCreated++; }
            }
            if (courseRepo.findAll().stream().noneMatch(c -> c.getCourseName().equals("4B")) && teacher8 != null && english != null) {
                TeacherSubject ts = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher8.getId(), english.getId()).orElse(null);
                if (ts != null) { courses c = new courses(); c.setCourseName("4B"); c.setTeacherSubject(ts); courseRepo.save(c); assignedCreated++; }
            }

            // Cursos adicionales (5A-8B) reutilizando profesores
            if (courseRepo.findAll().stream().noneMatch(c -> c.getCourseName().equals("5A")) && teacher1 != null && math != null) {
                TeacherSubject ts = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher1.getId(), math.getId()).orElse(null);
                if (ts != null) { courses c = new courses(); c.setCourseName("5A"); c.setTeacherSubject(ts); courseRepo.save(c); assignedCreated++; }
            }
            if (courseRepo.findAll().stream().noneMatch(c -> c.getCourseName().equals("5B")) && teacher2 != null && physics != null) {
                TeacherSubject ts = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher2.getId(), physics.getId()).orElse(null);
                if (ts != null) { courses c = new courses(); c.setCourseName("5B"); c.setTeacherSubject(ts); courseRepo.save(c); assignedCreated++; }
            }
            if (courseRepo.findAll().stream().noneMatch(c -> c.getCourseName().equals("6A")) && teacher3 != null && chemistry != null) {
                TeacherSubject ts = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher3.getId(), chemistry.getId()).orElse(null);
                if (ts != null) { courses c = new courses(); c.setCourseName("6A"); c.setTeacherSubject(ts); courseRepo.save(c); assignedCreated++; }
            }
            if (courseRepo.findAll().stream().noneMatch(c -> c.getCourseName().equals("6B")) && teacher4 != null && biology != null) {
                TeacherSubject ts = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher4.getId(), biology.getId()).orElse(null);
                if (ts != null) { courses c = new courses(); c.setCourseName("6B"); c.setTeacherSubject(ts); courseRepo.save(c); assignedCreated++; }
            }
            if (courseRepo.findAll().stream().noneMatch(c -> c.getCourseName().equals("7A")) && teacher5 != null && ethics != null) {
                TeacherSubject ts = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher5.getId(), ethics.getId()).orElse(null);
                if (ts != null) { courses c = new courses(); c.setCourseName("7A"); c.setTeacherSubject(ts); courseRepo.save(c); assignedCreated++; }
            }
            if (courseRepo.findAll().stream().noneMatch(c -> c.getCourseName().equals("7B")) && teacher6 != null && history != null) {
                TeacherSubject ts = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher6.getId(), history.getId()).orElse(null);
                if (ts != null) { courses c = new courses(); c.setCourseName("7B"); c.setTeacherSubject(ts); courseRepo.save(c); assignedCreated++; }
            }
            if (courseRepo.findAll().stream().noneMatch(c -> c.getCourseName().equals("8A")) && teacher7 != null && literature != null) {
                TeacherSubject ts = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher7.getId(), literature.getId()).orElse(null);
                if (ts != null) { courses c = new courses(); c.setCourseName("8A"); c.setTeacherSubject(ts); courseRepo.save(c); assignedCreated++; }
            }
            if (courseRepo.findAll().stream().noneMatch(c -> c.getCourseName().equals("8B")) && teacher8 != null && english != null) {
                TeacherSubject ts = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher8.getId(), english.getId()).orElse(null);
                if (ts != null) { courses c = new courses(); c.setCourseName("8B"); c.setTeacherSubject(ts); courseRepo.save(c); assignedCreated++; }
            }

            // Crear algunos cursos SIN profesores asignados (para testing de casos sin profesor)
            String[] unassignedCourses = {"9A", "9B"};
            int unassignedCreated = 0;

            for (String courseName : unassignedCourses) {
                boolean exists = courseRepo.findAll().stream()
                    .anyMatch(c -> c.getCourseName().equals(courseName));
                if (!exists) {
                    courses course = new courses();
                    course.setCourseName(courseName);
                    course.setTeacherSubject(null); // Sin profesor asignado - útil para testing
                    courseRepo.save(course);
                    unassignedCreated++;
                }
            }

            if (assignedCreated > 0 || unassignedCreated > 0) {
                System.out.println(">> Cursos de prueba creados (" + assignedCreated + " con profesores, " + unassignedCreated + " sin asignar)");
            }

            // Resumen final de datos iniciales
            long totalSubjects = subjectRepo.count();
            long totalTeachers = teacherRepo.count();
            long totalCourses = courseRepo.count();
            long totalAvailabilities = availabilityRepo.count();
            long totalAssignments = teacherSubjectRepo.count();

            System.out.println(">> DATOS INICIALES COMPLETOS:");
            System.out.println("   - Materias: " + totalSubjects);
            System.out.println("   - Profesores: " + totalTeachers);
            System.out.println("   - Cursos: " + totalCourses + " (" + assignedCreated + " con profesor, " + unassignedCreated + " sin asignar)");
            System.out.println("   - Disponibilidades: " + totalAvailabilities);
            System.out.println("   - Asignaciones profesor-materia: " + totalAssignments);
            System.out.println(">> Listo para testing de generación automática de horarios");
        };
    }
}
