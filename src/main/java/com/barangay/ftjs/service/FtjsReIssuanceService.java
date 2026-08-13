package com.barangay.ftjs.service;

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

}
