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

            // 1. Set status of all closed/past cases that have been reopened to REOPENED
            jdbcTemplate.update(
                "UPDATE cases SET case_status = 'REOPENED' WHERE reopened_case_number IS NOT NULL AND TRIM(reopened_case_number) != '' AND case_status != 'REOPENED'"
            );

            // 2. Sync root case statuses in DB to match their latest active child case status
            jdbcTemplate.execute(
                "UPDATE cases r SET case_status = (" +
                "  WITH RECURSIVE lineage AS (" +
                "    SELECT c.id, c.case_number, c.reopened_case_number, c.case_status, 1 as depth FROM cases c WHERE c.id = r.id " +
                "    UNION ALL " +
                "    SELECT next_c.id, next_c.case_number, next_c.reopened_case_number, next_c.case_status, l.depth + 1 " +
                "    FROM cases next_c JOIN lineage l ON TRIM(next_c.case_number) = TRIM(l.reopened_case_number)" +
                "  ) SELECT case_status FROM lineage ORDER BY depth DESC LIMIT 1" +
                ") WHERE (r.previous_case_number IS NULL OR TRIM(r.previous_case_number) = '') AND r.reopened_case_number IS NOT NULL AND TRIM(r.reopened_case_number) != ''"
            );
            log.info("Successfully updated cases table constraint and synchronized root case statuses.");
        } catch (Exception e) {
            log.warn("Could not execute constraint update on cases table: {}", e.getMessage());
        }
    }
}
