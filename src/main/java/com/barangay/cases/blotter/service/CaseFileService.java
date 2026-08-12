package com.barangay.cases.blotter.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.CaseEvidence;
import com.barangay.cases.shared.repository.CaseEvidenceRepository;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.people.witness.model.Witness;
import com.barangay.people.witness.repository.WitnessRepository;
import com.barangay.zshared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

/**
 * Service for retrieving binary files (narratives, evidence, witness statements) for Blotter cases.
 */
@Service
@RequiredArgsConstructor
public class CaseFileService {

    private final CaseRepository caseRepository;
    private final CaseEvidenceRepository caseEvidenceRepository;
    private final WitnessRepository witnessRepository;

    /**
     * Retrieves the narrative statement attachment for a given case.
     *
     * @param caseId the case ID
     * @return {@link ResponseEntity} containing file byte array
     */
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getNarrativeFile(Long caseId) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found"));
        if (aCase.getNarrativeStatement() == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = aCase.getNarrativeStatement().getFileData();
        String fileName = aCase.getNarrativeStatement().getFileName() != null
                ? aCase.getNarrativeStatement().getFileName() : "narrative.pdf";

        return buildFileResponse(bytes, fileName);
    }

    /**
     * Retrieves the evidence file for a given evidence entry.
     *
     * @param evidenceId the evidence ID
     * @return {@link ResponseEntity} containing file byte array
     */
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getEvidenceFile(Long evidenceId) {
        CaseEvidence evidence = caseEvidenceRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found"));
        byte[] bytes = evidence.getFiles();
        String fileName = evidence.getFileName() != null ? evidence.getFileName() : "evidence.png";

        return buildFileResponse(bytes, fileName);
    }

    /**
     * Retrieves the witness statement file for a given witness entry.
     *
     * @param witnessId the witness ID
     * @return {@link ResponseEntity} containing file byte array
     */
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getWitnessFile(Long witnessId) {
        Witness witness = witnessRepository.findById(witnessId)
                .orElseThrow(() -> new ResourceNotFoundException("Witness not found"));
        byte[] bytes = witness.getTestimonyFile();
        String fileName = witness.getTestimonyFileName() != null
                ? witness.getTestimonyFileName() : "witness_statement_" + witnessId + ".pdf";

        return buildFileResponse(bytes, fileName);
    }

    /**
     * Shared helper to construct file download/inline responses with content-disposition and media type headers.
     */
    private ResponseEntity<byte[]> buildFileResponse(byte[] data, String fileName) {
        if (data == null || data.length == 0) {
            return ResponseEntity.notFound().build();
        }
        String name = (fileName != null && !fileName.isBlank()) ? fileName : "file";
        MediaType mediaType = detectMediaTypeFromBytes(data, name);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + name + "\"")
                .contentType(mediaType)
                .body(data);
    }

    private MediaType determineMediaType(String fileName) {
        if (fileName == null) return MediaType.APPLICATION_OCTET_STREAM;
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (lower.endsWith(".svg")) return MediaType.parseMediaType("image/svg+xml");
        if (lower.endsWith(".txt")) return MediaType.TEXT_PLAIN;
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return MediaType.TEXT_HTML;
        if (lower.endsWith(".mp4")) return MediaType.parseMediaType("video/mp4");
        if (lower.endsWith(".mp3")) return MediaType.parseMediaType("audio/mpeg");
        if (lower.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private MediaType detectMediaTypeFromBytes(byte[] bytes, String defaultFileName) {
        if (bytes != null && bytes.length >= 4) {
            // PDF Magic Number: %PDF (0x25 0x50 0x44 0x46)
            if (bytes[0] == 0x25 && bytes[1] == 0x50 && bytes[2] == 0x44 && bytes[3] == 0x46) {
                return MediaType.APPLICATION_PDF;
            }
            // PNG Magic Number: 0x89 PNG
            if ((bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47) {
                return MediaType.IMAGE_PNG;
            }
            // JPEG Magic Number: 0xFF 0xD8 0xFF
            if ((bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
                return MediaType.IMAGE_JPEG;
            }
            // GIF Magic Number: GIF8
            if (bytes[0] == 0x47 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x38) {
                return MediaType.IMAGE_GIF;
            }
        }
        return determineMediaType(defaultFileName);
    }
}
