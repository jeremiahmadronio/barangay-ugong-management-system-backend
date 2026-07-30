package com.barangay.events.controller;

import com.barangay.events.dto.*;
import com.barangay.events.service.EventsService;
import com.barangay.zshared.audit.annotations.LogAudit;
import com.barangay.zshared.enumerated.AuditModule;
import com.barangay.zshared.enumerated.Severity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventsController {

    private final EventsService eventsService;

    @GetMapping("/stats")
    public ResponseEntity<EventStatsResponse> getEventStats() {
        return ResponseEntity.ok(eventsService.getEventStats());
    }

    @GetMapping("/table")
    public ResponseEntity<List<EventResponse>> getEventsTable(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(eventsService.getFilteredEventsTable(search, category, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventsService.getEventById(id));
    }

    @PostMapping
    @LogAudit(
            module = AuditModule.USER_MANAGEMENT,
            action = "CREATE_EVENT",
            severity = Severity.INFO,
            captureNewValue = true
    )
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventsService.createEvent(request));
    }

    @PutMapping("/{id}")
    @LogAudit(
            module = AuditModule.USER_MANAGEMENT,
            action = "UPDATE_EVENT",
            severity = Severity.INFO,
            captureOldValue = true,
            captureNewValue = true
    )
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody EventUpdateRequest request
    ) {
        return ResponseEntity.ok(eventsService.updateEvent(id, request));
    }

    @PatchMapping("/{id}/status")
    @LogAudit(
            module = AuditModule.USER_MANAGEMENT,
            action = "UPDATE_EVENT_STATUS",
            severity = Severity.WARNING,
            captureOldValue = true,
            captureNewValue = true
    )
    public ResponseEntity<EventResponse> updateEventStatus(
            @PathVariable Long id,
            @Valid @RequestBody EventStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(eventsService.updateEventStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @LogAudit(
            module = AuditModule.USER_MANAGEMENT,
            action = "DELETE_EVENT",
            severity = Severity.CRITICAL
    )
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        eventsService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
