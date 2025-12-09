package com.horarios.SGH.Service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FilePhotoStorageStrategy implements PhotoStorageStrategy {

    private final FileStorageService fileStorageService;

    public FilePhotoStorageStrategy(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public PhotoData storePhoto(MultipartFile file) {
        FileStorageService.PhotoData d = fileStorageService.processImageFile(file);
        PhotoData pd = new PhotoData();
        pd.setData(d.getData());
        pd.setContentType(d.getContentType());
        pd.setFileName(d.getFileName());
        return pd;
    }
}
