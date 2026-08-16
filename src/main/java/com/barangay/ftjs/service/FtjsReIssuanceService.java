package com.barangay.ftjs.service;

import com.barangay.ftjs.dto.FtjsTimelineDto;
import com.barangay.ftjs.dto.ReIssuanceHistoryDto;
import com.barangay.ftjs.dto.ReIssuanceRequestDto;
import com.barangay.ftjs.model.FirstTimeJobSeeker;
import com.barangay.ftjs.model.FirstTimeJobSeekerAffidavitOfLoss;
import com.barangay.ftjs.model.FirstTimeJobSeekerTimeLine;
import com.barangay.ftjs.repository.FirstTimeJobSeekerAffidavitOfLossRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerTimeLineRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.usermanagement.user.repository.UserRepository;
import com.barangay.zshared.enumerated.FtjsStatus;
import com.barangay.zshared.enumerated.TimeLineType;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * Service class managing FTJS certificate re-issuance requests, loss affidavits, and event timelines.
 */
public class FtjsReIssuanceService {

    private final FirstTimeJobSeekerRepository firstTimeJobSeekerRepository;
    private final FirstTimeJobSeekerAffidavitOfLossRepository firstTimeJobSeekerAffidavitOfLossRepository;
    private final FirstTimeJobSeekerTimeLineRepository  firstTimeJobSeekerTimeLineRepository;
    private final UserRepository userRepository;


    @Transactional
    /**
     * Creates a re-issuance request for an FTJS certificate subject to 1-year validity rules.
     */
    public ReIssuanceRequestDto createReIssuanceRequest(Long ftjsId,ReIssuanceRequestDto request) throws BadRequestException {

        FirstTimeJobSeeker ftjs = firstTimeJobSeekerRepository.findById(ftjsId).orElseThrow(()
        -> new ResourceNotFoundException("ftjs not found"));

        LocalDate originalDate = ftjs.getDateSubmitted() != null ? ftjs.getDateSubmitted() : ftjs.getCreatedAt().toLocalDate();
        if (originalDate != null && LocalDate.now().isAfter(originalDate.plusDays(365))) {
            throw new BadRequestException("Cannot process re-issuance. The original FTJS certification has already expired (1-year validity limit under RA 11261).");
        }

        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null && ftjs.getVerifiedBy() != null) {
            currentUser = ftjs.getVerifiedBy();
        }
        if (currentUser == null) {
            currentUser = userRepository.findAll().stream()
                    .filter(u -> u.getPerson() != null)
                    .findFirst()
                    .orElse(null);
        }

        int currentCount = (ftjs.getIssuanceCount() != null) ? ftjs.getIssuanceCount() : 1;
        int nextIssuanceCount = currentCount + 1;

        ftjs.setIssuanceCount(nextIssuanceCount);
        ftjs.setStatus(FtjsStatus.RE_ISSUANCE);
        FirstTimeJobSeeker updatedFtjs = firstTimeJobSeekerRepository.save(ftjs);

        FirstTimeJobSeekerAffidavitOfLoss affidavit = new FirstTimeJobSeekerAffidavitOfLoss();
        affidavit.setFtjs(updatedFtjs);
        affidavit.setReason(request.reason());
        affidavit.setDateOfLoss(request.dateOfLoss());
        affidavit.setOrNumber(request.orNumber());
        affidavit.setIssuanceNumber(nextIssuanceCount);
        affidavit.setCreatedBy(currentUser);
        affidavit.setAffidavitFiles(request.affidavit());

        FirstTimeJobSeekerAffidavitOfLoss savedAffidavit = firstTimeJobSeekerAffidavitOfLossRepository.save(affidavit);

        FirstTimeJobSeekerTimeLine timeline = new FirstTimeJobSeekerTimeLine();
        timeline.setTimelineType(TimeLineType.RE_ISSUANCE);
        timeline.setTitle("Certificate Re-issued");
        timeline.setEventDate(LocalDateTime.now());
        timeline.setFtjs(updatedFtjs);
        timeline.setDescription("Re-issued FTJS Certificate (Copy #" + nextIssuanceCount + ") under OR #" + request.orNumber());
        timeline.setCreatedBy(currentUser);

        firstTimeJobSeekerTimeLineRepository.save(timeline);

