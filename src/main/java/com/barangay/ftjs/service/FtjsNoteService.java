package com.barangay.ftjs.service;

import com.barangay.ftjs.dto.FtjsNoteRequest;
import com.barangay.ftjs.dto.FtjsNoteResponse;
import com.barangay.ftjs.model.FirstTimeJobSeeker;
import com.barangay.ftjs.model.FirstTimeJobSeekerNotes;
import com.barangay.ftjs.model.FirstTimeJobSeekerTimeLine;
import com.barangay.ftjs.repository.FirstTimeJobSeekerNotesRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerTimeLineRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.enumerated.TimeLineType;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * Service class managing administrative notes attached to FTJS records.
 */
public class FtjsNoteService {

    private final FirstTimeJobSeekerNotesRepository ftjsNotesRepository;
    private final FirstTimeJobSeekerRepository ftjsRepository;
    private final FirstTimeJobSeekerTimeLineRepository ftjsTimelineRepository;


    @Transactional
    public FtjsNoteResponse addNote(Long ftjsId, FtjsNoteRequest request) {
        FirstTimeJobSeeker ftjs = ftjsRepository.findById(ftjsId)
                .orElseThrow(() -> new ResourceNotFoundException("FTJS record not found with id: " + ftjsId));

        User currentUser = SecurityUtils.getCurrentUser();

        FirstTimeJobSeekerNotes note = new FirstTimeJobSeekerNotes();
        note.setFtjs(ftjs);
        note.setNotes(request.getNote());
        note.setCreatedBy(currentUser);

        FirstTimeJobSeekerNotes savedNote = ftjsNotesRepository.save(note);

        FirstTimeJobSeekerTimeLine timeline = new FirstTimeJobSeekerTimeLine();
        timeline.setFtjs(ftjs);
        timeline.setTimelineType(TimeLineType.NOTES_ADDED);
        timeline.setTitle("Note Added");
        timeline.setDescription(request.getNote());
        timeline.setEventDate(LocalDateTime.now());
        timeline.setCreatedBy(currentUser);

        ftjsTimelineRepository.save(timeline);


        return mapToResponse(savedNote);
    }


    @Transactional(readOnly = true)
    /**
     * Retrieves all administrative notes attached to an FTJS application.
     */
    public List<FtjsNoteResponse> getNotesByFtjsId(Long ftjsId) {
        if (!ftjsRepository.existsById(ftjsId)) {
            throw new ResourceNotFoundException("FTJS record not found with id: " + ftjsId);
        }

        return ftjsNotesRepository.findByFtjsIdOrderByCreatedAtDesc(ftjsId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private FtjsNoteResponse mapToResponse(FirstTimeJobSeekerNotes note) {
        return FtjsNoteResponse.builder()
                .id(note.getId())
                .ftjsId(note.getFtjs().getId())
                .note(note.getNotes())
                .createdBy(formatUserFullName(note.getCreatedBy()))
                .createdAt(note.getCreatedAt())
                .build();
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
}