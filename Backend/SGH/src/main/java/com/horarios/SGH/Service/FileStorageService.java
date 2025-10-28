package com.horarios.SGH.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Servicio responsable del manejo de archivos de imagen en base de datos.
 * Principio de responsabilidad única (SRP): Solo maneja el procesamiento de imágenes.
 */
@Service
public class FileStorageService {

    /**
     * Procesa un archivo de imagen y retorna sus datos binarios.
     * @param file Archivo multipart a procesar
     * @return PhotoData conteniendo los datos binarios, tipo de contenido y nombre del archivo
     * @throws IllegalArgumentException Si el archivo no es válido
     * @throws RuntimeException Si ocurre un error de I/O
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
     * Valida que el archivo sea una imagen válida y no exceda el tamaño máximo.
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
     * Clase interna para encapsular los datos de la foto.
     */
    public static class PhotoData {
        private byte[] data;
        private String contentType;
        private String fileName;

        public byte[] getData() { return data; }
        public void setData(byte[] data) { this.data = data; }

        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
    }
}