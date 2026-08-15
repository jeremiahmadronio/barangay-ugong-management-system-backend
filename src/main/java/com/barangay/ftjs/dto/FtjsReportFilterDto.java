package com.barangay.ftjs.dto;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public record FtjsReportFilterDto(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
        String purpose,
        String sex,
        String status,
        String educationalAttainment
) {}