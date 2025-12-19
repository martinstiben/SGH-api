package com.horarios.SGH.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Servicio para el manejo y procesamiento de archivos de imagen.
 * Implementa validación y procesamiento de imágenes para almacenamiento en base de datos.
 *
 * Principios SOLID aplicados:
 * - SRP: Responsabilidad única de procesar archivos de imagen
 * - DIP: No depende de implementaciones concretas de almacenamiento
 *
 * Funcionalidades:
 * - Validación de tipo y tamaño de archivo
 * - Procesamiento de imágenes multipart
 * - Conversión a formato binario para BD
 *
 * @author Sistema SGH
 * @version 1.0
 */
@Service
public class FileStorageService {

    /**
     * Procesa un archivo de imagen multipart y extrae sus datos para almacenamiento.
     * Realiza validación completa del archivo antes del procesamiento.
     *
     * @param file Archivo multipart recibido del cliente. No debe ser null.
     * @return PhotoData conteniendo los datos binarios, tipo MIME y nombre original del archivo
     * @throws IllegalArgumentException si el archivo no cumple con las validaciones
     * @throws RuntimeException si ocurre un error de I/O durante el procesamiento
     */
    public PhotoData processImageFile(MultipartFile file) {
        validateImageFile(file);

        try {
            PhotoData photoData = new PhotoData();
            photoData.setData(file.getBytes());
            photoData.setContentType(file.getContentType());
            photoData.setFileName(file.getOriginalFilename());
            return photoData;
        } catch (IOException e) {
            throw new RuntimeException("Error al procesar el archivo: " + e.getMessage(), e);
        }
    }

    /**
     * Valida que el archivo multipart sea una imagen válida y cumpla con restricciones.
     * Verifica que no sea null/vacío, sea de tipo imagen y no exceda el tamaño máximo.
     *
     * @param file Archivo a validar
     * @throws IllegalArgumentException si alguna validación falla
     */
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        }

        // Validar tamaño máximo (2MB)
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new IllegalArgumentException("El archivo no puede exceder los 2MB");
        }
    }

    /**
     * Clase de datos para encapsular información de una imagen procesada.
     * Contiene los datos binarios, tipo MIME y nombre original del archivo.
     * Se utiliza como DTO para transferir datos de imagen entre capas.
     */
    public static class PhotoData {
        private byte[] data;
        private String contentType;
        private String fileName;

        /**
         * Obtiene los datos binarios de la imagen.
         * @return Array de bytes con el contenido de la imagen
         */
        public byte[] getData() { return data; }

        /**
         * Establece los datos binarios de la imagen.
         * @param data Array de bytes con el contenido de la imagen
         */
        public void setData(byte[] data) { this.data = data; }

        /**
         * Obtiene el tipo MIME del archivo de imagen.
         * @return Tipo de contenido (ej. "image/jpeg", "image/png")
         */
        public String getContentType() { return contentType; }

        /**
         * Establece el tipo MIME del archivo de imagen.
         * @param contentType Tipo de contenido MIME
         */
        public void setContentType(String contentType) { this.contentType = contentType; }

        /**
         * Obtiene el nombre original del archivo.
         * @return Nombre del archivo como fue subido por el usuario
         */
        public String getFileName() { return fileName; }

        /**
         * Establece el nombre original del archivo.
         * @param fileName Nombre del archivo original
         */
        public void setFileName(String fileName) { this.fileName = fileName; }
    }
}