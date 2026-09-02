package com.barangay.clearance.projection;

import java.time.LocalDate;

public interface ClearanceDailyTrendProjection {
    LocalDate getDate();
    Long getCount();
}
