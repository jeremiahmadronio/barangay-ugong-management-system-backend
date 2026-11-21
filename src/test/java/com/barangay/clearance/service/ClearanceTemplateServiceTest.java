package com.barangay.clearance.service;

import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.IncidentDetail;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.clearance.dto.*;
import com.barangay.clearance.model.CertificateTemplate;
import com.barangay.clearance.model.IssuedCertificate;
import com.barangay.clearance.repository.CertificateTemplateRepository;
import com.barangay.clearance.repository.IssuedCertificateRepository;
import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.ClearanceStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

/**
 * Unit tests for {@link ClearanceTemplateService}.
 */
@ExtendWith(MockitoExtension.class)
class ClearanceTemplateServiceTest {

    @Mock
    private CertificateTemplateRepository certificateTemplateRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private IssuedCertificateRepository issuedCertificateRepository;

    @Mock
    private CaseRepository caseRepository;

    @InjectMocks
    private ClearanceTemplateService clearanceTemplateService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("getTemplateOptions Method Tests")
    class GetTemplateOptionsTests {

        /**
         * Verifies retrieval of active certificate template options.
         */
        @Test
        @DisplayName("shouldReturnActiveTemplateOptions_whenActiveTemplatesExist")
        void shouldReturnActiveTemplateOptions_whenActiveTemplatesExist() {
            // Given
            CertificateTemplate t1 = new CertificateTemplate();
            t1.setId(1L);
            t1.setCertTitle("BARANGAY CLEARANCE");

            CertificateTemplate t2 = new CertificateTemplate();
            t2.setId(2L);
            t2.setCertTitle("BUILDING CLEARANCE");

            given(certificateTemplateRepository.findAllByIsArchivedFalseAndIsDeletedFalse())
                    .willReturn(List.of(t1, t2));

            // When
            List<TemplateOptionResponse> results = clearanceTemplateService.getTemplateOptions();

            // Then
            assertThat(results).hasSize(2);
            assertThat(results.get(0).certTitle()).isEqualTo("BARANGAY CLEARANCE");
            assertThat(results.get(1).certTitle()).isEqualTo("BUILDING CLEARANCE");
        }
    }

    @Nested
    @DisplayName("getTemplateById Method Tests")
    class GetTemplateByIdTests {

        /**
         * Verifies retrieval of template details when template exists.
         */
        @Test
        @DisplayName("shouldReturnTemplateDetails_whenTemplateFound")
        void shouldReturnTemplateDetails_whenTemplateFound() {
            // Given
            CertificateTemplate t = new CertificateTemplate();
            t.setId(1L);
            t.setCertTitle("BARANGAY CLEARANCE");
            t.setValidityMonths(6);
            t.setRequiresPhoto(true);
            t.setRequiresThumbmark(true);
            t.setIsDeleted(false);

            given(certificateTemplateRepository.findById(1L)).willReturn(Optional.of(t));

            // When
            TemplateDetailResponse response = clearanceTemplateService.getTemplateById(1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.certTitle()).isEqualTo("BARANGAY CLEARANCE");
            assertThat(response.validityMonths()).isEqualTo(6);
            assertThat(response.requiresPhoto()).isTrue();
        }

        /**
         * Verifies 404 response when template is not found.
         */
        @Test
        @DisplayName("shouldThrowResponseStatusException_whenTemplateNotFound")
        void shouldThrowResponseStatusException_whenTemplateNotFound() {
            // Given
            given(certificateTemplateRepository.findById(99L)).willReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> clearanceTemplateService.getTemplateById(99L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Certificate template not found");
        }
    }

    @Nested
    @DisplayName("searchResidents Method Tests")
    class SearchResidentsTests {

        /**
         * Verifies empty result when search query is blank or shorter than minimum requirement.
         */
        @Test
        @DisplayName("shouldReturnEmptyList_whenQueryIsShortOrBlank")
        void shouldReturnEmptyList_whenQueryIsShortOrBlank() {
            // When / Then
            assertThat(clearanceTemplateService.searchResidents(null)).isEmpty();
            assertThat(clearanceTemplateService.searchResidents(" ")).isEmpty();
            assertThat(clearanceTemplateService.searchResidents("a")).isEmpty();
        }

