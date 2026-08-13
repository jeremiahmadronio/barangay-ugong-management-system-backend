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
public class FtjsReIssuanceService {

    private final FirstTimeJobSeekerRepository firstTimeJobSeekerRepository;
    private final FirstTimeJobSeekerAffidavitOfLossRepository firstTimeJobSeekerAffidavitOfLossRepository;
    private final FirstTimeJobSeekerTimeLineRepository  firstTimeJobSeekerTimeLineRepository;


    @Transactional
    public ReIssuanceRequestDto createReIssuanceRequest(Long ftjsId,ReIssuanceRequestDto request) throws BadRequestException {

        FirstTimeJobSeeker ftjs = firstTimeJobSeekerRepository.findById(ftjsId).orElseThrow(()
        -> new ResourceNotFoundException("ftjs not found"));

        LocalDate originalDate = ftjs.getDateSubmitted() != null ? ftjs.getDateSubmitted() : ftjs.getCreatedAt().toLocalDate();
        if (originalDate != null && LocalDate.now().isAfter(originalDate.plusDays(365))) {
            throw new BadRequestException("Cannot process re-issuance. The original FTJS certification has already expired (1-year validity limit under RA 11261).");
        }

        User currentUser = SecurityUtils.getCurrentUser();

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
                        formatUserFullName(affidavit.getCreatedBy())
                ))
                .toList();
    }


    private String formatUserFullName(User user) {
        if (user == null) {
            return "System";
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

        return sb.isEmpty() ? "System" : sb.toString();
    }


    @Transactional(readOnly = true)
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

}
