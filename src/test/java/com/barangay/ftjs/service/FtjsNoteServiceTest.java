package com.barangay.ftjs.service;

import com.barangay.ftjs.dto.FtjsNoteRequest;
import com.barangay.ftjs.dto.FtjsNoteResponse;
import com.barangay.ftjs.model.FirstTimeJobSeeker;
import com.barangay.ftjs.model.FirstTimeJobSeekerNotes;
import com.barangay.ftjs.model.FirstTimeJobSeekerTimeLine;
import com.barangay.ftjs.repository.FirstTimeJobSeekerNotesRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerRepository;
import com.barangay.ftjs.repository.FirstTimeJobSeekerTimeLineRepository;
import com.barangay.zshared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FtjsNoteServiceTest {

    @Mock
    private FirstTimeJobSeekerNotesRepository ftjsNotesRepository;

    @Mock
    private FirstTimeJobSeekerRepository ftjsRepository;

    @Mock
    private FirstTimeJobSeekerTimeLineRepository ftjsTimelineRepository;

    @InjectMocks
    private FtjsNoteService ftjsNoteService;

    @Nested
    @DisplayName("addNote Method Tests")
    class AddNoteTests {

        @Test
        @DisplayName("shouldAddNote_whenFtjsExists")
        void shouldAddNote_whenFtjsExists() {
            // Given
            Long ftjsId = 1L;
            FirstTimeJobSeeker ftjs = new FirstTimeJobSeeker();
            ftjs.setId(ftjsId);

            FtjsNoteRequest request = new FtjsNoteRequest("Resident verified via school ID.");

            FirstTimeJobSeekerNotes savedNote = new FirstTimeJobSeekerNotes();
            savedNote.setId(10L);
            savedNote.setFtjs(ftjs);
            savedNote.setNotes(request.getNote());
            savedNote.setCreatedAt(LocalDateTime.now());

            when(ftjsRepository.findById(ftjsId)).thenReturn(Optional.of(ftjs));
            when(ftjsNotesRepository.save(any(FirstTimeJobSeekerNotes.class))).thenReturn(savedNote);

            // When
            FtjsNoteResponse response = ftjsNoteService.addNote(ftjsId, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getNote()).isEqualTo("Resident verified via school ID.");
            assertThat(response.getFtjsId()).isEqualTo(ftjsId);
            verify(ftjsNotesRepository, times(1)).save(any(FirstTimeJobSeekerNotes.class));
            verify(ftjsTimelineRepository, times(1)).save(any(FirstTimeJobSeekerTimeLine.class));
        }

        @Test
        @DisplayName("shouldThrowResourceNotFoundException_whenFtjsNotFoundForAddNote")
        void shouldThrowResourceNotFoundException_whenFtjsNotFoundForAddNote() {
            // Given
            Long ftjsId = 99L;
            FtjsNoteRequest request = new FtjsNoteRequest("Note text");
            when(ftjsRepository.findById(ftjsId)).thenReturn(Optional.empty());

            // When / Then
            assertThatThrownBy(() -> ftjsNoteService.addNote(ftjsId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("FTJS record not found with id: 99");
        }
    }

    @Nested
    @DisplayName("getNotesByFtjsId Method Tests")
    class GetNotesByFtjsIdTests {

        @Test
        @DisplayName("shouldReturnNotesList_whenFtjsExists")
        void shouldReturnNotesList_whenFtjsExists() {
            // Given
            Long ftjsId = 1L;
            FirstTimeJobSeeker ftjs = new FirstTimeJobSeeker();
            ftjs.setId(ftjsId);

            FirstTimeJobSeekerNotes note = new FirstTimeJobSeekerNotes();
            note.setId(100L);
            note.setFtjs(ftjs);
            note.setNotes("First note");
            note.setCreatedAt(LocalDateTime.now());

            when(ftjsRepository.existsById(ftjsId)).thenReturn(true);
            when(ftjsNotesRepository.findByFtjsIdOrderByCreatedAtDesc(ftjsId)).thenReturn(List.of(note));

            // When
            List<FtjsNoteResponse> notes = ftjsNoteService.getNotesByFtjsId(ftjsId);

            // Then
            assertThat(notes).isNotNull().hasSize(1);
            assertThat(notes.get(0).getNote()).isEqualTo("First note");
            verify(ftjsNotesRepository, times(1)).findByFtjsIdOrderByCreatedAtDesc(ftjsId);
        }
    }
}
