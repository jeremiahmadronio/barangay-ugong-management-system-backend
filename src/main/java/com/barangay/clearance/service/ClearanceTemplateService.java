package com.barangay.clearance.service;

import com.barangay.clearance.dto.TemplateOptionResponse;
import com.barangay.clearance.model.CertificateTemplate;
import com.barangay.clearance.repository.CertificateTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service handling clearance and certificate template operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClearanceTemplateService {

    private final CertificateTemplateRepository certificateTemplateRepository;

    /**
     * Retrieves lightweight document options (id and certTitle) for active templates.
     * Used for populating the Document Type selection grid and dropdown menus.
     *
     * @return List of {@link TemplateOptionResponse}
     */
    @Transactional(readOnly = true)
    public List<TemplateOptionResponse> getTemplateOptions() {
        return certificateTemplateRepository.findAllByIsArchivedFalseAndIsDeletedFalse()
                .stream()
                .map(t -> new TemplateOptionResponse(t.getId(), t.getCertTitle()))
                .toList();
    }
}
