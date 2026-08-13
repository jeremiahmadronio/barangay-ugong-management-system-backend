package com.barangay.ftjs.service;

import com.barangay.ftjs.dto.FtjsNoteRequest;
import com.barangay.ftjs.dto.FtjsNoteResponse;
import com.barangay.ftjs.model.FirstTimeJobSeeker;
import com.barangay.ftjs.model.FirstTimeJobSeekerNotes;
import com.barangay.ftjs.repository.FirstTimeJobSeekerNotesRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.usermanagement.user.model.User;
import com.barangay.zshared.exception.ResourceNotFoundException;
import com.barangay.zshared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FtjsNoteService {

    private final FirstTimeJobSeekerNotesRepository ftjsNotesRepository;
    private final FirstTimeJobSeekerRepository ftjsRepository;


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

        return mapToResponse(savedNote);
    }


    @Transactional(readOnly = true)
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
}