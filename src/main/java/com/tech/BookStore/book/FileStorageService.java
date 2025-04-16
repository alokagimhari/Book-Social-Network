package com.tech.BookStore.book;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${application.file.upload.photos-output-path}")
    private String fileUploadPath;

    public String saveFile(
            @NonNull MultipartFile sourceFile,
            @NonNull Book book,
            @NonNull Integer userId) {

        final String fileUploadSubPath = "users" + File.separator + userId;
        return uploadFile(sourceFile,fileUploadSubPath);
    }

    private String uploadFile(MultipartFile sourceFile, String fileUploadSubPath) {
    }
}
