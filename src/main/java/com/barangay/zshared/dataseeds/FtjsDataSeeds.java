package com.barangay.zshared.dataseeds;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.barangay.ftjs.model.FirstTimeJobSeeker;
import com.barangay.ftjs.model.FirstTimeJobSeekerTimeLine;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerTimeLineRepository;
import com.barangay.people.person.model.Person;
import com.barangay.people.resident.model.Resident;
import com.barangay.people.resident.repository.ResidentRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.enumerated.FtjsStatus;
import com.barangay.zshared.enumerated.TimeLineType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * FtjsDataSeeds.java
 *
 * Bootstraps initial historical First-Time Jobseeker (FTJS) records under RA 11261
 * for analytics, report filtering, and trend evaluation.
 * - Seeds 60 records connected to distinct existing Residents/Persons.
 * - Leaves document verification files (valid ID and oath) optional/null.
 * - Distributes issuance timestamps realistically across the past 6 months (March to September 2026).
 */
@Slf4j
@Component
@Profile("!test")
@Order(12)
@RequiredArgsConstructor
public class FtjsDataSeeds implements CommandLineRunner {

    private final FirstTimeJobSeekerRepository ftjsRepository;
    private final FirstTimeJobSeekerTimeLineRepository ftjsTimelineRepository;
    private final ResidentRepository residentRepository;
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final String[] PURPOSES = {
            "First Time Jobseeker Requirement",
            "Employment Application",
            "Local Employment",
            "Government Requirement",
            "Pre-Employment Requirement"
    };

    private static final String[] COLLEGE_COURSES = {
            "BS Information Technology",
            "BS Business Administration",
            "BS Hospitality Management",
            "BS Accountancy",
            "BS Criminology",
            "BS Secondary Education",
            "BS Computer Engineering",
            "BS Nursing"
    };

    private static final String[] SHS_STRANDS = {
            "STEM Strand",
            "ABM Strand",
            "HUMSS Strand",
            "TVL - Information & Communications Technology",
            "General Academic Strand (GAS)"
    };

    private static final String[] VOCATIONAL_COURSES = {
            "NC II Cookery",
            "NC II Automotive Servicing",
            "NC II Computer Systems Servicing",
            "Shielded Metal Arc Welding NC II",
            "Bread and Pastry Production NC II"
    };

    private static final String[] HIGH_SCHOOL_COURSES = {
            "High School Graduate",
            "Grade 10 Completer"
    };

    private static final String[] ELEMENTARY_COURSES = {
            "Elementary Graduate",
            "Grade 6"
    };

    private static final String[] HIGHER_ED_SCHOOLS = {
            "Pamantasan ng Lungsod ng Valenzuela (PLV)",
            "Valenzuela City Polytechnic College (VCPC)",
            "Our Lady of Fatima University",
            "STI College - Valenzuela",
            "University of Caloocan City"
    };

    private static final String[] TECH_VOC_SCHOOLS = {
            "Valenzuela City Technical Institute (VTI)",
            "TESDA Women's Center",
            "Valenzuela Skills & Training Academy"
    };

    private static final String[] HIGH_SCHOOLS = {
            "Ugong National High School",
            "Valenzuela City Stand-Alone Senior High School",
            "Malinta National High School",
            "Valenzuela National High School"
    };

    private static final String[] ELEMENTARY_SCHOOLS = {
            "Barangay Ugong Elementary School",
            "Purok 1 Elementary School",
            "Malinta Elementary School"
    };

    @Override
    @Transactional
    public void run(String... args) {
        long existingCount = ftjsRepository.count();
        if (existingCount >= 60) {
            log.info("FTJS records already seeded ({} records found). Skipping...", existingCount);
            return;
        }

        List<Resident> residents = residentRepository.findAll().stream()
                .filter(r -> r.getPerson() != null && r.getPerson().getFirstName() != null && !r.getPerson().getFirstName().isBlank())
                .toList();

        if (residents.isEmpty()) {
            log.warn("No resident records available to seed FTJS applications.");
            return;
        }

        seedFtjsRecords(residents, 60);
    }

