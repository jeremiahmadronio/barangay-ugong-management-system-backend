package com.barangay.zshared.scheduler;

import com.barangay.events.model.Events;
import com.barangay.events.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventStatusScheduler {

    private final EventsRepository eventsRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void updateEventStatuses() {
        log.info("Running scheduled check for Event Statuses...");
        LocalDate today = LocalDate.now();

        try {
            List<Events> eventsList = eventsRepository.findAll();
            int updatedCount = 0;

            for (Events event : eventsList) {
                if (event.getEventDate() == null) continue;

                LocalDate eventDate = event.getEventDate().toLocalDate();
                String calculatedStatus;
                if (eventDate.isBefore(today)) {
                    calculatedStatus = "Completed";
                } else if (eventDate.isEqual(today)) {
                    calculatedStatus = "Ongoing";
                } else {
                    calculatedStatus = "Upcoming";
                }

                if (!calculatedStatus.equalsIgnoreCase(event.getStatus())) {
                    event.setStatus(calculatedStatus);
                    eventsRepository.save(event);
                    updatedCount++;
                }
            }

            log.info("Event Status Scheduler completed successfully. Updated {} events.", updatedCount);
        } catch (Exception e) {
            log.error("Error updating event statuses: {}", e.getMessage(), e);
        }
    }
}
