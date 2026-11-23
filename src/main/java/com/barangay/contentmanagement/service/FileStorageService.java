package com.barangay.contentmanagement.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path fileStorageLocation;
    private final Path secureFileStorageLocation;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".webp", ".gif", ".pdf", ".docx", ".xlsx", ".txt"
    );

    public FileStorageService(@Value("${upload.path:uploads}") String uploadPath,
                              @Value("${secure.upload.path:secure_uploads}") String secureUploadPath) {
        this.fileStorageLocation = Paths.get(uploadPath).toAbsolutePath().normalize();
        this.secureFileStorageLocation = Paths.get(secureUploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            Files.createDirectories(this.secureFileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        return storeFileToPath(file, this.fileStorageLocation, "/uploads/");
    }

    public String storeSecureFile(MultipartFile file) {
        return storeFileToPath(file, this.secureFileStorageLocation, "/api/secure-files/");
    }

    private String storeFileToPath(MultipartFile file, Path targetDir, String urlPrefix) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        String fileExtension = "";

        try {
            if (originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase(Locale.ROOT);
            }

            if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
                throw new IllegalArgumentException("File type " + fileExtension + " is not allowed. Only safe documents and images are permitted.");
            }

            String fileName = UUID.randomUUID().toString() + fileExtension;

            if (fileName.contains("..")) {
                throw new RuntimeException("Filename contains invalid path sequence " + fileName);
            }

            Path targetLocation = targetDir.resolve(fileName).normalize();
            if (!targetLocation.startsWith(targetDir)) {
                throw new SecurityException("Cannot store file outside current directory: " + fileName);
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return urlPrefix + fileName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + originalFileName + ". Please try again!", ex);
        }
    }
}
