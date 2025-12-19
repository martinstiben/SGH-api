package com.horarios.SGH.Config;

import com.horarios.SGH.jwt.JwtAuthenticationEntryPoint;
import com.horarios.SGH.jwt.JwtAuthenticationFilter;
import com.horarios.SGH.jwt.JwtTokenProvider;
import com.horarios.SGH.Service.TokenRevocationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

/**
 * Configuración de seguridad para la aplicación Spring Boot.
 * Implementa autenticación JWT stateless con CORS configurado para desarrollo.
 * Aplica el patrón Strategy para manejo de autenticación y autorización.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Lista de orígenes permitidos para CORS en desarrollo.
     */
    private static final String[] ALLOWED_ORIGINS = {
        "http://localhost:3000",    // Next.js
        "http://localhost:3001",    // Next.js dev
        "http://localhost:5173",    // Vite dev server
        "http://127.0.0.1:3000",
        "http://127.0.0.1:3001",
        "http://127.0.0.1:5173",
        "http://127.0.0.1:5500",   // Live Server
        "http://localhost:5500",   // Live Server
        "http://10.3.226.178:19000", // Expo Go
        "http://10.3.226.178:8081",  // Metro bundler
        "http://172.30.5.58:8085"   // Metro bundler
    };

    /**
     * Métodos HTTP permitidos para CORS.
     */
    private static final String[] ALLOWED_METHODS = {"GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"};

    /**
     * Cache de preflight CORS en segundos.
     */
    private static final long MAX_AGE_SECONDS = 3600L;

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * Constructor con inyección de dependencias.
     * Aplica el principio de Inversión de Dependencias.
     *
     * @param jwtAuthenticationEntryPoint Punto de entrada para errores de autenticación
     */
    public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    /**
     * Crea el filtro de autenticación JWT con dependencias inyectadas.
     * Implementa el patrón Decorator sobre el filtro base de Spring Security.
     *
     * @param userDetailsService Servicio para cargar detalles de usuario
     * @param jwtTokenProvider Proveedor de tokens JWT
     * @param tokenRevocationService Servicio de revocación de tokens
     * @return Filtro JWT configurado
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(UserDetailsService userDetailsService,
                                                           JwtTokenProvider jwtTokenProvider,
                                                           TokenRevocationService tokenRevocationService) {
        return new JwtAuthenticationFilter(userDetailsService, jwtTokenProvider, tokenRevocationService);
    }

    /**
     * Configura el codificador de contraseñas usando BCrypt.
     * Implementa el patrón Strategy para algoritmos de hash.
     *
     * @return Codificador BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // BCrypt para login con password en texto plano
    }

    /**
     * Configura la fuente de configuración CORS para permitir orígenes de desarrollo.
     * Implementa el patrón Singleton para la configuración CORS.
     *
     * @return Configuración CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Permitir múltiples orígenes de desarrollo y producción
        configuration.setAllowedOrigins(Arrays.asList(ALLOWED_ORIGINS));
        configuration.setAllowedMethods(Arrays.asList(ALLOWED_METHODS));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(MAX_AGE_SECONDS); // Cache preflight por 1 hora

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Configura el AuthenticationManager de Spring Security.
     * Delega la configuración al framework manteniendo compatibilidad.
     *
     * @param authConfig Configuración de autenticación
     * @return AuthenticationManager configurado
     * @throws Exception si ocurre error en configuración
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Configura la cadena de filtros de seguridad.
     * Define reglas de autorización, CORS, manejo de excepciones y filtros personalizados.
     * Implementa el patrón Chain of Responsibility para procesamiento de requests.
     *
     * @param http Configuración HTTP de Spring Security
     * @param jwtAuthenticationFilter Filtro JWT personalizado
     * @param corsConfigurationSource Configuración CORS
     * @return Cadena de filtros configurada
     * @throws Exception si ocurre error en configuración
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                    JwtAuthenticationFilter jwtAuthenticationFilter,
                                                    CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos (sin autenticación)
                .requestMatchers(
                    "/auth/**",          // login y register
                    "/teachers/**",      // CRUD completo de profesores
                    "/subjects/**",      // materias para dashboard
                    "/courses/**",       // cursos para dashboard (excepto estudiantes)
                    "/schedules/history", // historial de horarios
                    "/schedules/debug-courses", // debug estado de cursos
                    "/schedules/pdf/**", // exportar PDFs
                    "/schedules/excel/**", // exportar Excel
                    "/schedules/image/**", // exportar imágenes
                    "/schedules-crud/by-course/**",  // ver horarios de curso
                    "/schedules-crud/by-teacher/**", // ver horarios de profesor
                    "/availability/**",  // disponibilidad de profesores
                    "/users/*/photo",    // obtener foto de usuario
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/api-docs/**"
                ).permitAll()
                // Endpoint específico para estudiantes requiere rol COORDINADOR
                .requestMatchers("/courses/*/students").hasAuthority("ROLE_COORDINADOR")
                // Endpoints que requieren autenticación para operaciones de escritura
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/schedules-crud/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/schedules-crud/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/schedules-crud/**").authenticated()
                // Endpoint específico para estudiantes requiere autenticación
                .requestMatchers("/schedules-crud/my-schedule").authenticated()
                // Solo subjects y courses requieren autenticación para operaciones de escritura
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/subjects/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/subjects/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/subjects/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/courses/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/courses/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/courses/**").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}