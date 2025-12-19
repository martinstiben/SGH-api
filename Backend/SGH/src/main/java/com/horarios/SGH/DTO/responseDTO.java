package com.horarios.SGH.DTO;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO genérico para respuestas del sistema SGH.
 * Implementa métodos de utilidad para respuestas estandarizadas.
 *
 * @author Sistema SGH
 * @version 1.0
 */
public class responseDTO extends AbstractDTO {
    private String status;
    private String message;

    /**
     * Timestamp de la respuesta.
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización.
     */
    private LocalDateTime updatedAt;

    /**
     * Datos adicionales opcionales.
     */
    private Map<String, Object> data;

    /**
     * Código de error específico (opcional).
     */
    private String errorCode;

    /**
     * Constructor vacío.
     */
    public responseDTO() {
        super();
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor con estado y mensaje.
     */
    public responseDTO(String status, String message){
        super();
        this.status = status;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Método Factory para crear una respuesta exitosa.
     */
    public static responseDTO success(String message) {
        return new responseDTO("OK", message);
    }

    /**
     * Método Factory para crear una respuesta de error.
     */
    public static responseDTO error(String message) {
        return new responseDTO("ERROR", message);
    }

    /**
     * Verifica si la respuesta es exitosa.
     */
    public boolean isSuccess() {
        return "OK".equals(status);
    }

    /**
     * Verifica si la respuesta es un error.
     */
    public boolean isError() {
        return "ERROR".equals(status);
    }

    /**
     * Valida si el DTO tiene información básica completa.
     */
    @Override
    public boolean isValid() {
        return status != null && !status.trim().isEmpty() &&
               message != null && !message.trim().isEmpty();
    }

    /**
     * Obtiene una representación resumida de la respuesta.
     * Formato: "[status]: [message]"
     *
     * @return Representación resumida
     */
    @Override
    public String getSummary() {
        return String.format("%s: %s",
                status != null ? status : "Sin estado",
                message != null ? message : "Sin mensaje");
    }

    @Override
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return createdAt;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.createdAt = timestamp;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}