package com.barangay.cases.blotter.dto;

/**
 * Filter parameters for the Archived Blotter cases table.
 *
 * @param search     keyword for case number, complainant's last name, or respondent's last name
 * @param filingType type filter ("FORMAL_COMPLAINT", "FOR_RECORD_ONLY", or "ALL")
 * @param page       page number (0-indexed)
 * @param size       page size
 */
public record BlotterArchiveFilterRequest(
        String search,
        String filingType,
        Integer page,
        Integer size
) {
    public int getPageNumber() {
        return (page != null && page >= 0) ? page : 0;
    }
    public int getPageSize() {
        return (size != null && size > 0) ? size : 10;
    }
}