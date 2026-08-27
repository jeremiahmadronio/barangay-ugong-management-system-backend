package com.barangay.zshared.dataseeds;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * DatabaseConstraintFixer.java
 * Initializer that drops obsolete PostgreSQL check constraints on cases table
 * to allow updated enum values like REOPENED without throwing database errors.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class DatabaseConstraintFixer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            log.info("Dropping obsolete cases_case_status_check constraint if present...");
            jdbcTemplate.execute("ALTER TABLE cases DROP CONSTRAINT IF EXISTS cases_case_status_check");
            // Restore any past cases that were reopened back to DISMISSED if their status was mutated
            jdbcTemplate.update(
                "UPDATE cases SET case_status = 'DISMISSED' WHERE reopened_case_number IS NOT NULL AND TRIM(reopened_case_number) != '' AND case_status IN ('PENDING', 'REOPENED')"
            );
            // Update For the Record entries to RECORDED status if currently set to PENDING
            jdbcTemplate.update(
                "UPDATE cases SET case_status = 'RECORDED' WHERE (filing_type = 'FOR_RECORD_ONLY' OR filing_type = 'FOR_THE_RECORD') AND case_status = 'PENDING'"
            );
            log.info("Successfully dropped obsolete cases table constraint, updated For the Record statuses to RECORDED, and restored past case statuses.");
        } catch (Exception e) {
            log.warn("Could not execute constraint update on cases table: {}", e.getMessage());
        }
    }
}
