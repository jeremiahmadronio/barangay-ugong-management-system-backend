package com.barangay.ftjs.dto;


import java.time.LocalDate;

public record TrendPointDto(
        String label,       // "Aug 01" (Daily) o "Aug 26" (Monthly)
        long issued,
        LocalDate date
) {}