package com.barangay.zshared.mapper;

import com.barangay.cases.sharedmodel.Case;
import com.barangay.cases.sharedmodel.CaseEvidence;
import com.barangay.cases.sharedmodel.EvidenceType;
import com.barangay.cases.sharedmodel.repository.EvidenceTypeRepository;
import com.barangay.zshared.dto.EvidenceCreateDto;
import com.barangay.zshared.dto.EvidenceDetailDto;
import com.barangay.zshared.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
/**
 * Converts between {@link CaseEvidence} entities and their corresponding
 * evidence DTOs.
 * <p>
 * Note: {@link #fromCreateDto} performs a read-only lookup via
 * {@link EvidenceTypeRepository} to resolve the evidence type by name.
 * If stricter mapper/persistence separation is desired, consider resolving
 * the {@link EvidenceType} in the service layer and passing it in directly.
 */
@Component
@RequiredArgsConstructor
public class CaseEvidenceMapper {

    private final EvidenceTypeRepository evidenceTypeRepository;


    /**
     * Maps a {@link CaseEvidence} entity to its detail DTO.
     *
     * @param evidence the entity to convert, may be {@code null}
     * @return the corresponding {@link EvidenceDetailDto}, or {@code null} if input is {@code null}
     */
    public EvidenceDetailDto toDetailDto(CaseEvidence evidence) {
        if (evidence == null) return null;

        return new EvidenceDetailDto(
                evidence.getId(),
                evidence.getFileName(),
                evidence.getFiles()
        );
    }

    /**
     * Maps a list of {@link CaseEvidence} entities to detail DTOs.
     *
     * @param evidences the entities to convert, may be {@code null}
     * @return list of mapped DTOs, or an empty list if input is {@code null}
     */
    public List<EvidenceDetailDto> toDetailDtoList(List<CaseEvidence> evidences) {
        if (evidences == null) return List.of();
        return evidences.stream().map(this::toDetailDto).toList();
    }


    /**
     * Builds a new, unsaved {@link CaseEvidence} entity from a create request,
     * resolving the evidence type by name.
     *
     * @param dto   the create request
     * @param aCase the case the evidence belongs to
     * @return a new, unsaved {@link CaseEvidence} entity
     * @throws BusinessRuleException if {@code dto.evidenceTypeName()} does not match a known {@link EvidenceType}
     */
    public CaseEvidence fromCreateDto(EvidenceCreateDto dto, Case aCase) {
        if (dto == null) return null;

        EvidenceType type = evidenceTypeRepository.findByNameIgnoreCase(dto.evidenceTypeName())
                .orElseThrow(() -> new BusinessRuleException("Unknown evidence type: " + dto.evidenceTypeName()));

        CaseEvidence evidence = new CaseEvidence();
        evidence.setCases(aCase);
        evidence.setType(type);
        evidence.setFileName(dto.evidenceTypeName());
        evidence.setFiles(dto.files());
        return evidence;
    }
}