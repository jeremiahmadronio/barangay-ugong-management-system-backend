package com.barangay.cases.bcpc.dto;

import java.time.LocalDateTime;

/**
 * Item in the Active Interventions & Agency Referrals Monitoring Feed on the BCPC Dashboard.
 */
public record BcpcDashboardMonitoringItem(
        Long id,
        Long caseId,
        String caseNumber,
        String childName,
        String category, // "CAR" or "CICL"
        String itemType, // "REFERRAL" or "INTERVENTION"
        String title,
        String agencyOrFacilitator,
        String status,
        LocalDateTime itemDate,
        long daysPending
) {}

