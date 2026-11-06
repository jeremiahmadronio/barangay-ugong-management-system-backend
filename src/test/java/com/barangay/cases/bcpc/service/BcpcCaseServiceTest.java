package com.barangay.cases.bcpc.service;

import com.barangay.cases.bcpc.dto.BcpcCarCaseCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcCarCaseDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseCreateRequest;
import com.barangay.cases.bcpc.dto.BcpcCiclCaseDetailResponse;
import com.barangay.cases.bcpc.dto.BcpcPersonInputDto;
import com.barangay.cases.bcpc.model.BcpcCarAssessment;
import com.barangay.cases.bcpc.model.BcpcCaseDetails;
import com.barangay.cases.bcpc.model.BcpcCiclAssessment;
import com.barangay.cases.bcpc.repository.BcpcCarAssessmentRepository;
import com.barangay.cases.bcpc.repository.BcpcCaseDetailsRepository;
import com.barangay.cases.bcpc.repository.BcpcCiclAssessmentRepository;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.IncidentDetail;
import com.barangay.cases.shared.model.NarrativeStatement;
import com.barangay.cases.shared.repository.CaseEvidenceRepository;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.repository.EvidenceTypeRepository;
import com.barangay.cases.shared.service.CaseTimelineService;
import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.people.witness.repository.WitnessRepository;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.zshared.dto.ComplainantDetailDto;
import com.barangay.zshared.dto.IncidentCreateDto;
import com.barangay.zshared.dto.IncidentDetailDto;
import com.barangay.zshared.dto.NarrativeCreateDto;
import com.barangay.zshared.dto.NarrativeDetailDto;
import com.barangay.zshared.enumerated.CarActCategory;
import com.barangay.zshared.enumerated.CarOccurrenceCategory;
import com.barangay.zshared.enumerated.CiclPathway;
import com.barangay.zshared.enumerated.DiscernmentStatus;
import com.barangay.zshared.enumerated.DiversionAuthority;
import com.barangay.zshared.enumerated.PenaltyRoutingCategory;
import com.barangay.zshared.enumerated.SpecificCarAct;
import com.barangay.zshared.enumerated.TimelineEventType;
import com.barangay.zshared.mapper.CaseEvidenceMapper;
import com.barangay.zshared.mapper.ComplainantMapper;
import com.barangay.zshared.mapper.IncidentDetailMapper;
import com.barangay.zshared.mapper.NarrativeStatementMapper;
import com.barangay.zshared.mapper.PersonMapper;
import com.barangay.zshared.mapper.WitnessMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BcpcCaseServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private BcpcCaseDetailsRepository bcpcCaseDetailsRepository;

    @Mock
    private BcpcCarAssessmentRepository bcpcCarAssessmentRepository;

    @Mock
    private BcpcCiclAssessmentRepository bcpcCiclAssessmentRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private WitnessRepository witnessRepository;

    @Mock
    private EvidenceTypeRepository evidenceTypeRepository;

    @Mock
    private CaseEvidenceRepository caseEvidenceRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private OfficerRepository officerRepository;

    @Mock
    private CaseTimelineService caseTimelineService;

    @Mock
    private PersonMapper personMapper;

    @Mock
    private ComplainantMapper complainantMapper;

    @Mock
    private IncidentDetailMapper incidentDetailMapper;

    @Mock
    private NarrativeStatementMapper narrativeStatementMapper;

    @Mock
    private WitnessMapper witnessMapper;

    @Mock
    private CaseEvidenceMapper caseEvidenceMapper;

    @InjectMocks
    private BcpcCaseService bcpcCaseService;

    private Person mockChild;
    private Person mockVictim;

    @BeforeEach
    void setUp() {
        mockChild = new Person();
        mockChild.setId(10L);
        mockChild.setFirstName("Juan");
        mockChild.setLastName("Dela Cruz");
        mockChild.setBirthDate(LocalDate.now().minusYears(14));
        mockChild.setAge((short) 14);

        mockVictim = new Person();
        mockVictim.setId(20L);
        mockVictim.setFirstName("Maria");
        mockVictim.setLastName("Santos");
    }

    @Nested
    @DisplayName("CICL (Child in Conflict with the Law) Tests")
    class CiclTests {

        @Test
        @DisplayName("Should successfully file CICL case and auto-route child <= 15 to INTERVENTION")
        void createCiclCase_minorUnder15_shouldForceIntervention() {
            // Arrange
            BcpcPersonInputDto childInput = new BcpcPersonInputDto(
                    null, 10L, "Juan", "Dela Cruz", null, null,
                    "09123456789", "Barangay Center", (short) 14, "MALE",
                    LocalDate.now().minusYears(14), "SINGLE", null
            );

            BcpcPersonInputDto victimInput = new BcpcPersonInputDto(
                    null, 20L, "Maria", "Santos", null, null,
                    "09987654321", "Street 1", (short) 35, "FEMALE",
                    LocalDate.now().minusYears(35), "MARRIED", null
            );

            IncidentCreateDto incident = new IncidentCreateDto(
                    "Theft", LocalDate.now().toString(), "10:00", "Public Market", "First time", null, null
            );

            BcpcCiclCaseCreateRequest request = new BcpcCiclCaseCreateRequest(
                    "BU26-BCPC-0001", null, null, false, victimInput, "Victim", "Neighbor",
                    childInput, "Grade 8", "National High School",
                    false, null, null, "Parent deceased",
                    incident, "Petty Theft",
                    14, CiclPathway.DIVERSION, DiscernmentStatus.WITH_DISCERNMENT,
                    PenaltyRoutingCategory.SIX_YEARS_OR_LESS, DiversionAuthority.BARANGAY,
                    true, true, false,
                    "Officer Tanod", "Barangay Police", "09111111111", null, "Market Alley",
                    null, Collections.emptyList(), Collections.emptyList(), true
            );

            when(personRepository.findById(10L)).thenReturn(Optional.of(mockChild));
            when(personRepository.findById(20L)).thenReturn(Optional.of(mockVictim));
            when(departmentRepository.findByName("BCPC")).thenReturn(Optional.of(new Department()));
            when(caseRepository.existsByCaseNumber("BU26-BCPC-0001")).thenReturn(false);

            when(incidentDetailMapper.fromCreateDto(any())).thenReturn(new IncidentDetail());
            when(caseRepository.save(any(Case.class))).thenAnswer(invocation -> {
                Case c = invocation.getArgument(0);
                c.setId(100L);
                return c;
            });
            when(bcpcCaseDetailsRepository.save(any(BcpcCaseDetails.class))).thenAnswer(invocation -> {
                BcpcCaseDetails d = invocation.getArgument(0);
                d.setId(200L);
                return d;
            });
            when(bcpcCiclAssessmentRepository.save(any(BcpcCiclAssessment.class))).thenAnswer(invocation -> {
                BcpcCiclAssessment a = invocation.getArgument(0);
                a.setId(300L);
                return a;
            });

            when(personMapper.buildFullName(mockChild)).thenReturn("Juan Dela Cruz");

            // Act
            BcpcCiclCaseDetailResponse response = bcpcCaseService.createCiclCase(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.caseId()).isEqualTo(100L);
            assertThat(response.caseNumber()).isEqualTo("BU26-BCPC-0001");
            assertThat(response.ciclAssessment()).isNotNull();

            // Legal verification under RA 9344 / RA 10630: 14 y/o is exempt from criminal liability
            assertThat(response.ciclAssessment().ciclPathway()).isEqualTo(CiclPathway.INTERVENTION);
            assertThat(response.ciclAssessment().isDiversionEligible()).isFalse();
            assertThat(response.ciclAssessment().discernmentStatus()).isEqualTo(DiscernmentStatus.WITHOUT_DISCERNMENT);

            verify(caseTimelineService).logEvent(
                    any(Case.class),
                    eq(TimelineEventType.CASE_FILED),
                    eq("BCPC Case Filed (CICL)"),
                    any(),
                    any()
            );
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when child age is 18 or older")
        void createCiclCase_adultAge_shouldThrowException() {
            // Arrange
            Person adultPerson = new Person();
            adultPerson.setId(99L);
            adultPerson.setFirstName("Pedro");
            adultPerson.setLastName("Penduko");
            adultPerson.setAge((short) 19);

            BcpcPersonInputDto adultInput = new BcpcPersonInputDto(
                    null, 99L, "Pedro", "Penduko", null, null,
                    null, null, (short) 19, "MALE", null, null, null
            );

            IncidentCreateDto incident = new IncidentCreateDto(
                    "Robbery", LocalDate.now().toString(), "10:00", "Street", "Once", null, null
            );

            BcpcCiclCaseCreateRequest request = new BcpcCiclCaseCreateRequest(
                    null, null, null, false, null, null, null,
                    adultInput, null, null, false, null, null, null,
                    incident, "Robbery", 19, null, null, null, null,
                    null, null, null, null, null, null, null, null,
                    null, null, null, true
            );

            when(personRepository.findById(99L)).thenReturn(Optional.of(adultPerson));

            // Act & Assert
            assertThatThrownBy(() -> bcpcCaseService.createCiclCase(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be processed as a Child in Conflict with the Law (CICL)");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when victim and child are the same person")
        void createCiclCase_victimSameAsChild_shouldThrowException() {
            // Arrange
            BcpcPersonInputDto samePersonInput = new BcpcPersonInputDto(
                    null, 10L, "Juan", "Dela Cruz", null, null,
                    null, null, (short) 14, "MALE", null, null, null
            );

            IncidentCreateDto incident = new IncidentCreateDto(
                    "Theft", LocalDate.now().toString(), "10:00", "Street", "Once", null, null
            );

            BcpcCiclCaseCreateRequest request = new BcpcCiclCaseCreateRequest(
                    null, null, null, false, samePersonInput, null, null,
                    samePersonInput, null, null, false, null, null, null,
                    incident, "Theft", 14, null, null, null, null,
                    null, null, null, null, null, null, null, null,
                    null, null, null, true
            );

            when(personRepository.findById(10L)).thenReturn(Optional.of(mockChild));

            // Act & Assert
            assertThatThrownBy(() -> bcpcCaseService.createCiclCase(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot be the same resident");
        }
    }

    @Nested
    @DisplayName("CAR (Child at Risk) Tests")
    class CarTests {

        @Test
        @DisplayName("Should successfully file CAR case")
        void createCarCase_validRequest_success() {
            // Arrange
            BcpcPersonInputDto reporterInput = new BcpcPersonInputDto(
                    null, 20L, "Maria", "Santos", null, null,
                    "09987654321", "Street 1", (short) 35, "FEMALE",
                    null, null, null
            );

            BcpcPersonInputDto childInput = new BcpcPersonInputDto(
                    null, 10L, "Juan", "Dela Cruz", null, null,
                    "09123456789", "Street 2", (short) 12, "MALE",
                    null, null, null
            );

            IncidentCreateDto incident = new IncidentCreateDto(
                    "Curfew Violation", LocalDate.now().toString(), "23:00", "Park", "Habitual", null, null
            );

            BcpcCarCaseCreateRequest request = new BcpcCarCaseCreateRequest(
                    "BU26-BCPC-0002", null, null,
                    reporterInput, "Barangay Tanod", "Officer",
                    childInput, "Grade 6", "Elementary School",
                    true, null, "Mother", null,
                    incident, CarActCategory.JUVENILE_STATUS_OFFENSE, SpecificCarAct.CURFEW_VIOLATION,
                    null, CarOccurrenceCategory.SECOND_OCCURRENCE, false, "No immediate danger", "LOW",
                    null, Collections.emptyList(), Collections.emptyList(), true
            );

            when(personRepository.findById(20L)).thenReturn(Optional.of(mockVictim));
            when(personRepository.findById(10L)).thenReturn(Optional.of(mockChild));
            when(departmentRepository.findByName("BCPC")).thenReturn(Optional.of(new Department()));
            when(caseRepository.existsByCaseNumber("BU26-BCPC-0002")).thenReturn(false);

            when(incidentDetailMapper.fromCreateDto(any())).thenReturn(new IncidentDetail());
            when(caseRepository.save(any(Case.class))).thenAnswer(invocation -> {
                Case c = invocation.getArgument(0);
                c.setId(101L);
                return c;
            });
            when(bcpcCaseDetailsRepository.save(any(BcpcCaseDetails.class))).thenAnswer(invocation -> {
                BcpcCaseDetails d = invocation.getArgument(0);
                d.setId(201L);
                return d;
            });
            when(bcpcCarAssessmentRepository.save(any(BcpcCarAssessment.class))).thenAnswer(invocation -> {
                BcpcCarAssessment a = invocation.getArgument(0);
                a.setId(301L);
                return a;
            });

            when(personMapper.buildFullName(mockChild)).thenReturn("Juan Dela Cruz");

            // Act
            BcpcCarCaseDetailResponse response = bcpcCaseService.createCarCase(request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.caseId()).isEqualTo(101L);
            assertThat(response.caseNumber()).isEqualTo("BU26-BCPC-0002");
            assertThat(response.carActCategory()).isEqualTo(CarActCategory.JUVENILE_STATUS_OFFENSE);
            assertThat(response.specificCarAct()).isEqualTo(SpecificCarAct.CURFEW_VIOLATION);

            verify(caseTimelineService).logEvent(
                    any(Case.class),
                    eq(TimelineEventType.CASE_FILED),
                    eq("BCPC Case Filed (CAR)"),
                    any(),
                    any()
            );
        }
    }
}
