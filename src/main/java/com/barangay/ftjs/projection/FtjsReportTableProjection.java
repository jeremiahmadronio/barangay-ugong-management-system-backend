package com.barangay.ftjs.projection;

import java.time.LocalDateTime;

public interface FtjsReportTableProjection {
    Long getId();
    String getApplicationNo();
    String getApplicantName();
    Long getIssuanceCount();
    String getPurpose();
    String getStatus();
    LocalDateTime getDateSubmitted();
}
