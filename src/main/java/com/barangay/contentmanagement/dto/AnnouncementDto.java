package com.barangay.contentmanagement.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class AnnouncementDto {

    @NotBlank(message = "Category is required")
    private String category;

    private String categoryColor;

    @NotBlank(message = "Title is required")
    private String title;

    private String excerpt;
    private String author;
    private String content;

    // Main cover image upload (optional for updates)
    private MultipartFile image;

    private boolean removeImage;
    private boolean clearAdditionalImages;

    // Additional images already uploaded (kept from before, if any)
    private List<String> additionalImages;

    // New files to upload for additional images
    private List<MultipartFile> additionalImageFiles;
}
