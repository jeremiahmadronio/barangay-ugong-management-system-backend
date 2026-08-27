package com.barangay.cases.shared.projection;

/**
 * Projection interface representing aggregated resolution metrics
 * exclusively for formal complaints.
 */
public interface FormalComplaintResolutionProjection {

    /**
     * Retrieves the total count of formal complaints filed.
     *
     * @return count of formal complaints
     */
    Long getTotalFormalComplaints();

    /**
     * Retrieves the count of cases settled directly at desk level without Lupon referral.
     *
     * @return count of desk-settled cases
     */
    Long getSettledAtBlotter();

    /**
     * Retrieves the count of cases escalated or referred to Lupong Tagapamayapa.
     *
     * @return count of Lupon referrals
     */
    Long getReferredToLupon();

    /**
     * Retrieves the count of cases still pending or unresolved at desk level.
     *
     * @return count of pending desk cases
     */
    Long getPendingAtBlotter();
}