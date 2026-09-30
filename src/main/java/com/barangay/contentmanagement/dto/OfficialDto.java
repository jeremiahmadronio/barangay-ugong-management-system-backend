package com.barangay.contentmanagement.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class OfficialDto {
    private String name;
    private String position;
    private String type;
    private MultipartFile image;
    private boolean removeImage;
}
