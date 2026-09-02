package com.barangay.clearance.projection;

public interface ClearanceMonthlyTrendProjection {
    Number getYear();
    Number getMonth();
    Long getCount();
}
