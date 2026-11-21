package com.barangay.clearance.service;

import com.barangay.clearance.dto.ClearanceStatsResponse;
import com.barangay.clearance.dto.IssuedCertificateTableResponse;
import com.barangay.clearance.model.CertificateTemplate;
import com.barangay.clearance.model.IssuedCertificate;
import com.barangay.clearance.repository.CertificateTemplateRepository;
import com.barangay.clearance.repository.IssuedCertificateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Unit tests for {@link ClearanceService}.
 */
@ExtendWith(MockitoExtension.class)
class ClearanceServiceTest {

    @Mock
    private CertificateTemplateRepository certificateTemplateRepository;

    @Mock
    private IssuedCertificateRepository issuedCertificateRepository;

    @InjectMocks
    private ClearanceService clearanceService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("getClearanceStats Method Tests")
    class GetClearanceStatsTests {

        /**
         * Verifies computation of dashboard KPI statistics excluding deleted and archived records.
         */
        @Test
        @DisplayName("shouldComputeAccurateStats_excludingDeletedAndArchived")
        void shouldComputeAccurateStats_excludingDeletedAndArchived() {
            // Given
            given(issuedCertificateRepository.countActiveCertificates()).willReturn(25L);
            given(issuedCertificateRepository.countActiveCertificatesBetween(any(), any())).willReturn(8L);

            IssuedCertificate c1 = new IssuedCertificate();
            CertificateTemplate t1 = new CertificateTemplate();
            t1.setCertTitle("BARANGAY CLEARANCE");
            c1.setTemplate(t1);
            c1.setFieldValues(objectMapper.createObjectNode().put("purpose", "Local Employment"));

            IssuedCertificate c2 = new IssuedCertificate();
            CertificateTemplate t2 = new CertificateTemplate();
            t2.setCertTitle("BARANGAY CLEARANCE");
            c2.setTemplate(t2);
            c2.setFieldValues(objectMapper.createObjectNode().put("purpose", "Local Employment"));

            IssuedCertificate c3 = new IssuedCertificate();
            CertificateTemplate t3 = new CertificateTemplate();
            t3.setCertTitle("INDIGENCY");
            c3.setTemplate(t3);
            c3.setFieldValues(objectMapper.createObjectNode().put("purpose", "Medical Assistance"));

            given(issuedCertificateRepository.findAllActiveCertificates()).willReturn(List.of(c1, c2, c3));

            // When
            ClearanceStatsResponse stats = clearanceService.getClearanceStats();

            // Then
            assertThat(stats.totalIssued()).isEqualTo(25L);
            assertThat(stats.issuedThisMonth()).isEqualTo(8L);
            assertThat(stats.mostIssuedCert()).isEqualTo("BARANGAY CLEARANCE");
            assertThat(stats.mostCommonPurpose()).isEqualTo("Local Employment");
        }
    }

    @Nested
    @DisplayName("getIssuedCertificates Method Tests")
    class GetIssuedCertificatesTests {

        /**
         * Verifies filtering of issued certificates by search term, template title, and purpose.
         */
        @Test
        @DisplayName("shouldFilterIssuedCertificates_bySearchTemplateTitleAndPurpose")
        void shouldFilterIssuedCertificates_bySearchTemplateTitleAndPurpose() {
            // Given
            IssuedCertificate c1 = new IssuedCertificate();
            c1.setId(101L);
            c1.setCertNumber("BC-2026-00001");
            c1.setRequestorName("Jeremiah Madronio");
            CertificateTemplate t1 = new CertificateTemplate();
            t1.setId(1L);
            t1.setCertTitle("BARANGAY CLEARANCE");
            c1.setTemplate(t1);
            c1.setFieldValues(objectMapper.createObjectNode().put("purpose", "Local Employment"));

            given(issuedCertificateRepository.findActiveIssuedCertificates("je", "BARANGAY CLEARANCE"))
                    .willReturn(List.of(c1));

            // When
            List<IssuedCertificateTableResponse> result = clearanceService.getIssuedCertificates("je", "BARANGAY CLEARANCE", "Employment");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(101L);
            assertThat(result.get(0).certNumber()).isEqualTo("BC-2026-00001");
            assertThat(result.get(0).requestorName()).isEqualTo("Madronio, Jeremiah");
            assertThat(result.get(0).purpose()).isEqualTo("Local Employment");
        }

        /**
         * Verifies grouping of multiple certificates for a resident into a single row with history.
         */
        @Test
        @DisplayName("shouldGroupResidentCertificates_andKeepIssuanceHistory")
        void shouldGroupResidentCertificates_andKeepIssuanceHistory() {
            // Given
            com.barangay.people.person.model.Person person = new com.barangay.people.person.model.Person();
            person.setId(501L);

            IssuedCertificate cNew = new IssuedCertificate();
            cNew.setId(2L);
            cNew.setCertNumber("BC-2026-00002");
            cNew.setRequestorName("Jeremiah Madronio");
            cNew.setPerson(person);
            cNew.setCreatedAt(java.time.LocalDateTime.now());
            CertificateTemplate t1 = new CertificateTemplate();
            t1.setId(1L);
            t1.setCertTitle("BARANGAY CLEARANCE");
            cNew.setTemplate(t1);
            cNew.setFieldValues(objectMapper.createObjectNode().put("purpose", "Local Employment"));

            IssuedCertificate cOld = new IssuedCertificate();
            cOld.setId(1L);
            cOld.setCertNumber("BC-2026-00001");
            cOld.setRequestorName("Jeremiah Madronio");
            cOld.setPerson(person);
            cOld.setCreatedAt(java.time.LocalDateTime.now().minusMonths(6));
            cOld.setTemplate(t1);
            cOld.setFieldValues(objectMapper.createObjectNode().put("purpose", "Postal ID Application"));

            given(issuedCertificateRepository.findActiveIssuedCertificates(null, null))
                    .willReturn(List.of(cNew, cOld));

            // When
            List<IssuedCertificateTableResponse> result = clearanceService.getIssuedCertificates(null, null, null);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).certNumber()).isEqualTo("BC-2026-00002");
            assertThat(result.get(0).totalIssuances()).isEqualTo(2);
            assertThat(result.get(0).history()).hasSize(2);
            assertThat(result.get(0).history().get(0).certNumber()).isEqualTo("BC-2026-00002");
            assertThat(result.get(0).history().get(1).certNumber()).isEqualTo("BC-2026-00001");
        }
    }
}
