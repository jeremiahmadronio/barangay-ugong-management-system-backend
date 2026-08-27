package com.barangay.cases.shared.projection;

/**
 * Spring Data JPA Projection interface for Blotter Lifecycle KPIs (Archived and Trash counts).
 */
public interface BlotterLifecycleKpiProjection {

    /**
     * Retrieves the all-time count of archived blotter cases.
     *
     * @return all-time archived count
     */
    Number getTotalArchived();

    /**
     * Retrieves the count of blotter cases archived within the current month.
     *
     * @return monthly archived count
     */
    Number getTotalArchivedThisMonth();

    /**
     * Retrieves the all-time count of soft-deleted blotter cases currently in trash.
     *
     * @return all-time recently deleted count
     */
    Number getTotalRecentlyDeleted();

    /**
     * Retrieves the count of blotter cases soft-deleted within the current month.
     *
     * @return monthly recently deleted count
     */
    Number getTotalRecentlyDeletedThisMonth();
}