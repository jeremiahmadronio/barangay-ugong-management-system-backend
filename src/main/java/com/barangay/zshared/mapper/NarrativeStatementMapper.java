package com.barangay.zshared.mapper;

import com.barangay.cases.shared.model.NarrativeStatement;
import com.barangay.zshared.dto.NarrativeCreateDto;
import com.barangay.zshared.dto.NarrativeDetailDto;
import org.springframework.stereotype.Component;
/**
 * Converts between {@link NarrativeStatement} entities and their corresponding DTOs.
 * Pure transformation only — no persistence logic; saving is the responsibility
 * of the calling service.
 */
@Component
public class NarrativeStatementMapper {

    /**
     * Maps a {@link NarrativeStatement} entity to its detail DTO.
     *
     * @param narrative the entity to convert, may be {@code null}
     * @return the corresponding DTO, or {@code null} if input is {@code null}
     */
    public NarrativeDetailDto toDetailDto(NarrativeStatement narrative) {
        if (narrative == null) return null;

        boolean hasFile = narrative.getFileData() != null && narrative.getFileData().length > 0;
        return new NarrativeDetailDto(
                narrative.getId(),
                narrative.getFileName(),
                narrative.getStatementSummary(),
                narrative.getFileData(),
                hasFile
        );
    }

    /**
     * Builds a new, unsaved {@link NarrativeStatement} entity from a create request.
     *
     * @param dto the create request
     * @return a new, unsaved {@link NarrativeStatement} entity, or {@code null} if {@code dto} is {@code null}
     */
    public NarrativeStatement fromCreateDto(NarrativeCreateDto dto) {
        if (dto == null) return null;

        NarrativeStatement narrative = new NarrativeStatement();
        narrative.setFileName(dto.fileName());
        narrative.setStatementSummary(dto.statementSummary());
        narrative.setFileData(dto.fileData());
        return narrative;
    }
}