package com.barangay.cases.blotter.controller;

import com.barangay.cases.blotter.service.BlotterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Blotter Case Management.
 * Supports creating, listing, searching, status updating, and deleting
 * both Formal Complaint and For the Record cases.
 * All mutating operations are logged via @LogAudit.
 */
@RestController
@RequestMapping("/api/v1/blotter")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class BlotterController {

    private final BlotterService blotterService;


}
