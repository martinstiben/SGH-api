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

            // Crear usuario "Juan Saavedra"
            if (!repo.existsByUserName("saavedrajuanpis@gmail.com")) {
                if (peopleRepo.findByEmail("saavedrajuanpis@gmail.com").isPresent()) {
                    System.out.println(">> Persona con email saavedrajuanpis@gmail.com ya existe, saltando creación");
                } else {
                    People juanPerson = new People("Juan Saavedra", "saavedrajuanpis@gmail.com");
                    juanPerson = peopleRepo.save(juanPerson);

                    Roles estudianteRole = rolesRepo.findByRoleName("ESTUDIANTE")
                        .orElseGet(() -> {
                            System.out.println(">> Rol ESTUDIANTE no encontrado, creando...");
                            return rolesRepo.save(new Roles("ESTUDIANTE"));
                        });

                    users juanUser = new users(juanPerson, estudianteRole, encoder.encode("Simon12"));
                    repo.save(juanUser);
                    System.out.println(">> Usuario creado: saavedrajuanpis@gmail.com");
                }
            } else {
                System.out.println(">> Usuario ya existe: saavedrajuanpis@gmail.com");
            }
            long total = repo.count();
            System.out.println(">> Usuarios totales: " + total + " (sin límite)");
        };
    }

    @Bean
    public CommandLineRunner seedInitialData(Isubjects subjectRepo, Iteachers teacherRepo, Icourses courseRepo, TeacherSubjectRepository teacherSubjectRepo, ITeacherAvailabilityRepository availabilityRepo) {
        return args -> {
            // Crear materias si no existen
            if (subjectRepo.count() == 0) {
                subjects math = new subjects();
                math.setSubjectName("Matemáticas");
                subjectRepo.save(math);

                subjects physics = new subjects();
                physics.setSubjectName("Física");
                subjectRepo.save(physics);

                subjects chemistry = new subjects();
                chemistry.setSubjectName("Química");
                subjectRepo.save(chemistry);

                subjects biology = new subjects();
                biology.setSubjectName("Biología");
                subjectRepo.save(biology);

                subjects Etic = new subjects();
                Etic.setSubjectName("Etica");
                subjectRepo.save(Etic);

                System.out.println(">> Materias iniciales creadas");
            }

            // Crear profesores si no existen
            if (teacherRepo.count() == 0) {
                teachers teacher1 = new teachers();
                teacher1.setTeacherName("Juan Pérez");
                teacher1 = teacherRepo.save(teacher1);

                teachers teacher2 = new teachers();
                teacher2.setTeacherName("María García");
                teacher2 = teacherRepo.save(teacher2);

                teachers teacher3 = new teachers();
                teacher3.setTeacherName("Carlos López");
                teacher3 = teacherRepo.save(teacher3);

                teachers teacher4 = new teachers();
                teacher4.setTeacherName("Carlos Manchola");
                teacher4 = teacherRepo.save(teacher4);

                // Asignar especializaciones
                subjects math = subjectRepo.findBySubjectName("Matemáticas");
                if (math != null) {
                    TeacherSubject ts1 = new TeacherSubject();
                    ts1.setTeacher(teacher1);
                    ts1.setSubject(math);
                    teacherSubjectRepo.save(ts1);
                }

                subjects physics = subjectRepo.findBySubjectName("Física");
                if (physics != null) {
                    TeacherSubject ts2 = new TeacherSubject();
                    ts2.setTeacher(teacher2);
                    ts2.setSubject(physics);
                    teacherSubjectRepo.save(ts2);
                }

                subjects chemistry = subjectRepo.findBySubjectName("Química");
                if (chemistry != null) {
                    TeacherSubject ts3 = new TeacherSubject();
                    ts3.setTeacher(teacher3);
                    ts3.setSubject(chemistry);
                    teacherSubjectRepo.save(ts3);
                }

                subjects Etic = subjectRepo.findBySubjectName("Etica");
                if (Etic != null) {
                    TeacherSubject ts4 = new TeacherSubject();
                    ts4.setTeacher(teacher4);
                    ts4.setSubject(Etic);
                    teacherSubjectRepo.save(ts4);
                }
                
                // Crear disponibilidad inicial para profesores
                // Disponibilidad para Juan Pérez (Lunes y Miércoles)
                TeacherAvailability avail1 = new TeacherAvailability();
                avail1.setTeacher(teacher1);
                avail1.setDay(Days.Lunes);
                avail1.setAmStart(LocalTime.of(8, 0));
                avail1.setAmEnd(LocalTime.of(12, 0));
                avail1.setPmStart(LocalTime.of(14, 0));
                avail1.setPmEnd(LocalTime.of(18, 0));
                availabilityRepo.save(avail1);

                TeacherAvailability avail2 = new TeacherAvailability();
                avail2.setTeacher(teacher1);
                avail2.setDay(Days.Miércoles);
                avail2.setAmStart(LocalTime.of(8, 0));
                avail2.setAmEnd(LocalTime.of(12, 0));
                availabilityRepo.save(avail2);

                // Disponibilidad para María García (Martes y Jueves)
                TeacherAvailability avail3 = new TeacherAvailability();
                avail3.setTeacher(teacher2);
                avail3.setDay(Days.Martes);
                avail3.setAmStart(LocalTime.of(9, 0));
                avail3.setAmEnd(LocalTime.of(13, 0));
                availabilityRepo.save(avail3);

                TeacherAvailability avail4 = new TeacherAvailability();
                avail4.setTeacher(teacher2);
                avail4.setDay(Days.Jueves);
                avail4.setAmStart(LocalTime.of(9, 0));
                avail4.setAmEnd(LocalTime.of(13, 0));
                availabilityRepo.save(avail4);

                // Disponibilidad para Carlos López (Viernes)
                TeacherAvailability avail5 = new TeacherAvailability();
                avail5.setTeacher(teacher3);
                avail5.setDay(Days.Viernes);
                avail5.setAmStart(LocalTime.of(10, 0));
                avail5.setAmEnd(LocalTime.of(14, 0));
                avail5.setPmStart(LocalTime.of(15, 0));
                avail5.setPmEnd(LocalTime.of(19, 0));
                availabilityRepo.save(avail5);

                System.out.println(">> Profesores y disponibilidad iniciales creados");
            }

            // Crear cursos si no existen
            if (courseRepo.count() == 0) {
                // Obtener las especializaciones creadas
                subjects math = subjectRepo.findBySubjectName("Matemáticas");
                subjects physics = subjectRepo.findBySubjectName("Física");
                subjects chemistry = subjectRepo.findBySubjectName("Química");
                subjects Etic = subjectRepo.findBySubjectName("Etica");

                teachers teacher1 = teacherRepo.findAll().stream().filter(t -> t.getTeacherName().equals("Juan Pérez")).findFirst().orElse(null);
                teachers teacher2 = teacherRepo.findAll().stream().filter(t -> t.getTeacherName().equals("María García")).findFirst().orElse(null);
                teachers teacher3 = teacherRepo.findAll().stream().filter(t -> t.getTeacherName().equals("Carlos López")).findFirst().orElse(null);
                teachers teacher4 = teacherRepo.findAll().stream().filter(t -> t.getTeacherName().equals("Carlos Manchola")).findFirst().orElse(null);

                // Asignar teacherSubject a cursos
                TeacherSubject ts1 = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher1.getId(), math.getId()).orElse(null);
                courses course1 = new courses();
                course1.setCourseName("1A");
                course1.setTeacherSubject(ts1);
                courseRepo.save(course1);

                TeacherSubject ts2 = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher2.getId(), physics.getId()).orElse(null);
                courses course2 = new courses();
                course2.setCourseName("2B");
                course2.setTeacherSubject(ts2);
                courseRepo.save(course2);

                TeacherSubject ts3 = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher3.getId(), chemistry.getId()).orElse(null);
                courses course3 = new courses();
                course3.setCourseName("3C");
                course3.setTeacherSubject(ts3);
                courseRepo.save(course3);

                TeacherSubject ts4 = teacherSubjectRepo.findByTeacher_IdAndSubject_Id(teacher4.getId(), Etic.getId()).orElse(null);
                courses course4 = new courses();
                course3.setCourseName("3C");
                course3.setTeacherSubject(ts4);
                courseRepo.save(course4);

                System.out.println(">> Cursos iniciales creados");
            }
        };
    }
}
