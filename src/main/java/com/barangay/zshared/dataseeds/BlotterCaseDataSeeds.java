package com.barangay.zshared.dataseeds;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.CaseSession;
import com.barangay.cases.shared.model.IncidentDetail;
import com.barangay.cases.shared.model.NarrativeStatement;
import com.barangay.cases.shared.repository.CaseRepository;
import com.barangay.cases.shared.repository.CaseSessionRepository;
import com.barangay.people.complainant.model.Complainant;
import com.barangay.people.officer.model.Officer;
import com.barangay.people.officer.repository.OfficerRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.person.repository.PersonRepository;
import com.barangay.people.respondent.model.Respondent;
import com.barangay.usermanagement.department.model.Department;
import com.barangay.usermanagement.department.repository.DepartmentRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.enumerated.CaseStatus;
import com.barangay.zshared.enumerated.FilingType;
import com.barangay.zshared.enumerated.SessionStatus;
import com.barangay.zshared.enumerated.SessionType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * BlotterCaseDataSeeds.java
 *
 * Bootstraps initial historical Blotter Cases (Formal Complaints and For-The-Record entries)
 * spread realistically across the past 4 months for trend & report testing.
 * Directly links existing registered Persons/Residents as distinct Complainants and Respondents,
 * ensures all cases are root/initial cases (no reopened child cases),
 * schedules mediation hearings for any case under mediation,
 * and strictly assigns Blotter-department Officers and Blotter-department Users.
 * Also contains a midnight daily scheduler to continuously generate random daily cases.
 */
@Slf4j
@Component
@Profile("!test")
@Order(11)
@RequiredArgsConstructor
public class BlotterCaseDataSeeds implements CommandLineRunner {

    private final CaseRepository caseRepository;
    private final CaseSessionRepository caseSessionRepository;
    private final PersonRepository personRepository;
    private final DepartmentRepository departmentRepository;
    private final OfficerRepository officerRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final byte[] DUMMY_PDF = "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj\n3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R>>endobj\nxref\n0 4\n0000000000 65535 f \n0000000009 00000 n \n0000000052 00000 n \n0000000101 00000 n \ntrailer<</Size 4/Root 1 0 R>>\nstartxref\n178\n%%EOF".getBytes(StandardCharsets.UTF_8);

    private static final String[] COMPLAINT_NATURES = {
            "Public Disturbance / Unreasonable Noise",
            "Unsettled Debt / Financial Obligation",
            "Boundary & Property Encroachment",
            "Physical Altercation / Mutual Quarrel",
            "Property Damage & Vandalism",
            "Verbal Harassment / Slander / Oral Defamation",
            "Lease & Tenancy Non-Payment Dispute",
            "Pet Nuisance / Animal Damage Dispute",
            "Right of Way & Easement Blockage",
            "Barangay Ordinance Violation / Curfew"
    };

    private static final String[] RECORD_NATURES = {
            "Lost Barangay Clearance / ID Documentation",
            "Mutual Agreement / Boundary Demarcation Note",
            "Incident For Information & Record Purpose",
            "Unreturned Borrowed Personal Property",
            "Neighborhood Notice of Construction Activity"
    };

    private static final String[] LOCATIONS = {
            "Purok 1, Near Barangay Hall",
            "Sitio Riverside, Zone 2",
            "Mabini St. corner Rizal Ave.",
            "Purok 3, Commercial Area",
            "Diamond St., Greenhills Subdivision",
            "Purok 4, Near Public Market",
            "Block 5 Lot 12, Sunrise Village",
            "Purok 6, Purok Center Grounds"
    };

    private static final String[] RELATIONSHIPS = {
            "Neighbor", "Acquaintance / Co-resident", "Landlord / Lessor",
            "Tenant / Lessee", "Business Associate", "Community Member"
    };