        return mapToDto(savedAffidavit);

    }

    private ReIssuanceRequestDto mapToDto(FirstTimeJobSeekerAffidavitOfLoss affidavit) {
        return new ReIssuanceRequestDto(
                affidavit.getFtjs().getId(),
                affidavit.getReason(),
                affidavit.getDateOfLoss(),
                affidavit.getOrNumber(),
                affidavit.getAffidavitFiles()
        );
    }



    @Transactional(readOnly = true)
    /**
     * Retrieves complete re-issuance history records for a given FTJS application.
     */
    public List<ReIssuanceHistoryDto> getReIssuanceHistory(Long ftjsId) {
        if (!firstTimeJobSeekerRepository.existsById(ftjsId)) {
            throw new ResourceNotFoundException("FTJS record not found with id: " + ftjsId);
        }

        return firstTimeJobSeekerAffidavitOfLossRepository.findByFtjsIdOrderByIssuanceNumberDesc(ftjsId)
                .stream()
                .map(affidavit -> new ReIssuanceHistoryDto(
                        affidavit.getId(),
                        affidavit.getIssuanceNumber(),
                        affidavit.getDateOfLoss(),
                        affidavit.getReason(),
                        affidavit.getOrNumber(),
                        affidavit.getAffidavitFiles(),
                        affidavit.getCreatedAt(),
                        formatResidentFullName(affidavit.getFtjs()),
                        resolveIssuingOfficerName(affidavit)
                ))
                .toList();
    }

    private String formatResidentFullName(FirstTimeJobSeeker ftjs) {
        if (ftjs == null || ftjs.getResident() == null || ftjs.getResident().getPerson() == null) {
            return "N/A";
        }
        var person = ftjs.getResident().getPerson();
        StringBuilder sb = new StringBuilder();
        if (person.getFirstName() != null && !person.getFirstName().isBlank()) sb.append(person.getFirstName().trim());
        if (person.getMiddleName() != null && !person.getMiddleName().isBlank()) sb.append(" ").append(person.getMiddleName().trim());
        if (person.getLastName() != null && !person.getLastName().isBlank()) sb.append(" ").append(person.getLastName().trim());
        if (person.getSuffix() != null && !person.getSuffix().isBlank()) sb.append(" ").append(person.getSuffix().trim());
        return sb.toString().trim();
    }


    private String formatUserFullName(User user) {
        if (user == null) {
            return "System";
        }
        if (user.getPerson() == null) {
            return user.getUsername() != null && !user.getUsername().isBlank() ? user.getUsername() : "System";
        }
        StringBuilder sb = new StringBuilder();

        if (user.getPerson().getFirstName() != null && !user.getPerson().getFirstName().isBlank()) {
            sb.append(user.getPerson().getFirstName().trim());
        }
        if (user.getPerson().getMiddleName() != null && !user.getPerson().getMiddleName().isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(user.getPerson().getMiddleName().trim());
        }
        if (user.getPerson().getLastName() != null && !user.getPerson().getLastName().isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(user.getPerson().getLastName().trim());
        }
        if (user.getPerson().getSuffix() != null && !user.getPerson().getSuffix().isBlank()) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(user.getPerson().getSuffix().trim());
        }

        return sb.isEmpty() ? (user.getUsername() != null ? user.getUsername() : "System") : sb.toString();
    }


    @Transactional(readOnly = true)
    /**
     * Retrieves historical timeline audit events for a given FTJS application.
     */
    public List<FtjsTimelineDto> getFtjsTimeline(Long ftjsId) {
        if (!firstTimeJobSeekerRepository.existsById(ftjsId)) {
            throw new ResourceNotFoundException("FTJS record not found with id: " + ftjsId);
        }

        return firstTimeJobSeekerTimeLineRepository.findByFtjsIdOrderByEventDateDesc(ftjsId)
                .stream()
                .map(timeline -> new FtjsTimelineDto(
                        timeline.getId(),
                        timeline.getTitle(),
                        timeline.getDescription(),
                        timeline.getTimelineType(),
                        timeline.getEventDate(),
                        formatUserFullName(timeline.getCreatedBy())
                ))
                .toList();
    }

    private String resolveIssuingOfficerName(FirstTimeJobSeekerAffidavitOfLoss affidavit) {
        if (affidavit == null) return "Barangay Official";
        
        String residentName = formatResidentFullName(affidavit.getFtjs());

        // 1. Check affidavit createdBy user
        if (affidavit.getCreatedBy() != null) {
            String name = formatUserFullName(affidavit.getCreatedBy());
            if (name != null && !name.isBlank() && !"System".equalsIgnoreCase(name)) {
                if (!name.equalsIgnoreCase(residentName)) {
                    return name;
                }
            }
        }

        // 2. Check FTJS record verifiedBy user
        if (affidavit.getFtjs() != null && affidavit.getFtjs().getVerifiedBy() != null) {
            String name = formatUserFullName(affidavit.getFtjs().getVerifiedBy());
            if (name != null && !name.isBlank()) {
                if (!name.equalsIgnoreCase(residentName)) {
                    return name;
                }
            }
        }

        // 3. Fallback to currently authenticated logged-in user if available
        User currentUser = SecurityUtils.getCurrentUser();
        if (currentUser != null) {
            String name = formatUserFullName(currentUser);
            if (name != null && !name.isBlank() && !"System".equalsIgnoreCase(name) && !name.equalsIgnoreCase(residentName)) {
                return name;
            }
        }

        return "Barangay Official";
    }
}
