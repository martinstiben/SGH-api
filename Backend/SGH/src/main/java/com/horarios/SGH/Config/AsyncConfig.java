package com.horarios.SGH.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configuración para el envío asíncrono de notificaciones y tareas.
 * Implementa el patrón Factory Method para crear ejecutores de hilos optimizados.
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Constante para el tamaño del pool de hilos para emails.
     */
    private static final int EMAIL_CORE_POOL_SIZE = 5;

    /**
     * Constante para el tamaño máximo del pool de hilos para emails.
     */
    private static final int EMAIL_MAX_POOL_SIZE = 20;

    /**
     * Constante para la capacidad de cola para emails.
     */
    private static final int EMAIL_QUEUE_CAPACITY = 100;

    /**
     * Constante para el tiempo de vida de hilos para emails.
     */
    private static final int EMAIL_KEEP_ALIVE_SECONDS = 60;

    /**
     * Constante para el tamaño del pool de hilos para tareas generales.
     */
    private static final int TASK_CORE_POOL_SIZE = 3;

    /**
     * Constante para el tamaño máximo del pool de hilos para tareas generales.
     */
    private static final int TASK_MAX_POOL_SIZE = 15;

    /**
     * Constante para la capacidad de cola para tareas generales.
     */
    private static final int TASK_QUEUE_CAPACITY = 50;

    /**
     * Constante para el tiempo de vida de hilos para tareas generales.
     */
    private static final int TASK_KEEP_ALIVE_SECONDS = 30;

    /**
     * Crea un executor pool optimizado para envío de correos electrónicos.
     * Aplica el patrón Factory Method para encapsular la creación del executor.
     *
     * @return Executor configurado para emails
     */
    @Bean(name = "emailExecutor")
    public Executor emailExecutor() {
        return createThreadPoolExecutor("Email-", EMAIL_CORE_POOL_SIZE, EMAIL_MAX_POOL_SIZE,
                                      EMAIL_QUEUE_CAPACITY, EMAIL_KEEP_ALIVE_SECONDS);
    }

    /**
     * Crea un executor pool optimizado para procesamiento general de tareas.
     * Aplica el patrón Factory Method para encapsular la creación del executor.
     *
     * @return Executor configurado para tareas generales
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        return createThreadPoolExecutor("Task-", TASK_CORE_POOL_SIZE, TASK_MAX_POOL_SIZE,
                                      TASK_QUEUE_CAPACITY, TASK_KEEP_ALIVE_SECONDS);
    }

    /**
     * Método factory para crear ejecutores de hilos con configuración específica.
     * Implementa el patrón Factory Method para evitar duplicación de código.
     *
     * @param threadNamePrefix Prefijo para el nombre de los hilos
     * @param corePoolSize Tamaño mínimo del pool
     * @param maxPoolSize Tamaño máximo del pool
     * @param queueCapacity Capacidad de la cola
     * @param keepAliveSeconds Tiempo de vida de hilos inactivos
     * @return Executor configurado
     */
    private Executor createThreadPoolExecutor(String threadNamePrefix, int corePoolSize,
                                            int maxPoolSize, int queueCapacity, int keepAliveSeconds) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.initialize();
        return executor;
    }
}