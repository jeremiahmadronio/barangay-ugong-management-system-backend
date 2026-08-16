package com.barangay.ftjs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * DTO request for adding administrative notes to an FTJS record.
 */
public class FtjsNoteRequest {

    @NotBlank(message = "Note content cannot be blank")
    private String note;
}