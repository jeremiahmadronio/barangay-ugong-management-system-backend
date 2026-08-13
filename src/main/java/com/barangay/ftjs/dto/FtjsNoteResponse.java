package com.barangay.ftjs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FtjsNoteResponse {
    private Long id;
    private Long ftjsId;
    private String note;
    private String createdBy;
    private LocalDateTime createdAt;
}