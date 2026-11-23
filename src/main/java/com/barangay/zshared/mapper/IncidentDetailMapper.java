package com.barangay.zshared.mapper;

import com.barangay.cases.shared.model.IncidentDetail;
import com.barangay.zshared.dto.IncidentCreateDto;
import com.barangay.zshared.dto.IncidentDetailDto;
import com.barangay.zshared.enumerated.RelatedBarangayService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Converts between {@link IncidentDetail} entities and their corresponding DTOs.
 * Pure transformation only — no persistence logic.
 */
@Component
public class IncidentDetailMapper {

    /**
     * Maps an {@link IncidentDetail} entity to its detail DTO. Date and time
     * fields are serialized via {@code toString()}.
     *
     * @param incident the entity to convert, may be {@code null}
     * @return the corresponding DTO, or {@code null} if input is {@code null}
     */
    public IncidentDetailDto toDetailDto(IncidentDetail incident) {
        if (incident == null) {
            return null;
        }

        return new IncidentDetailDto(
                incident.getId(),
                incident.getNatureOfComplaint(),
                incident.getDateOfIncident() != null ? incident.getDateOfIncident().toString() : null,
                incident.getTimeOfIncident() != null ? incident.getTimeOfIncident().toString() : null,
                incident.getIncidentLocation(),
                incident.getFrequency(),
                incident.getInjuriesDamagesDescription(),
                incident.getRelatedBarangayService() != null ? incident.getRelatedBarangayService().name() : null
        );
    }

    /**
     * Builds a new, unsaved {@link IncidentDetail} entity from a create request.
     *
     * @param dto the create request
     * @return a new, unsaved {@link IncidentDetail} entity, or {@code null} if {@code dto} is {@code null}
     */
    public IncidentDetail fromCreateDto(IncidentCreateDto dto) {
        if (dto == null) {
            return null;
        }

        IncidentDetail incidentDetail = new IncidentDetail();
        incidentDetail.setNatureOfComplaint(dto.natureOfComplaint());
        incidentDetail.setFrequency(dto.frequencyOfIncident());
        incidentDetail.setIncidentLocation(dto.placeOfIncident());
        incidentDetail.setInjuriesDamagesDescription(dto.descriptionOfInjuries());

        if (dto.relatedBarangayService() != null && !dto.relatedBarangayService().isBlank()) {
            try {
                incidentDetail.setRelatedBarangayService(
                        RelatedBarangayService.valueOf(dto.relatedBarangayService().trim().toUpperCase())
                );
            } catch (Exception ignored) {
            }
        }

        if (dto.dateOfIncident() != null && !dto.dateOfIncident().isBlank()) {
            try {
                incidentDetail.setDateOfIncident(LocalDate.parse(dto.dateOfIncident()));
            } catch (Exception ignored) {
            }
        }
        if (dto.timeOfIncident() != null && !dto.timeOfIncident().isBlank()) {
            try {
                incidentDetail.setTimeOfIncident(LocalTime.parse(dto.timeOfIncident()));
            } catch (Exception ignored) {
            }
        }

        return incidentDetail;
    }
}
