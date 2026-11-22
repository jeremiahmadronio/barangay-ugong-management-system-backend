package com.barangay.contentmanagement.controller;

import com.barangay.contentmanagement.service.FileStorageService;
import com.barangay.contentmanagement.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for managing global system configurations and portal branding assets.
 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService systemSettingService;
    private final FileStorageService fileStorageService;

    /**
     * Retrieves all system settings accessible to the public portal.
     *
     * @return key-value map of configuration settings
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> getAllSettings() {
        return ResponseEntity.ok(systemSettingService.getAllSettings());
    }

    /**
     * Updates system configuration key-value pairs.
     *
     * @param newSettings updated configuration entries
     * @return saved configurations
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ROOT_ADMIN', 'SYSTEM_ADMIN') or hasAnyAuthority('ROLE_ADMIN', 'ROLE_ROOT_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, String>> updateSettings(@RequestBody Map<String, String> newSettings) {
        return ResponseEntity.ok(systemSettingService.updateSettings(newSettings));
    }

    /**
     * Uploads media assets such as portal logo and hero background images.
     *
     * @param file uploaded image file
     * @return response containing uploaded file URL
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'ROOT_ADMIN', 'SYSTEM_ADMIN') or hasAnyAuthority('ROLE_ADMIN', 'ROLE_ROOT_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String fileUrl = fileStorageService.storeFile(file);

        Map<String, String> response = new HashMap<>();
        response.put("url", fileUrl);

        return ResponseEntity.ok(response);
    }
}
