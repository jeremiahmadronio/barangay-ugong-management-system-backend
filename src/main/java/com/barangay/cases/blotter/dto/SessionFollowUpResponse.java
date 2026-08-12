package com.barangay.cases.blotter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionFollowUpResponse {

    private Long id;
    private Long sessionId;
    private String remarks;
    private String recordedBy;
    private LocalDateTime createdAt;
}
