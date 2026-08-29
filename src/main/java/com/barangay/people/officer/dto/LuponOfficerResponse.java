package com.barangay.people.officer.dto;

public record LuponOfficerResponse (
        Long officerId,
        String firstName,
        String lastName,
        String middleName,
        String position
) {
    public String getFullName() {
        String mid = (middleName != null && !middleName.isBlank()) ? " " + middleName : "";
        return (firstName != null ? firstName : "") + mid + " " + (lastName != null ? lastName : "");
    }
}
