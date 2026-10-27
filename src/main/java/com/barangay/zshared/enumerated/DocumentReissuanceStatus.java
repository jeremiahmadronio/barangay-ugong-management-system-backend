package com.barangay.zshared.enumerated;

public enum DocumentReissuanceStatus {
    WAITING_FOR_BLOTTER,
    PENDING_VERIFICATION,
    APPROVED_FOR_REISSUANCE,
    REJECTED,
    COMPLETED;

    public String getDisplayLabel() {
        return switch (this) {
            case WAITING_FOR_BLOTTER     -> "Waiting for Blotter";
            case PENDING_VERIFICATION    -> "Pending Verification";
            case APPROVED_FOR_REISSUANCE -> "Approved for Reissuance";
            case REJECTED                -> "Rejected";
            case COMPLETED               -> "Completed";
        };
    }
}
