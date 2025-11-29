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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(UserDetailsService userDetailsService,
                                                           JwtTokenProvider jwtTokenProvider,
                                                           TokenRevocationService tokenRevocationService) {
        return new JwtAuthenticationFilter(userDetailsService, jwtTokenProvider, tokenRevocationService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:3001",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:3001",
            "http://localhost:5173",
            "http://127.0.0.1:5173"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/auth/**",
                    "/teachers/**",
                    "/subjects/**",
                    "/courses/**",
                    "/schedules/history",
                    "/schedules/debug-courses",
                    "/schedules/pdf/**",
                    "/schedules/excel/**",
                    "/schedules/image/**",
                    "/schedules-crud/by-course/**",
                    "/schedules-crud/by-teacher/**",
                    "/availability/**",
                    "/users/*/photo",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/api-docs/**",
                    "/actuator/**" // ✅ acceso público para health checks
                ).permitAll()
                .requestMatchers("/courses/*/students").hasAuthority("ROLE_COORDINADOR")
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/schedules-crud/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/schedules-crud/**").authenticated()
                .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/schedules-crud/**").authenticated()
                .requestMatchers("/schedules-crud/my-schedule").authenticated()
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
