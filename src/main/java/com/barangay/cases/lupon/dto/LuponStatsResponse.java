package com.barangay.cases.lupon.dto;

/**
 * LuponStatsResponse.java
 *
 * DTO representing top KPI summary statistics for the Lupong Tagapamayapa module.
 */
public record LuponStatsResponse(
        long totalReferred,
        String totalReferredTrend,
        long activeConciliation,
        long successfullySettled,
        long cfaIssued
) {}
