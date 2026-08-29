package com.barangay.cases.blotter.service;

import java.util.List;

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
 * Seamlessly resolves files from root case records when viewing reopened case cycles.
 */
@Service
@RequiredArgsConstructor
public class CaseFileService {

    private final CaseRepository caseRepository;
    private final CaseEvidenceRepository caseEvidenceRepository;
    private final WitnessRepository witnessRepository;

    /**
     * Retrieves the narrative statement attachment for a given case.
     * If the case is a reopened cycle without attached files, traverses back to the root case.
     *
     * @param caseId the case ID
     * @return {@link ResponseEntity} containing file byte array
     */
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getNarrativeFile(Long caseId) {
        Case aCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found"));

        Case targetCase = findCaseWithNarrative(aCase);
        if (targetCase == null || targetCase.getNarrativeStatement() == null) {
            return ResponseEntity.notFound().build();
        }
        byte[] bytes = targetCase.getNarrativeStatement().getFileData();
        String fileName = targetCase.getNarrativeStatement().getFileName() != null
                ? targetCase.getNarrativeStatement().getFileName() : "narrative.pdf";

        return buildFileResponse(bytes, fileName);
    }

    private Case findCaseWithNarrative(Case currentCase) {
        Case curr = currentCase;
        while (curr != null) {
            if (curr.getNarrativeStatement() != null &&
                curr.getNarrativeStatement().getFileData() != null &&
                curr.getNarrativeStatement().getFileData().length > 0) {
                return curr;
            }
            if (curr.getPreviousCaseNumber() != null && !curr.getPreviousCaseNumber().trim().isEmpty()) {
                curr = caseRepository.findByCaseNumber(curr.getPreviousCaseNumber().trim()).orElse(null);
            } else {
                break;
            }
        }
        return currentCase;
    }

    /**
     * Retrieves the evidence file for a given evidence entry.
     * If the evidence entry has no direct binary data (e.g. from reopened cycle), traverses back to the root case.
     *
     * @param evidenceId the evidence ID
     * @return {@link ResponseEntity} containing file byte array
     */
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getEvidenceFile(Long evidenceId) {
        CaseEvidence evidence = caseEvidenceRepository.findById(evidenceId).orElse(null);

        if (evidence == null) {
            // Check if evidenceId might be a Case ID where we take the first evidence with files
            Case aCase = caseRepository.findById(evidenceId).orElse(null);
            if (aCase != null) {
                List<CaseEvidence> caseEvidences = caseEvidenceRepository.findByCasesId(aCase.getId());
                if (caseEvidences != null && !caseEvidences.isEmpty()) {
                    evidence = caseEvidences.stream()
                            .filter(ce -> ce.getFiles() != null && ce.getFiles().length > 0)
                            .findFirst()
                            .orElse(caseEvidences.get(0));
                }
            }
        }

        if (evidence == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] bytes = evidence.getFiles();
        String fileName = evidence.getFileName() != null ? evidence.getFileName() : "evidence.png";

        if ((bytes == null || bytes.length == 0) && evidence.getCases() != null) {
            Case curr = evidence.getCases();
            while (curr != null && (bytes == null || bytes.length == 0)) {
                if (curr.getPreviousCaseNumber() != null && !curr.getPreviousCaseNumber().trim().isEmpty()) {
                    curr = caseRepository.findByCaseNumber(curr.getPreviousCaseNumber().trim()).orElse(null);
                    if (curr != null) {
                        List<CaseEvidence> parentEvidences = caseEvidenceRepository.findByCasesId(curr.getId());
                        for (CaseEvidence pe : parentEvidences) {
                            boolean typeMatches = false;
                            if (pe.getType() != null && evidence.getType() != null) {
                                if (pe.getType().getId() != null && pe.getType().getId().equals(evidence.getType().getId())) {
                                    typeMatches = true;
                                } else if (pe.getType().getName() != null && pe.getType().getName().equalsIgnoreCase(evidence.getType().getName())) {
                                    typeMatches = true;
                                }
                            }
                            if (typeMatches && pe.getFiles() != null && pe.getFiles().length > 0) {
                                bytes = pe.getFiles();
                                if (pe.getFileName() != null && !pe.getFileName().isBlank()) {
                                    fileName = pe.getFileName();
                                }
                                break;
                            }
                        }
                    }
                } else {
                    break;
                }
            }
        }

        if ((bytes == null || bytes.length == 0) && evidence.getType() != null && evidence.getType().getFile() != null) {
            bytes = evidence.getType().getFile();
        }

        return buildFileResponse(bytes, fileName);
    }

    /**
     * Retrieves the witness statement file for a given witness entry.
     * If the witness entry has no direct binary data (e.g. from reopened cycle), traverses back to the root case.
     *
     * @param witnessId the witness ID
     * @return {@link ResponseEntity} containing file byte array
     */
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getWitnessFile(Long witnessId) {
        Witness witness = witnessRepository.findById(witnessId).orElse(null);

        if (witness == null) {
            // Check if witnessId might be a Case ID where we take the first witness with testimony file
            Case aCase = caseRepository.findById(witnessId).orElse(null);
            if (aCase != null) {
                List<Witness> caseWitnesses = witnessRepository.findByCaseId(aCase.getId());
                if (caseWitnesses != null && !caseWitnesses.isEmpty()) {
                    witness = caseWitnesses.stream()
                            .filter(w -> w.getTestimonyFile() != null && w.getTestimonyFile().length > 0)
                            .findFirst()
                            .orElse(caseWitnesses.get(0));
                }
            }
        }

        if (witness == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] bytes = witness.getTestimonyFile();
        String fileName = witness.getTestimonyFileName() != null
                ? witness.getTestimonyFileName() : "witness_statement_" + witnessId + ".pdf";

        if ((bytes == null || bytes.length == 0) && witness.getACase() != null) {
            Case curr = witness.getACase();
            while (curr != null && (bytes == null || bytes.length == 0)) {
                if (curr.getPreviousCaseNumber() != null && !curr.getPreviousCaseNumber().trim().isEmpty()) {
                    curr = caseRepository.findByCaseNumber(curr.getPreviousCaseNumber().trim()).orElse(null);
                    if (curr != null) {
                        List<Witness> parentWitnesses = witnessRepository.findByCaseId(curr.getId());
                        for (Witness pw : parentWitnesses) {
                            if (pw.getTestimonyFile() != null && pw.getTestimonyFile().length > 0) {
                                if (witness.getPerson() != null && pw.getPerson() != null &&
                                    witness.getPerson().getId().equals(pw.getPerson().getId())) {
                                    bytes = pw.getTestimonyFile();
                                    if (pw.getTestimonyFileName() != null && !pw.getTestimonyFileName().isBlank()) {
                                        fileName = pw.getTestimonyFileName();
                                    }
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    break;
                }
            }
        }

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
