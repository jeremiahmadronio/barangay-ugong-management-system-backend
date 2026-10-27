package com.barangay.zshared.enumerated;

public enum RelatedBarangayService {
    FTJS_REISSUANCE,
    BARANGAY_CLEARANCE,
    CERTIFICATE_OF_INDIGENCY,
    CERTIFICATE_OF_RESIDENCY,
    GOOD_MORAL_CHARACTER,
    BUSINESS_PERMIT,
    BUILDING_CLEARANCE,
    WORKING_CLEARANCE,
    UTILITY_CONNECTION,
    CERTIFICATION_FOR_BAIL,
    TRICYCLE_CLEARANCE;

    public String getDisplayLabel() {
        return switch (this) {
            case FTJS_REISSUANCE -> "FTJS Certificate Reissuance";
            case BARANGAY_CLEARANCE -> "Barangay Clearance";
            case CERTIFICATE_OF_INDIGENCY -> "Certificate of Indigency";
            case CERTIFICATE_OF_RESIDENCY -> "Certificate of Residency";
            case GOOD_MORAL_CHARACTER -> "Certificate of Good Moral Character";
            case BUSINESS_PERMIT -> "Barangay Business Clearance";
            case BUILDING_CLEARANCE -> "Building Clearance";
            case WORKING_CLEARANCE -> "Working Clearance";
            case UTILITY_CONNECTION -> "Utility Connection Clearance";
            case CERTIFICATION_FOR_BAIL -> "Certification for Bail";
            case TRICYCLE_CLEARANCE -> "Tricycle / Vehicle Clearance";
        };
    }

    public boolean isReissuanceService() {
        return this == FTJS_REISSUANCE;
    }
}