    @Override
    @Transactional
    public void run(String... args) {
        long rootCount = caseRepository.countRootBlotterCases();
        log.info("[BlotterCaseDataSeeds] Existing root blotter cases: {}", rootCount);

        if (rootCount < 40) {
            log.info("[BlotterCaseDataSeeds] Seeding initial historical Blotter cases across the last 4 months...");
            seedHistoricalCases(52);
            log.info("[BlotterCaseDataSeeds] Historical Blotter cases seeded successfully!");
        } else {
            log.info("[BlotterCaseDataSeeds] Database already populated with blotter cases. Skipping initial seed.");
        }
    }

    /**
     * Daily automated scheduler running every midnight (12:00 AM).
     * Generates 1 to 5 new random cases for the current date.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void scheduleDailyCaseGeneration() {
        log.info("[BlotterCaseDataSeeds] Midnight daily case generator triggered.");
        Random rnd = new Random();
        int formalCount = 1 + rnd.nextInt(3); // 1 to 3
        int recordCount = 1 + rnd.nextInt(2); // 1 to 2
        LocalDate today = LocalDate.now();

        List<Officer> blotterOfficers = getBlotterOfficers();
        List<User> blotterUsers = getBlotterUsers();
        List<Person> registeredPersons = getRegisteredPersons();
        Department blotterDept = departmentRepository.findByNameIgnoreCase("BLOTTER").orElse(null);
        Department luponDept = departmentRepository.findByNameIgnoreCase("LUPON").orElse(blotterDept);

        for (int i = 0; i < formalCount; i++) {
            createCaseWithDate(today, FilingType.FORMAL_COMPLAINT, CaseStatus.PENDING, 2000 + i + 1, blotterDept, luponDept, blotterOfficers, blotterUsers, registeredPersons, rnd);
        }
        for (int i = 0; i < recordCount; i++) {
            createCaseWithDate(today, FilingType.FOR_RECORD_ONLY, CaseStatus.RECORDED, 2000 + formalCount + i + 1, blotterDept, luponDept, blotterOfficers, blotterUsers, registeredPersons, rnd);
        }
        log.info("[BlotterCaseDataSeeds] Generated {} formal and {} for-record cases for {}.", formalCount, recordCount, today);
    }

    private List<Officer> getBlotterOfficers() {
        List<Officer> blotterOfficers = officerRepository.findAll().stream()
                .filter(o -> o.getDepartment() != null && o.getDepartment().getName() != null &&
                        o.getDepartment().getName().equalsIgnoreCase("BLOTTER"))
                .toList();
        return blotterOfficers.isEmpty() ? officerRepository.findAll() : blotterOfficers;
    }

    private List<User> getBlotterUsers() {
        List<User> blotterUsers = userRepository.findAll().stream()
                .filter(u -> u.getDepartments() != null && u.getDepartments().stream()
                        .anyMatch(d -> d.getName() != null && d.getName().equalsIgnoreCase("BLOTTER")))
                .toList();
        return blotterUsers.isEmpty() ? userRepository.findAll() : blotterUsers;
    }

    private List<Person> getRegisteredPersons() {
        List<Person> persons = personRepository.findAll().stream()
                .filter(p -> p.getFirstName() != null && !p.getFirstName().isBlank() &&
                             p.getLastName() != null && !p.getLastName().isBlank())
                .toList();
        return persons;
    }

    /**
     * Seeds 52 cases realistically distributed across May, June, July, and August 2026.
     */
    private void seedHistoricalCases(int totalCount) {
        LocalDate today = LocalDate.now();
        Department blotterDept = departmentRepository.findByNameIgnoreCase("BLOTTER").orElse(null);
        Department luponDept = departmentRepository.findByNameIgnoreCase("LUPON").orElse(blotterDept);
        List<Officer> blotterOfficers = getBlotterOfficers();
        List<User> blotterUsers = getBlotterUsers();
        List<Person> registeredPersons = getRegisteredPersons();

        Random random = new Random(42); // fixed seed for consistency

        List<LocalDate> targetDates = new ArrayList<>();

        // May 2026 (10 cases)
        for (int i = 0; i < 10; i++) {
            targetDates.add(today.minusMonths(3).withDayOfMonth(Math.min(2 + i * 2, 28)));
        }
        // June 2026 (12 cases)
        for (int i = 0; i < 12; i++) {
            targetDates.add(today.minusMonths(2).withDayOfMonth(Math.min(1 + i * 2, 28)));
        }
        // July 2026 (14 cases)
        for (int i = 0; i < 14; i++) {
            targetDates.add(today.minusMonths(1).withDayOfMonth(Math.min(2 + i * 2, 28)));
        }
        // August 2026 (16 cases: 10 earlier, 4 yesterday, 2 today)
        for (int i = 0; i < 10; i++) {
            targetDates.add(today.withDayOfMonth(Math.min(1 + i * 2, 24)));
        }
        // Yesterday (4 cases)
        targetDates.add(today.minusDays(1));
        targetDates.add(today.minusDays(1));
        targetDates.add(today.minusDays(1));
        targetDates.add(today.minusDays(1));
        // Today (2 cases)
        targetDates.add(today);
        targetDates.add(today);

        int seq = 100;
        int formalIndex = 0;

        for (int idx = 0; idx < targetDates.size(); idx++) {
            LocalDate date = targetDates.get(idx);
            boolean isForTheRecord = (idx % 3 == 2) && (idx < targetDates.size() - 2); // ~15 for the record
            FilingType filingType = isForTheRecord ? FilingType.FOR_RECORD_ONLY : FilingType.FORMAL_COMPLAINT;

            CaseStatus status;
            if (isForTheRecord) {
                status = CaseStatus.RECORDED;
            } else {
                formalIndex++;
                if (idx >= targetDates.size() - 2) {
                    // Today's cases
                    status = (formalIndex % 2 == 0) ? CaseStatus.UNDER_MEDIATION : CaseStatus.PENDING;
                } else if (idx >= targetDates.size() - 6) {
                    // Yesterday's cases
                    status = (formalIndex % 3 == 0) ? CaseStatus.UNDER_CONCILIATION : CaseStatus.UNDER_MEDIATION;
                } else {
                    // Historical distribution:
                    // 40% Settled, 20% Under Mediation, 15% Under Conciliation, 15% Dismissed, 10% Pending
                    int roll = random.nextInt(100);
                    if (roll < 40) status = CaseStatus.SETTLED;
                    else if (roll < 60) status = CaseStatus.UNDER_MEDIATION;
                    else if (roll < 75) status = CaseStatus.UNDER_CONCILIATION;
                    else if (roll < 90) status = CaseStatus.DISMISSED;
                    else status = CaseStatus.PENDING;
                }
            }

            createCaseWithDate(date, filingType, status, seq++, blotterDept, luponDept, blotterOfficers, blotterUsers, registeredPersons, random);
        }
    }

