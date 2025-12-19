package com.horarios.SGH.Service;

import com.horarios.SGH.Repository.IUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Servicio personalizado para cargar detalles de usuario para autenticación Spring Security.
 * Implementa el patrón Strategy para diferentes estrategias de carga de usuarios.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de cargar usuarios
 * - OCP: Abierto para extensión mediante estrategias
 * - DIP: Depende de abstracciones (UserDetailsService)
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Value("${app.master.username}")
    private String masterUsername;

    @Value("${app.master.password}")
    private String masterPassword;

    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserLoadingStrategy userLoadingStrategy;

    /**
     * Constructor con inyección de dependencias.
     * Utiliza patrón Strategy para flexibilidad en carga de usuarios.
     *
     * @param userRepository repositorio de usuarios
     * @param passwordEncoder codificador de contraseñas
     */
    public CustomUserDetailsService(IUserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userLoadingStrategy = new DatabaseUserLoadingStrategy(userRepository, passwordEncoder, masterUsername, masterPassword);
    }

    /**
     * Carga un usuario por su nombre de usuario (email) utilizando la estrategia configurada.
     * Delega la lógica de carga a la estrategia UserLoadingStrategy.
     *
     * @param username El nombre de usuario (email) a buscar
     * @return UserDetails del usuario encontrado
     * @throws UsernameNotFoundException si el usuario no existe
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userLoadingStrategy.loadUser(username);
    }

    /**
     * Interfaz Strategy para diferentes estrategias de carga de usuarios.
     * Permite flexibilidad en cómo se cargan los usuarios (BD, cache, etc.).
     */
    interface UserLoadingStrategy {
        UserDetails loadUser(String username) throws UsernameNotFoundException;
    }

    /**
     * Estrategia concreta para cargar usuarios desde base de datos con fallback a usuario master.
     * Implementa el patrón Strategy para encapsular la lógica de carga.
     */
    static class DatabaseUserLoadingStrategy implements UserLoadingStrategy {

        private final IUserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final String masterUsername;
        private final String masterPassword;

        public DatabaseUserLoadingStrategy(IUserRepository userRepository, PasswordEncoder passwordEncoder,
                                         String masterUsername, String masterPassword) {
            this.userRepository = userRepository;
            this.passwordEncoder = passwordEncoder;
            this.masterUsername = masterUsername;
            this.masterPassword = masterPassword;
        }

        /**
         * Carga usuario desde base de datos, con fallback a usuario master.
         *
         * @param username nombre de usuario a buscar
         * @return UserDetails del usuario
         * @throws UsernameNotFoundException si no se encuentra el usuario
         */
        @Override
        public UserDetails loadUser(String username) throws UsernameNotFoundException {
            // Intentar cargar desde base de datos
            com.horarios.SGH.Model.User appUserEntity = userRepository.findByUserName(username).orElse(null);
            if (appUserEntity != null) {
                // Verificar que el email coincida exactamente
                if (!appUserEntity.getPerson().getEmail().equals(username)) {
                    throw new UsernameNotFoundException("Usuario no encontrado: " + username);
                }

                // Obtener la contraseña del usuario desde UserCredentials
                String passwordHash = appUserEntity.getUserCredentials() != null ?
                    appUserEntity.getUserCredentials().getPasswordHash() : "";

                // Obtener el rol del usuario usando getFirstRole()
                String roleName = appUserEntity.getFirstRole() != null ?
                    appUserEntity.getFirstRole().getRoleName() : "ESTUDIANTE";

                return User.withUsername(appUserEntity.getPerson().getEmail())
                          .password(passwordHash)
                          .roles(roleName)
                          .build();
            }

            // Fallback para usuario master solo si no existe en BD
            if (masterUsername.equals(username)) {
                return User.withUsername(masterUsername)
                          .password(passwordEncoder.encode(masterPassword))
                          .roles("COORDINADOR")
                          .build();
            }

            throw new UsernameNotFoundException("Usuario no encontrado: " + username);
        }
    }
}