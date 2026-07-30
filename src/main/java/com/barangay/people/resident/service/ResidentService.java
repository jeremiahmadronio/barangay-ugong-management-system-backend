package com.barangay.people.resident.service;

import com.barangay.people.resident.dto.*;

import java.util.List;

/**
 * Service interface for Resident Management business logic.
 */
public interface ResidentService {
    ResidentManagementStatsResponse getResidentStats();
    List<ResidentTableResponse> getFilteredResidentsTable(ResidentFilterRequest filter);
    ResidentDetailResponse getResidentDetail(Long id);
    ResidentTableResponse createResident(ResidentCreateRequest request);
    ResidentTableResponse updateResident(Long id, ResidentUpdateRequest request);
    ResidentTableResponse updateResidentStatus(Long id, ResidentStatusUpdateRequest request);
    ResidentTableResponse archiveResident(Long id);
    ResidentTableResponse restoreResident(Long id);
    void deleteResident(Long id);

    /** Suggests the next available Barangay ID and Household Number for a new resident form. */
    ResidentSuggestionsResponse getSuggestions();

    /** Returns active residents matching search term for Officer registration. */
    List<ResidentSearchOptionResponse> searchActiveResidents(String search);

    /** Returns all active residents with the given last name (for family/relative suggestions). */
    List<ResidentRelativeResponse> getRelativesByLastName(String lastName);
}
