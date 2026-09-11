package com.barangay.people.officer.service;

import com.barangay.people.officer.dto.*;
import java.util.List;

public interface OfficerService {
    OfficerStatsResponse getOfficerStats();
    List<OfficerTableResponse> getFilteredOfficersTable(OfficerFilterRequest filter);
    OfficerTableResponse createOfficer(OfficerCreateRequest request);
    OfficerTableResponse updateOfficer(Long id, OfficerUpdateRequest request);
    OfficerTableResponse updateOfficerStatus(Long id, OfficerStatusUpdateRequest request);
    void deleteOfficer(Long id);
    void softDeleteOfficer(Long id);
    void permanentDeleteOfficer(Long id);
    OfficerTableResponse restoreOfficer(Long id);
    List<LuponOfficerResponse> getLuponOfficer();
    List<OfficerCaseHistoryResponse> getOfficerCasesHandled(Long officerId);
}

