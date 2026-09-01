package com.barangay.clearance.service;

import com.barangay.clearance.repository.CertificateTemplateRepository;
import com.barangay.clearance.repository.IssuedCertificateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClearanceService {

    private final CertificateTemplateRepository certificateTemplateRepository;
    private final IssuedCertificateRepository issuedCertificateRepository;


    
}
