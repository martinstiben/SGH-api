package com.horarios.SGH.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuración para el envío asíncrono de notificaciones
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * Executor pool para envío de correos electrónicos
     */
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Email-");
        executor.setKeepAliveSeconds(60);
        executor.initialize();
        return executor;
    }
    
    /**
     * Executor pool para procesamiento general
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(15);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("Task-");
        executor.setKeepAliveSeconds(30);
        executor.initialize();
        return executor;
    }
}