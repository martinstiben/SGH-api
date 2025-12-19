package com.horarios.SGH.Config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Configuración para OpenAPI (Swagger) y serialización JSON.
 * Configura la documentación de la API y el formato de serialización de fechas.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Configuration
public class OpenApiConfig {

    /**
     * Nombre del esquema de seguridad para autenticación Bearer.
     */
    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    /**
     * Patrón de formato para LocalTime en serialización JSON.
     */
    private static final String LOCAL_TIME_PATTERN = "HH:mm";

    /**
     * Configura la documentación OpenAPI para la API REST.
     * Define información básica de la API y esquema de seguridad JWT.
     *
     * @return Configuración OpenAPI
     */
    @Bean
    public OpenAPI api() {
        return new OpenAPI()
                .info(new Info()
                        .title("SGH API")
                        .description("API del Sistema de Generación de Horarios")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    /**
     * Configura el ObjectMapper principal para serialización JSON.
     * Registra módulo para manejo de tipos Java 8 Time, especialmente LocalTime.
     *
     * @return ObjectMapper configurado
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();

        // Configurar LocalTime para que se serialice como string "HH:mm"
        module.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(LOCAL_TIME_PATTERN)));

        mapper.registerModule(module);
        return mapper;
    }
}