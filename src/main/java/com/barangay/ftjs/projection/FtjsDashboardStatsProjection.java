package com.barangay.ftjs.projection;

public interface FtjsDashboardStatsProjection {
    Long getIssuedToday();
    Long getIssuedThisWeek();
    Long getFemaleCount();
    Long getYouthCount();
    Long getTotalCount();
}