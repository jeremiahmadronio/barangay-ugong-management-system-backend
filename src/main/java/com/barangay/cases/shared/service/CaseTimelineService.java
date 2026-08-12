package com.barangay.cases.shared.service;

import com.barangay.cases.blotter.dto.TimelineEventResponse;
import com.barangay.cases.shared.model.Case;
import com.barangay.cases.shared.model.CaseTimeline;
import com.barangay.cases.shared.repository.CaseTimelineRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.enumerated.TimelineEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseTimelineService {

    private final CaseTimelineRepository caseTimelineRepository;

    /**
     * Logs a reusable timeline event for any case type (Blotter, VAWC, Lupon, etc.).
     */
    @Transactional
    public CaseTimeline logEvent(Case caseEntity, TimelineEventType eventType, String title, String description, User performedBy) {
        CaseTimeline timeline = new CaseTimeline();
        timeline.setCases(caseEntity);
        timeline.setEventType(eventType != null ? eventType : TimelineEventType.STATUS_CHANGED);
        timeline.setTitle(title);
        timeline.setDescription(description);
        timeline.setPerformedBy(performedBy);

        return caseTimelineRepository.save(timeline);
    }

    /**
     * Retrieves all timeline events for a given case ID ordered by most recent first.
     */
    @Transactional(readOnly = true)
    public List<TimelineEventResponse> getTimelineForCase(Long caseId) {
        return caseTimelineRepository.findByCasesIdOrderByEventDateDesc(caseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TimelineEventResponse toResponse(CaseTimeline t) {
        String performedBy = null;
        if (t.getPerformedBy() != null) {
            User u = t.getPerformedBy();
            if (u.getPerson() != null) {
                String first = u.getPerson().getFirstName() != null ? u.getPerson().getFirstName() : "";
                String last  = u.getPerson().getLastName()  != null ? u.getPerson().getLastName()  : "";
                performedBy = (first + " " + last).trim();
            } else {
                performedBy = u.getUsername() != null ? u.getUsername() : u.getSystemEmail();
            }
        }

        return TimelineEventResponse.builder()
                .id(t.getId())
                .caseId(t.getCases() != null ? t.getCases().getId() : null)
                .eventType(t.getEventType() != null ? t.getEventType().name() : null)
                .title(t.getTitle())
                .description(t.getDescription())
                .performedBy(performedBy)
                .eventDate(t.getEventDate())
                .build();
    }
}