    private Person getOrCreatePerson(List<Person> registeredPersons, Random random, Long excludeId) {
        if (registeredPersons != null && registeredPersons.size() >= 2) {
            List<Person> pool = registeredPersons.stream()
                    .filter(p -> excludeId == null || !p.getId().equals(excludeId))
                    .toList();
            if (!pool.isEmpty()) {
                return pool.get(random.nextInt(pool.size()));
            }
        }
        // Fallback: create distinct registered person
        Person p = new Person();
        p.setFirstName("Resident" + (100 + random.nextInt(900)));
        p.setLastName("Ugong" + (100 + random.nextInt(900)));
        p.setGender(random.nextBoolean() ? "Male" : "Female");
        p.setAge((short) (25 + random.nextInt(40)));
        p.setCivilStatus(random.nextBoolean() ? "Single" : "Married");
        p.setContactNumber("09" + (100000000 + random.nextInt(899999999)));
        p.setCompleteAddress(LOCATIONS[random.nextInt(LOCATIONS.length)] + ", Brgy. Ugong, Valenzuela City");
        p.setIsResident(true);
        return personRepository.save(p);
    }

    private void createCaseWithDate(
            LocalDate date,
            FilingType filingType,
            CaseStatus status,
            int sequence,
            Department blotterDept,
            Department luponDept,
            List<Officer> blotterOfficers,
            List<User> blotterUsers,
            List<Person> registeredPersons,
            Random random
    ) {
        String caseNumber = String.format("BC-%s-%04d", date.format(DateTimeFormatter.ofPattern("yyyyMMdd")), sequence);

        // 1. Link Distinct Complainant Person from registered resident pool
        Person compPerson = getOrCreatePerson(registeredPersons, random, null);

        Complainant complainant = new Complainant();
        complainant.setPerson(compPerson);

        // 2. Link Distinct Respondent Person (Guaranteed Different from Complainant)
        Person respPerson = getOrCreatePerson(registeredPersons, random, compPerson.getId());

        Respondent respondent = new Respondent();
        respondent.setPerson(respPerson);
        respondent.setRelationshipToComplainant(RELATIONSHIPS[random.nextInt(RELATIONSHIPS.length)]);
        respondent.setLivingWithComplainant(random.nextInt(10) == 0); // 10% chance true, 90% false

        // 3. Incident Detail
        IncidentDetail incident = new IncidentDetail();
        if (filingType == FilingType.FORMAL_COMPLAINT) {
            incident.setNatureOfComplaint(COMPLAINT_NATURES[random.nextInt(COMPLAINT_NATURES.length)]);
        } else {
            incident.setNatureOfComplaint(RECORD_NATURES[random.nextInt(RECORD_NATURES.length)]);
        }
        incident.setFrequency("First Occurrence");
        incident.setDateOfIncident(date.minusDays(random.nextInt(3)));
        incident.setTimeOfIncident(LocalTime.of(8 + random.nextInt(12), random.nextInt(60)));
        incident.setIncidentLocation(LOCATIONS[random.nextInt(LOCATIONS.length)]);
        incident.setInjuriesDamagesDescription("No permanent structural damage reported. Incident verified on-site.");

        // 4. Narrative Statement
        NarrativeStatement narrative = new NarrativeStatement();
        narrative.setFileName("statement_" + caseNumber.toLowerCase() + ".pdf");
        narrative.setStatementSummary("Complainant " + compPerson.getFirstName() + " " + compPerson.getLastName() +
                " formally filed this blotter record on " + date + " regarding " + incident.getNatureOfComplaint() +
                " involving respondent " + respPerson.getFirstName() + " " + respPerson.getLastName() +
                " at " + incident.getIncidentLocation() + ".");
        narrative.setFileData(DUMMY_PDF);

        Case aCase = new Case();
        aCase.setCaseNumber(caseNumber);
        aCase.setFilingType(filingType);
        aCase.setCaseStatus(status);
        aCase.setIsArchived(false);
        aCase.setIsAttested(true);
        aCase.setAttestedAt(date.atTime(10, 0));

        // Ensure all seeded cases are initial root cases (no reopened parent or child links)
        aCase.setPreviousCaseNumber(null);
        aCase.setReOpenedCaseNumber(null);
        aCase.setReopenRemarks(null);

        if (status == CaseStatus.UNDER_CONCILIATION && luponDept != null) {
            aCase.setDepartment(luponDept);
            aCase.setCaseStatusRemarks("Referred to Lupon Tagapamayapa for conciliation.");
            aCase.setSummonCount(1);
        } else if (status == CaseStatus.UNDER_MEDIATION) {
            if (blotterDept != null) aCase.setDepartment(blotterDept);
            aCase.setCaseStatusRemarks("Scheduled for mediation hearing.");
            aCase.setSummonCount(1);
        } else if (blotterDept != null) {
            aCase.setDepartment(blotterDept);
            aCase.setCaseStatusRemarks("Filed at Blotter Desk.");
            aCase.setSummonCount(0);
        }

        if (status == CaseStatus.SETTLED) {
            aCase.setSettledAt(date.plusDays(2).atTime(14, 30));
            aCase.setSettlementVenue("Barangay Mediation Hall");
            aCase.setSettlementTerms("Both parties mutually agreed to peaceful co-existence, non-repetition of acts, and amicable resolution.");
        } else if (status == CaseStatus.DISMISSED) {
            aCase.setCaseStatusRemarks("Dismissed upon complainant's voluntary withdrawal and amicable understanding.");
        }

        // Strictly assign Blotter Officer
        if (blotterOfficers != null && !blotterOfficers.isEmpty()) {
            aCase.setOfficer(blotterOfficers.get(random.nextInt(blotterOfficers.size())));
        }

        // Strictly assign Blotter User
        if (blotterUsers != null && !blotterUsers.isEmpty()) {
            User actionUser = blotterUsers.get(random.nextInt(blotterUsers.size()));
            aCase.setCaseFilledBy(actionUser);
            aCase.setCaseUpdatedBy(actionUser);
        }

        aCase.setComplainant(complainant);
        aCase.setRespondent(respondent);
        aCase.setIncidentDetail(incident);
        aCase.setNarrativeStatement(narrative);

        Case saved = caseRepository.save(aCase);

        // Schedule Mediation / Conciliation Session if case is under mediation or conciliation
        if (status == CaseStatus.UNDER_MEDIATION) {
            CaseSession session = new CaseSession();
            session.setCases(saved);
            session.setHearingType(SessionType.MEDIATION);
            session.setSummonNumber((short) 1);
            session.setScheduledStart(date.plusDays(3).atTime(9, 0));
            session.setScheduledEnd(date.plusDays(3).atTime(10, 0));
            session.setVenue("Barangay Mediation Hall");
            session.setNotes("First mediation hearing between complainant and respondent.");
            session.setStatus(SessionStatus.SCHEDULED);
            caseSessionRepository.save(session);
        } else if (status == CaseStatus.UNDER_CONCILIATION) {
            CaseSession session = new CaseSession();
            session.setCases(saved);
            session.setHearingType(SessionType.CONCILIATION);
            session.setSummonNumber((short) 1);
            session.setScheduledStart(date.plusDays(5).atTime(14, 0));
            session.setScheduledEnd(date.plusDays(5).atTime(15, 0));
            session.setVenue("Lupon Conciliation Chamber");
            session.setNotes("First conciliation hearing before Pangkat Tagapagkasundo.");
            session.setStatus(SessionStatus.SCHEDULED);
            caseSessionRepository.save(session);
        }

        // Update database timestamps to match target date
        LocalDateTime caseDateTime = date.atTime(8 + random.nextInt(9), random.nextInt(60), random.nextInt(60));
        jdbcTemplate.update("UPDATE cases SET created_at = ?, updated_at = ? WHERE id = ?", caseDateTime, caseDateTime, saved.getId());
        if (saved.getIncidentDetail() != null && saved.getIncidentDetail().getId() != null) {
            jdbcTemplate.update("UPDATE incident_details SET incident_date = ?, created_at = ?, updated_at = ? WHERE id = ?", incident.getDateOfIncident(), caseDateTime, caseDateTime, saved.getIncidentDetail().getId());
        }
        if (saved.getNarrativeStatement() != null && saved.getNarrativeStatement().getId() != null) {
            jdbcTemplate.update("UPDATE narrative_statement SET created_at = ?, updated_at = ? WHERE id = ?", caseDateTime, caseDateTime, saved.getNarrativeStatement().getId());
        }
    }
}