    private void seedFtjsRecords(List<Resident> residents, int totalCount) {
        log.info("Starting historical FTJS dataset seeding (Target: {} records across last 6 months)...", totalCount);

        User verifiedUser = userRepository.findAll().stream()
                .filter(u -> u.getSystemEmail() != null || u.getUsername() != null)
                .findFirst()
                .orElse(null);

        LocalDate today = LocalDate.of(2026, 9, 11);
        Random random = new Random(11261);

        // Build 60 dates distributed realistically across the last 6 months (March - Sept 2026)
        List<LocalDate> targetDates = new ArrayList<>();

        // March 2026 (8 records)
        int[] marchDays = {3, 7, 12, 16, 19, 23, 26, 30};
        for (int d : marchDays) targetDates.add(LocalDate.of(2026, 3, d));

        // April 2026 (9 records)
        int[] aprilDays = {2, 6, 10, 14, 17, 21, 24, 28, 30};
        for (int d : aprilDays) targetDates.add(LocalDate.of(2026, 4, d));

        // May 2026 (10 records)
        int[] mayDays = {4, 7, 11, 14, 18, 21, 25, 27, 29, 31};
        for (int d : mayDays) targetDates.add(LocalDate.of(2026, 5, d));

        // June 2026 (11 records)
        int[] juneDays = {2, 5, 9, 12, 16, 19, 22, 25, 26, 28, 30};
        for (int d : juneDays) targetDates.add(LocalDate.of(2026, 6, d));

        // July 2026 (10 records)
        int[] julyDays = {3, 6, 10, 13, 17, 20, 23, 27, 29, 31};
        for (int d : julyDays) targetDates.add(LocalDate.of(2026, 7, d));

        // August 2026 (8 records)
        int[] augustDays = {4, 8, 12, 17, 20, 24, 27, 31};
        for (int d : augustDays) targetDates.add(LocalDate.of(2026, 8, d));

        // September 2026 (4 records)
        int[] septDays = {2, 5, 8, 11};
        for (int d : septDays) targetDates.add(LocalDate.of(2026, 9, d));

        int residentsAvailable = residents.size();
        int seededCount = 0;

        for (int i = 0; i < targetDates.size() && i < residentsAvailable; i++) {
            LocalDate date = targetDates.get(i);
            Resident resident = residents.get(i % residentsAvailable);
            Person person = resident.getPerson();

            // Check if this resident already has an FTJS record
            if (ftjsRepository.existsByResidentIdAndIsDeletedFalse(resident.getResidentId())) {
                continue;
            }

            // Determine Educational Attainment distribution
            String education;
            String course;
            String institution;

            int eduType = i % 10;
            if (eduType < 4) { // 40% College Graduate
                education = "College Graduate";
                course = COLLEGE_COURSES[random.nextInt(COLLEGE_COURSES.length)];
                institution = HIGHER_ED_SCHOOLS[random.nextInt(HIGHER_ED_SCHOOLS.length)];
            } else if (eduType < 7) { // 30% Senior High School
                education = "Senior High School";
                course = SHS_STRANDS[random.nextInt(SHS_STRANDS.length)];
                institution = HIGH_SCHOOLS[random.nextInt(HIGH_SCHOOLS.length)];
            } else if (eduType < 8) { // 10% Vocational / TESDA
                education = "Vocational / TESDA";
                course = VOCATIONAL_COURSES[random.nextInt(VOCATIONAL_COURSES.length)];
                institution = TECH_VOC_SCHOOLS[random.nextInt(TECH_VOC_SCHOOLS.length)];
            } else if (eduType < 9) { // 10% High School
                education = "High School";
                course = HIGH_SCHOOL_COURSES[random.nextInt(HIGH_SCHOOL_COURSES.length)];
                institution = HIGH_SCHOOLS[random.nextInt(HIGH_SCHOOLS.length)];
            } else { // 10% Elementary
                education = "Elementary";
                course = ELEMENTARY_COURSES[random.nextInt(ELEMENTARY_COURSES.length)];
                institution = ELEMENTARY_SCHOOLS[random.nextInt(ELEMENTARY_SCHOOLS.length)];
            }

            String purpose = PURPOSES[i % PURPOSES.length];
            String ftjsNumber = String.format("2026-FTJS-%04d", 1001 + i);

            // A few records as RE_ISSUANCE for chart variety (e.g., indices 15, 35)
            boolean isReissuance = (i == 15 || i == 35);
            FtjsStatus status = isReissuance ? FtjsStatus.RE_ISSUANCE : FtjsStatus.ISSUED;
            int issuanceCount = isReissuance ? 2 : 1;

            FirstTimeJobSeeker ftjs = new FirstTimeJobSeeker();
            ftjs.setResident(resident);
            ftjs.setPerson(person);
            ftjs.setFtjsNumber(ftjsNumber);
            ftjs.setPurposeDocuments(purpose);
            ftjs.setEducationalAttainment(education);
            ftjs.setCourse(course);
            ftjs.setSchoolInstitution(institution);
            ftjs.setSchoolAddress("Valenzuela City");
            ftjs.setValidIdType("Barangay Resident ID");
            ftjs.setValidIdFiles(null); // Optional
            ftjs.setOathFiles(null);    // Optional
            ftjs.setIssuanceCount(issuanceCount);
            ftjs.setDateSubmitted(date);
            ftjs.setStatus(status);
            ftjs.setVerifiedBy(verifiedUser);
            ftjs.setIsDeleted(false);
            ftjs.setIsArchived(false);

            FirstTimeJobSeeker savedFtjs = ftjsRepository.save(ftjs);

            // Create corresponding audit timeline
            LocalDateTime recordDateTime = date.atTime(8 + random.nextInt(8), random.nextInt(60), random.nextInt(60));

            FirstTimeJobSeekerTimeLine timeline = new FirstTimeJobSeekerTimeLine();
            timeline.setFtjs(savedFtjs);
            timeline.setTimelineType(isReissuance ? TimeLineType.RE_ISSUANCE : TimeLineType.SUBMITTED);
            timeline.setTitle(isReissuance ? "FTJS Certificate Re-Issued" : "FTJS Certificate Issued");
            timeline.setDescription(String.format(
                    "%s processed for %s %s (App #: %s) under RA 11261.",
                    isReissuance ? "Re-issuance" : "First-time certificate",
                    person.getFirstName(), person.getLastName(),
                    ftjsNumber
            ));
            timeline.setEventDate(recordDateTime);
            timeline.setCreatedBy(verifiedUser);
            ftjsTimelineRepository.save(timeline);

            // Update database timestamps to match target date exactly
            jdbcTemplate.update(
                    "UPDATE first_time_job_seeker SET created_at = ?, updated_at = ? WHERE id = ?",
                    recordDateTime, recordDateTime, savedFtjs.getId()
            );

            seededCount++;
        }

        log.info("Successfully seeded {} FTJS historical records spanning the last 6 months.", seededCount);
    }
}
