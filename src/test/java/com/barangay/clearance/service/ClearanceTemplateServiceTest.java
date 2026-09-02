package com.barangay.clearance.service;

import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.IncidentDetail;
import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.respondent.model.Respondent;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.clearance.dto.*;
import com.barangay.clearance.model.CertificateTemplate;
import com.barangay.clearance.model.IssuedCertificate;
import com.barangay.clearance.repository.CertificateTemplateRepository;
import com.barangay.clearance.repository.IssuedCertificateRepository;
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
import static org.mockito.Mockito.*;

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
    @DisplayName("getTemplateOptions Tests")
    class GetTemplateOptionsTests {

        @Test
        @DisplayName("should return active template options list")
        void shouldReturnActiveTemplateOptions() {
            CertificateTemplate t1 = new CertificateTemplate();
            t1.setId(1L);
            t1.setCertTitle("BARANGAY CLEARANCE");

            CertificateTemplate t2 = new CertificateTemplate();
            t2.setId(2L);
            t2.setCertTitle("BUILDING CLEARANCE");

            when(certificateTemplateRepository.findAllByIsArchivedFalseAndIsDeletedFalse())
                    .thenReturn(List.of(t1, t2));

            List<TemplateOptionResponse> results = clearanceTemplateService.getTemplateOptions();

            assertThat(results).hasSize(2);
            assertThat(results.get(0).certTitle()).isEqualTo("BARANGAY CLEARANCE");
            assertThat(results.get(1).certTitle()).isEqualTo("BUILDING CLEARANCE");
        }
    }

    @Nested
    @DisplayName("getTemplateById Tests")
    class GetTemplateByIdTests {

        @Test
        @DisplayName("should return template details when found")
        void shouldReturnTemplateDetails_whenFound() {
            CertificateTemplate t = new CertificateTemplate();
            t.setId(1L);
            t.setCertTitle("BARANGAY CLEARANCE");
            t.setValidityMonths(6);
            t.setRequiresPhoto(true);
            t.setRequiresThumbmark(true);
            t.setIsDeleted(false);

            when(certificateTemplateRepository.findById(1L)).thenReturn(Optional.of(t));

            TemplateDetailResponse response = clearanceTemplateService.getTemplateById(1L);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.certTitle()).isEqualTo("BARANGAY CLEARANCE");
            assertThat(response.validityMonths()).isEqualTo(6);
            assertThat(response.requiresPhoto()).isTrue();
        }

        @Test
        @DisplayName("should throw 404 when template not found or deleted")
        void shouldThrow404_whenNotFound() {
            when(certificateTemplateRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> clearanceTemplateService.getTemplateById(99L))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Certificate template not found");
        }
    }

    @Nested
    @DisplayName("searchResidents Tests")
    class SearchResidentsTests {

        @Test
        @DisplayName("should return empty list when query is blank or short")
        void shouldReturnEmpty_whenQueryIsShort() {
            assertThat(clearanceTemplateService.searchResidents(null)).isEmpty();
            assertThat(clearanceTemplateService.searchResidents(" ")).isEmpty();
            assertThat(clearanceTemplateService.searchResidents("a")).isEmpty();
        }

        @Test
        @DisplayName("should return resident clearance profile with case & issuance history")
        void shouldReturnResidentClearanceProfile() {
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

            when(residentRepository.searchActiveResidents("Juan")).thenReturn(List.of(resident));

            IssuedCertificate cert = new IssuedCertificate();
            cert.setCertNumber("BC-2026-00001");
            CertificateTemplate tpl = new CertificateTemplate();
            tpl.setCertTitle("BARANGAY CLEARANCE");
            cert.setTemplate(tpl);
            cert.setStatus(ClearanceStatus.ACTIVE);
            cert.setCreatedAt(LocalDateTime.now());

            when(issuedCertificateRepository.findActiveCertificatesByPersonOrName(eq(10L), eq("Juan Dela Cruz"))).thenReturn(List.of(cert));

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

            when(caseRepository.findByPersonInCaseRoles(eq(10L), eq("Juan"), eq("Dela Cruz"))).thenReturn(List.of(c));

            List<ClearanceResidentSearchResponse> results = clearanceTemplateService.searchResidents("Juan");

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
    @DisplayName("issueClearance Tests")
    class IssueClearanceTests {

        @Test
        @DisplayName("should throw error if templateId is null")
        void shouldThrowError_whenTemplateIdIsNull() {
            IssueClearanceRequest req = new IssueClearanceRequest(
                    null, 1L, "Juan Dela Cruz", null, "1234567", "12345678", "Job application"
            );

            assertThatThrownBy(() -> clearanceTemplateService.issueClearance(req))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Template ID is required");
        }

        @Test
        @DisplayName("should successfully issue clearance and generate sequential certificate number")
        void shouldSuccessfullyIssueClearance() {
            CertificateTemplate tpl = new CertificateTemplate();
            tpl.setId(1L);
            tpl.setCertTitle("BUILDING CLEARANCE");
            tpl.setValidityMonths(6);
            tpl.setIsDeleted(false);

            when(certificateTemplateRepository.findById(1L)).thenReturn(Optional.of(tpl));

            Person person = new Person();
            person.setId(5L);
            person.setFirstName("Maria");
            person.setLastName("Santos");

            when(personRepository.findById(5L)).thenReturn(Optional.of(person));
            when(issuedCertificateRepository.count()).thenReturn(0L);
            when(issuedCertificateRepository.existsByCertNumber(any())).thenReturn(false);

            IssuedCertificate savedCert = new IssuedCertificate();
            savedCert.setId(100L);
            savedCert.setCertNumber("BC-2026-00001");

            when(issuedCertificateRepository.save(any(IssuedCertificate.class))).thenReturn(savedCert);

            IssueClearanceRequest req = new IssueClearanceRequest(
                    1L,
                    5L,
                    "Maria Santos",
                    objectMapper.createObjectNode().put("typeOfApplication", "New Construction"),
                    "9824102",
                    "10294812",
                    "Building Permit"
            );

            IssueClearanceResponse response = clearanceTemplateService.issueClearance(req);

            assertThat(response).isNotNull();
            assertThat(response.success()).isTrue();
            assertThat(response.certificateNumber()).isEqualTo("BC-2026-00001");
            assertThat(response.issuedId()).isEqualTo(100L);

            verify(issuedCertificateRepository, times(1)).save(any(IssuedCertificate.class));
        }
    }
}
