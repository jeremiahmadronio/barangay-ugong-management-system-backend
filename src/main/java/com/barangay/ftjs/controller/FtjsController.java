package com.barangay.ftjs.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.barangay.ftjs.dto.FtjsResidentSearchResponse;
import com.barangay.ftjs.service.FtjsService;

import lombok.RequiredArgsConstructor;

/**
 * REST Controller for FTJS Resident Search, returning unified Resident Information,
 * FTJS Issuance History, and Barangay Case History.
 */
@RestController
@RequestMapping("/api/v1/ftjs")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class FtjsController {

    private final FtjsService ftjsService;

    /**
     * Search residents by name/query for FTJS verification.
     * Returns list of FtjsResidentSearchResponse objects containing demographics, issuance history, and case history.
     */
    @GetMapping("/search-resident")
    public ResponseEntity<List<FtjsResidentSearchResponse>> searchResidents(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String search) {
        String searchTerm = (query != null && !query.isBlank()) ? query : search;
        return ResponseEntity.ok(ftjsService.searchResidents(searchTerm));
    }

    /**
     * Get FTJS resident response profile by resident ID.
     */
    @GetMapping("/resident/{residentId}")
    public ResponseEntity<FtjsResidentSearchResponse> getResidentFtjsProfile(@PathVariable Long residentId) {
        return ResponseEntity.ok(ftjsService.getResidentFtjsProfile(residentId));
    }
}