        /**
         * Verifies resident clearance profile retrieval including past clearance issuance and case histories.
         */
        @Test
        @DisplayName("shouldReturnResidentClearanceProfile_withCaseAndIssuanceHistory")
        void shouldReturnResidentClearanceProfile_withCaseAndIssuanceHistory() {
            // Given
            Person person = new Person();
            person.setId(10L);
            person.setFirstName("Juan");
            person.setLastName("Dela Cruz");
            person.setCompleteAddress("123 F. Legaspi St., Ugong, Valenzuela City");
            person.setBirthDate(LocalDate.of(1995, 5, 20));

            Resident resident = new Resident();
            resident.setResidentId(10L);
            resident.setPerson(person);
            resident.setDateOfResidency(LocalDate.of(2015, 1, 1));

            given(residentRepository.searchActiveResidents("Juan")).willReturn(List.of(resident));

            IssuedCertificate cert = new IssuedCertificate();
            cert.setCertNumber("BC-2026-00001");
            CertificateTemplate tpl = new CertificateTemplate();
            tpl.setCertTitle("BARANGAY CLEARANCE");
            cert.setTemplate(tpl);
            cert.setStatus(ClearanceStatus.ACTIVE);
            cert.setCreatedAt(LocalDateTime.now());

            given(issuedCertificateRepository.findActiveCertificatesByPersonOrName(eq(10L), eq("Juan Dela Cruz"))).willReturn(List.of(cert));

            Case c = new Case();
            c.setId(101L);
            c.setCaseNumber("BLOTTER-2026-001");
            c.setCaseStatus(CaseStatus.SETTLED);
            c.setCreatedAt(LocalDateTime.now());

            Complainant comp = new Complainant();
            comp.setPerson(person);
            c.setComplainant(comp);

            IncidentDetail detail = new IncidentDetail();
            detail.setNatureOfComplaint("Neighborhood Dispute");
            c.setIncidentDetail(detail);

            given(caseRepository.findByPersonInCaseRoles(eq(10L), eq("Juan"), eq("Dela Cruz"))).willReturn(List.of(c));

            // When
            List<ClearanceResidentSearchResponse> results = clearanceTemplateService.searchResidents("Juan");

            // Then
            assertThat(results).hasSize(1);
            ClearanceResidentSearchResponse profile = results.get(0);
            assertThat(profile.fullName()).isEqualTo("Juan Dela Cruz");
            assertThat(profile.issuanceHistory()).hasSize(1);
            assertThat(profile.issuanceHistory().get(0).certNumber()).isEqualTo("BC-2026-00001");
            assertThat(profile.caseHistory()).hasSize(1);
            assertThat(profile.caseHistory().get(0).role()).isEqualTo("COMPLAINANT");
            assertThat(profile.caseHistory().get(0).incidentNature()).isEqualTo("Neighborhood Dispute");
        }
    }

    @Nested
    @DisplayName("issueClearance Method Tests")
    class IssueClearanceTests {

        /**
         * Verifies error thrown when template ID is null.
         */
        @Test
        @DisplayName("shouldThrowResponseStatusException_whenTemplateIdIsNull")
        void shouldThrowResponseStatusException_whenTemplateIdIsNull() {
            // Given
            IssueClearanceRequest req = new IssueClearanceRequest(
                    null, 1L, "Juan Dela Cruz", null, "1234567", "12345678", "Job application"
            );

            // When / Then
            assertThatThrownBy(() -> clearanceTemplateService.issueClearance(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Template ID is required");
        }

        /**
         * Verifies successful issuance of a clearance certificate and generation of sequential number.
         */
        @Test
        @DisplayName("shouldSuccessfullyIssueClearance_andGenerateSequentialNumber")
        void shouldSuccessfullyIssueClearance_andGenerateSequentialNumber() {
            // Given
            CertificateTemplate tpl = new CertificateTemplate();
            tpl.setId(1L);
            tpl.setCertTitle("BUILDING CLEARANCE");
            tpl.setValidityMonths(6);
            tpl.setIsDeleted(false);

            given(certificateTemplateRepository.findById(1L)).willReturn(Optional.of(tpl));

            Person person = new Person();
            person.setId(5L);
            person.setFirstName("Maria");
            person.setLastName("Santos");

            given(personRepository.findById(5L)).willReturn(Optional.of(person));
            given(issuedCertificateRepository.count()).willReturn(0L);
            given(issuedCertificateRepository.existsByCertNumber(any())).willReturn(false);

            IssuedCertificate savedCert = new IssuedCertificate();
            savedCert.setId(100L);
            savedCert.setCertNumber("BC-2026-00001");

            given(issuedCertificateRepository.save(any(IssuedCertificate.class))).willReturn(savedCert);

            IssueClearanceRequest req = new IssueClearanceRequest(
                    1L,
                    5L,
                    "Maria Santos",
                    objectMapper.createObjectNode().put("typeOfApplication", "New Construction"),
                    "9824102",
                    "10294812",
                    "Building Permit"
            );

            // When
            IssueClearanceResponse response = clearanceTemplateService.issueClearance(req);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();
            assertThat(response.certificateNumber()).isEqualTo("BC-2026-00001");
            assertThat(response.issuedId()).isEqualTo(100L);

            then(issuedCertificateRepository).should(times(1)).save(any(IssuedCertificate.class));
        }
    }
}
