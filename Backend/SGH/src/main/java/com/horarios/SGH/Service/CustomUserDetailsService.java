package com.horarios.SGH.Service;

import com.horarios.SGH.Model.users;
import com.horarios.SGH.Repository.Iusers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Servicio personalizado para cargar detalles de usuario para autenticación.
 * Maneja la carga de usuarios desde la base de datos y proporciona un usuario master como fallback.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Value("${app.master.username}")
    private String masterUsername;

    @Value("${app.master.password}")
    private String masterPassword;

    private final Iusers userRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomUserDetailsService(Iusers userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Carga un usuario por su nombre de usuario (email).
     * Primero intenta cargar desde la base de datos, luego usa el usuario master como fallback.
     *
     * @param username El nombre de usuario (email) a buscar
     * @return UserDetails del usuario encontrado
     * @throws UsernameNotFoundException si el usuario no existe
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Intentar cargar desde base de datos
        users user = userRepository.findByUserName(username).orElse(null);
        if (user != null) {
            // Verificar que el email coincida exactamente
            if (!user.getPerson().getEmail().equals(username)) {
                throw new UsernameNotFoundException("Usuario no encontrado: " + username);
            }

            return User.withUsername(user.getPerson().getEmail())
                    .password(user.getPasswordHash())
                    .roles(user.getRole().getRoleName())
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