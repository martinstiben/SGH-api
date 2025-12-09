package com.horarios.SGH.Service;

import org.springframework.web.multipart.MultipartFile;

public interface PhotoStorageStrategy {

    PhotoData storePhoto(MultipartFile file);

    class PhotoData {
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
