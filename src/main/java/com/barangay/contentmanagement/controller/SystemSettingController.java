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

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SystemSettingController {

    private final SystemSettingService systemSettingService;
    private final FileStorageService fileStorageService;

    // Publicly accessible so the landing page can fetch settings without auth
    @GetMapping
    public ResponseEntity<Map<String, String>> getAllSettings() {
        return ResponseEntity.ok(systemSettingService.getAllSettings());
    }

    // Secure endpoint for admin to update settings
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ROOT_ADMIN', 'SYSTEM_ADMIN') or hasAnyAuthority('ROLE_ADMIN', 'ROLE_ROOT_ADMIN', 'ROLE_SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, String>> updateSettings(@RequestBody Map<String, String> newSettings) {
        return ResponseEntity.ok(systemSettingService.updateSettings(newSettings));
    }

    // Secure endpoint for admin to upload images (logos, hero bg, etc.)
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
