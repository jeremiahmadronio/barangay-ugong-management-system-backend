package com.barangay.events.service.impl;

import com.barangay.events.dto.*;
import com.barangay.events.model.Events;
import com.barangay.events.repository.EventsRepository;
import com.barangay.events.service.EventsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventsServiceImpl implements EventsService {

    private final EventsRepository eventsRepository;

    @Override
    @Transactional(readOnly = true)
    public EventStatsResponse getEventStats() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthStart = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime monthEnd = monthStart.plusMonths(1).minusNanos(1);

        long upcomingEvents = eventsRepository.countByStatus("Upcoming");
        long thisMonthEvents = eventsRepository.countByStartDateBetween(monthStart, monthEnd);

        List<Events> allEvents = eventsRepository.findAll();
        long expectedAttendeesTotal = allEvents.stream()
                .mapToLong(e -> e.getExpectedAttendees() != null ? e.getExpectedAttendees() : 0)
                .sum();

        List<Events> upcomingList = eventsRepository.findUpcomingEventsFrom(now);
        String nextEventTitle = "No upcoming events";
        String nextEventDate = "—";

        if (!upcomingList.isEmpty()) {
            Events next = upcomingList.get(0);
            nextEventTitle = next.getTitle();
            if (next.getEventDate() != null) {
                nextEventDate = next.getEventDate().format(DateTimeFormatter.ofPattern("MMM dd"));
            } else if (next.getStartDate() != null) {
                nextEventDate = next.getStartDate().format(DateTimeFormatter.ofPattern("MMM dd"));
            } else {
                nextEventDate = "TBA";
            }
        }

        long activeDepts = allEvents.stream()
                .map(Events::getOrganizer)
                .filter(o -> o != null && !o.isBlank())
                .distinct()
                .count();

        return new EventStatsResponse(
                upcomingEvents,
                thisMonthEvents,
                expectedAttendeesTotal,
                nextEventTitle,
                nextEventDate,
                expectedAttendeesTotal,
                Math.max(activeDepts, 2)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventResponse> getFilteredEventsTable(String search, String category, String status) {
        List<Events> events = eventsRepository.findFilteredEvents(search, category, status);
        return events.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        Events event = eventsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + id));
        return mapToResponse(event);
    }

    @Override
    @Transactional
    public EventResponse createEvent(EventCreateRequest request) {
        LocalDateTime evDate = request.eventDate() != null ? request.eventDate() : request.startDate();
        Integer durDays = request.durationDays() != null ? request.durationDays() : 1;
        String autoStatus = computeAutoStatus(evDate, durDays, request.status());

        Events event = Events.builder()
                .title(request.title())
                .startDate(request.startDate() != null ? request.startDate() : LocalDateTime.now())
                .endDate(request.endDate())
                .eventDate(evDate)
                .durationDays(durDays)
                .time(request.time() != null && !request.time().isBlank() ? request.time() : "8:00 AM - 12:00 PM")
                .location(request.location())
                .description(request.description())
                .category(request.category())
                .expectedAttendees(request.expectedAttendees() != null ? request.expectedAttendees() : 100)
                .status(autoStatus)
                .organizer(request.organizer())
                .contactPerson(request.contactPerson())
                .contactNumber(request.contactNumber())
                .requirements(request.requirements())
                .targetAudience(request.targetAudience())
                .isFeatured(Boolean.TRUE.equals(request.isFeatured()))
                .build();

        Events saved = eventsRepository.save(event);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public EventResponse updateEvent(Long id, EventUpdateRequest request) {
        Events event = eventsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + id));

        if (request.title() != null && !request.title().isBlank()) event.setTitle(request.title());
        if (request.startDate() != null) event.setStartDate(request.startDate());
        if (request.endDate() != null) event.setEndDate(request.endDate());
        if (request.eventDate() != null) event.setEventDate(request.eventDate());
        if (request.durationDays() != null) event.setDurationDays(request.durationDays());
        if (request.time() != null && !request.time().isBlank()) event.setTime(request.time());
        if (request.location() != null && !request.location().isBlank()) event.setLocation(request.location());
        if (request.description() != null && !request.description().isBlank()) event.setDescription(request.description());
        if (request.category() != null && !request.category().isBlank()) event.setCategory(request.category());
        if (request.expectedAttendees() != null) event.setExpectedAttendees(request.expectedAttendees());
        if (request.organizer() != null) event.setOrganizer(request.organizer());
        if (request.contactPerson() != null) event.setContactPerson(request.contactPerson());
        if (request.contactNumber() != null) event.setContactNumber(request.contactNumber());
        if (request.requirements() != null) event.setRequirements(request.requirements());
        if (request.targetAudience() != null) event.setTargetAudience(request.targetAudience());
        if (request.isFeatured() != null) event.setIsFeatured(request.isFeatured());

        // Recompute status based on eventDate and durationDays
        String autoStatus = computeAutoStatus(event.getEventDate(), event.getDurationDays(), request.status());
        event.setStatus(autoStatus);

        Events updated = eventsRepository.save(event);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public EventResponse updateEventStatus(Long id, EventStatusUpdateRequest request) {
        Events event = eventsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found with ID: " + id));
        event.setStatus(request.status());
        Events updated = eventsRepository.save(event);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteEvent(Long id) {
        if (!eventsRepository.existsById(id)) {
            throw new IllegalArgumentException("Event not found with ID: " + id);
        }
        eventsRepository.deleteById(id);
    }

    private String computeAutoStatus(LocalDateTime eventDate, Integer durationDays, String fallbackStatus) {
        if (fallbackStatus != null && fallbackStatus.equalsIgnoreCase("Cancelled")) {
            return "Cancelled";
        }
        if (eventDate == null) {
            return fallbackStatus != null ? fallbackStatus : "Upcoming";
        }
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate eventStartDay = eventDate.toLocalDate();
        int days = (durationDays != null && durationDays > 0) ? durationDays : 1;
        java.time.LocalDate eventEndDay = eventStartDay.plusDays(days);

        if (today.isBefore(eventStartDay)) {
            return "Upcoming";
        } else if (today.isEqual(eventEndDay) || today.isAfter(eventEndDay)) {
            return "Completed";
        } else {
            return "Ongoing";
        }
    }

    private EventResponse mapToResponse(Events event) {
        int registeredCount = (int) Math.round((event.getExpectedAttendees() != null ? event.getExpectedAttendees() : 100) * 0.72);

        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getStartDate(),
                event.getEndDate(),
                event.getEventDate(),
                event.getDurationDays(),
                event.getTime(),
                event.getLocation(),
                event.getDescription(),
                event.getCategory(),
                event.getExpectedAttendees(),
                registeredCount,
                event.getStatus(),
                event.getOrganizer(),
                event.getContactPerson(),
                event.getContactNumber(),
                event.getRequirements(),
                event.getTargetAudience(),
                event.getIsFeatured(),
                event.getCreatedAt()
        );
    }
}
