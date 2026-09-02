package com.barangay.clearance.projection;

/**
 * Spring Data JPA Projection interface for Clearance Lifecycle KPIs (Archived and Trash counts).
 */
public interface ClearanceLifecycleKpiProjection {

    /**
     * All-time count of archived certificates.
     */
    Number getTotalArchived();

    /**
     * Count of certificates archived within the current month.
     */
    Number getTotalArchivedThisMonth();

    /**
     * All-time count of soft-deleted certificates currently in trash.
     */
    Number getTotalRecentlyDeleted();

    /**
     * Count of certificates soft-deleted within the current month.
     */
    Number getTotalRecentlyDeletedThisMonth();
}
