package com.horarios.SGH.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.horarios.SGH.Model.users;
import com.horarios.SGH.Repository.Iusers;
import com.horarios.SGH.Repository.IPeopleRepository;
import com.horarios.SGH.Repository.IRolesRepository;
import com.horarios.SGH.Model.People;
import com.horarios.SGH.Model.Roles;
import com.horarios.SGH.Model.AccountStatus;

import org.springframework.boot.CommandLineRunner;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Value("${app.master.name:master}")
    private String masterUsername;

    @Value("${app.master.password:Master$2025!}")
    private String masterPassword;

    private void createUserIfNotExists(String email, String name, String password, String roleName, Iusers repo, PasswordEncoder encoder, IPeopleRepository peopleRepo, IRolesRepository rolesRepo) {
        if (!repo.existsByUserName(email)) {
            if (peopleRepo.findByEmail(email).isPresent()) {
                logger.info("Persona con email {} ya existe, saltando creación", email);
                return;
            }

            People person = new People(name, email);
            person = peopleRepo.save(person);

            Roles role = rolesRepo.findByRoleName(roleName)
                .orElseGet(() -> {
                    logger.info("Rol {} no encontrado, creando...", roleName);
                    return rolesRepo.save(new Roles(roleName));
                });

            users user = new users(person, role, encoder.encode(password));
            user.setVerified(true);
            user.setAccountStatus(AccountStatus.ACTIVE);
            repo.save(user);
            logger.info("Usuario {} creado: {}", roleName.toLowerCase(), email);
        } else {
            logger.info("Usuario {} ya existe: {}", roleName.toLowerCase(), email);
        }
    }

    @Bean
    public CommandLineRunner seedRolesAndMasterUser(Iusers repo, PasswordEncoder encoder, IPeopleRepository peopleRepo, IRolesRepository rolesRepo) {
        return args -> {
            // Primero crear los roles si no existen
            if (rolesRepo.count() == 0) {
                rolesRepo.save(new Roles("MAESTRO"));
                rolesRepo.save(new Roles("ESTUDIANTE"));
                rolesRepo.save(new Roles("COORDINADOR"));
                rolesRepo.save(new Roles("DIRECTOR_DE_AREA"));
                logger.info("Roles iniciales creados");
            } else {
                logger.info("Roles ya existen");
            }
            
            // Ahora crear el usuario master
            createUserIfNotExists(masterUsername, "Master User", masterPassword, "MAESTRO", repo, encoder, peopleRepo, rolesRepo);

            // Crear usuario coordinador si no existe
            String coordinatorEmail = "saavedrajuanpis@gmail.com";
            String coordinatorName = "Juan Pablo Saavedra";
            String coordinatorPassword = "Simon1234";

            createUserIfNotExists(coordinatorEmail, coordinatorName, coordinatorPassword, "COORDINADOR", repo, encoder, peopleRepo, rolesRepo);

            long total = repo.count();
            logger.info("Usuarios totales: {} (sin límite)", total);
        };
    }

}
