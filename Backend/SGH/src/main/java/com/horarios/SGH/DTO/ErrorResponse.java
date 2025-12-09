package com.horarios.SGH.DTO;

import java.time.LocalDateTime;

public class ErrorResponse {
    private String message;
    private String code;
    private LocalDateTime timestamp;

    public ErrorResponse() {}

    public ErrorResponse(String message, String code) {
        this.message = message;
        this.code = code;
        this.timestamp = LocalDateTime.now();
    }

    public static ErrorResponse of(String message, String code) {
        return new ErrorResponse(message, code);
    }

    public String getMessage() { return message; }
    public String getCode() { return code; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public void setMessage(String message) { this.message = message; }
    public void setCode(String code) { this.code = code; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
