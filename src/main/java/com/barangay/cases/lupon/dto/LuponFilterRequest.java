package com.barangay.cases.lupon.dto;

import java.time.LocalDate;

public record LuponFilterRequest(
        String search,
        String status,
        String natureOfComplaint,
        LocalDate startDate,
        LocalDate endDate,
        Integer page,
        Integer size
) {
    public int getPageNumber() {
        return (page != null && page >= 0) ? page : 0;
    }

    public int getPageSize() {
        return (size != null && size > 0) ? size : 10;
    }
}
