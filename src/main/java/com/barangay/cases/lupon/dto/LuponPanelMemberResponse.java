package com.barangay.cases.lupon.dto;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing an assigned Pangkat panel member in response payloads.
 *
 * @param id          unique identifier of the panel membership record
 * @param officerId   database identifier of the assigned officer
 * @param officerName concatenated full name of the officer
 * @param position    role held within the panel (CHAIRMAN, SECRETARY, MEMBER)
 * @param assignedAt  timestamp when the officer was designated to the panel
 */
public record LuponPanelMemberResponse(
        Long id,
        Long officerId,
        String officerName,
        String position,
        LocalDateTime assignedAt
) {}