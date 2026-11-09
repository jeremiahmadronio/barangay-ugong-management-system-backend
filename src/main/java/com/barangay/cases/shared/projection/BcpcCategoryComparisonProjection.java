package com.barangay.cases.shared.projection;

/**
 * Spring Data JPA Projection interface for CAR vs CICL category comparison metrics.
 */
public interface BcpcCategoryComparisonProjection {
    Number getCarCases();
    Number getCiclCases();
}

