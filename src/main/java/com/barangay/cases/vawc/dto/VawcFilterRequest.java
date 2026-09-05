package com.barangay.cases.vawc.dto;

public record VawcFilterRequest(
        String search,
        String status,
        String riskLevel,
        Integer page,
        Integer size
) {
    public int getPageNumber() {
        return (page != null && page >= 0) ? page : 0;
    }

    public int getPageSize() {
        return (size != null && size > 0) ? size : 5;
    }
}
