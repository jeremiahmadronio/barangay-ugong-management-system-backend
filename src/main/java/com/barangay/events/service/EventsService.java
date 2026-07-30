package com.barangay.events.service;

import com.barangay.events.dto.*;

import java.util.List;

public interface EventsService {

    EventStatsResponse getEventStats();

    List<EventResponse> getFilteredEventsTable(String search, String category, String status);

    EventResponse getEventById(Long id);

    EventResponse createEvent(EventCreateRequest request);

    EventResponse updateEvent(Long id, EventUpdateRequest request);

    EventResponse updateEventStatus(Long id, EventStatusUpdateRequest request);

    void deleteEvent(Long id);
}
