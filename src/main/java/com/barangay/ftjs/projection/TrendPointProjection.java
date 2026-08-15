package com.barangay.ftjs.projection;

import java.time.LocalDate;

public interface TrendPointProjection {
    String getLabel();
    LocalDate getPointDate();
    Long getIssuedCount();
}