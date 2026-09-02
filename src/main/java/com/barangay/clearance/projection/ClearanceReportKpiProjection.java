package com.barangay.clearance.projection;

public interface ClearanceReportKpiProjection {
    Long getTotalIssued();
    Long getTotalExpired();
    Long getMaleCount();
    Long getFemaleCount();
}
