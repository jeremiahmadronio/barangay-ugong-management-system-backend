package com.barangay.cases.lupon.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
/**
 * Request payload for escalating an unresolved desk-level Blotter case to Lupong Tagapamayapa.
 *
 * @param memberList        statutory 3-member Pangkat Tagapagkasundo panel composition
 */
public record LuponReferRalRequest(

        @NotEmpty(message = "Pangkat panel members are required")
        @Size(min = 3, max = 3, message = "Exactly 3 Pangkat members (Chairman, Secretary, Member) are required")
        List<@Valid PangkatMember> memberList
) {
}
