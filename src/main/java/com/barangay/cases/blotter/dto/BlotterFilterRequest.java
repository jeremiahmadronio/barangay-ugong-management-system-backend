package com.barangay.cases.blotter.dto;

public record BlotterFilterRequest(
        String search,
        String status,
        String filingType,
        String natureOfComplaint,
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
