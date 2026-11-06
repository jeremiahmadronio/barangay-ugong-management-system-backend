package com.barangay.cases.bcpc.dto;

public record BcpcFilterRequest(
        String search,
        String status,
        String category,
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

