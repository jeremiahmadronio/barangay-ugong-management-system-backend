package com.barangay.cases.lupon.dto;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object representing the full details of an active Lupon conciliation referral.
 *
 * @param id               database identifier of the Lupon referral
 * @param caseNumber       official tracking number of the referred case
 * @param referredAt       timestamp when the case was officially transferred to Lupon
 * @param initialDeadline  statutory 15-day KP conciliation deadline
 * @param extendedDeadline optional extended deadline granted for settlement
 * @param extensionCount   number of deadline extensions granted
 * @param panelMembers     list of assigned Pangkat Tagapagkasundo members
 */
public record LuponReferralResponse(
        Long id,
        String caseNumber,
        LocalDateTime referredAt,
        LocalDateTime initialDeadline,
        LocalDateTime extendedDeadline,
        Integer extensionCount,
        List<LuponPanelMemberResponse> panelMembers
) {}