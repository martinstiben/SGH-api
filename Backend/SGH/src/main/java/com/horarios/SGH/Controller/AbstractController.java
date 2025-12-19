package com.horarios.SGH.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import java.util.Map;

/**
 * Clase abstracta base para todos los controladores del sistema SGH.
 * Implementa el patrón Template Method para operaciones comunes como
 * validación de entrada, manejo de errores y respuestas estándar.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de proporcionar funcionalidades comunes a controladores
 * - OCP: Abierto para extensión por subclases
 *
 * @author Sistema SGH
 * @version 1.0
 */
public abstract class AbstractController {

    /**
     * Maneja errores de validación de Spring.
     * Implementa patrón Template Method para validación consistente.
     *
     * @param bindingResult Resultado de validación de Spring
     * @return ResponseEntity con error si hay problemas, null si válido
     */
    protected ResponseEntity<?> handleValidationErrors(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .findFirst()
                    .orElse("Error de validación");
            return ResponseEntity.badRequest().body(Map.of("error", errorMessage));
        }
        return null;
    }

    /**
     * Maneja excepciones genéricas.
     * Proporciona respuesta estándar para errores internos.
     *
     * @param e Excepción ocurrida
     * @return ResponseEntity con error interno
     */
    protected ResponseEntity<?> handleException(Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", "Error interno del servidor"));
    }

    /**
     * Crea respuesta de éxito estándar.
     *
     * @param message Mensaje de éxito
     * @return ResponseEntity con éxito
     */
    protected ResponseEntity<?> successResponse(String message) {
        return ResponseEntity.ok(Map.of("message", message));
    }

    /**
     * Crea respuesta de error estándar.
     *
     * @param message Mensaje de error
     * @return ResponseEntity con error
     */
    protected ResponseEntity<?> errorResponse(String message) {
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    /**
     * Crea respuesta de error para DTO responseDTO.
     *
     * @param message Mensaje de error
     * @return ResponseEntity con responseDTO de error
     */
    protected ResponseEntity<com.horarios.SGH.DTO.responseDTO> errorResponseDTO(String message) {
        return ResponseEntity.badRequest().body(new com.horarios.SGH.DTO.responseDTO("ERROR", message));
    }

    /**
     * Crea respuesta de éxito para DTO responseDTO.
     *
     * @param message Mensaje de éxito
     * @return ResponseEntity con responseDTO de éxito
     */
    protected ResponseEntity<com.horarios.SGH.DTO.responseDTO> successResponseDTO(String message) {
        return ResponseEntity.ok(new com.horarios.SGH.DTO.responseDTO("OK", message));
    }
}