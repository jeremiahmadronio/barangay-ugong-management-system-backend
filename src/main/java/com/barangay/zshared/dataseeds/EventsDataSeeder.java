package com.barangay.zshared.dataseeds;

import com.barangay.events.model.Events;
import com.barangay.events.repository.EventsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(10)
@RequiredArgsConstructor
public class EventsDataSeeder implements CommandLineRunner {

    private final EventsRepository eventsRepository;

    @Override
    public void run(String... args) {
        eventsRepository.deleteAll();

        LocalDateTime targetDate = LocalDateTime.of(2026, 7, 29, 8, 0);

        Events e1 = Events.builder()
                .title("Senior Citizens Health Screening")
                .startDate(targetDate.minusDays(5))
                .endDate(targetDate.plusDays(5))
                .eventDate(targetDate)
                .durationDays(1)
                .time("8:00 AM - 12:00 PM")
                .location("Barangay Health Center")
                .description("Free comprehensive health checkup, blood pressure monitoring, and medical consultation for all senior residents of Barangay Ugong.")
                .category("Health")
                .expectedAttendees(120)
                .status("Upcoming")
                .organizer("Health Committee - Senior Citizens")
                .contactPerson("Dr. Maria Santos")
                .contactNumber("0917-555-0192")
                .requirements("Senior Citizen ID, Barangay Clearance")
                .targetAudience("Senior Citizens (60+)")
                .isFeatured(true)
                .build();

        Events e2 = Events.builder()
                .title("Livelihood & Skills Training Workshop")
                .startDate(targetDate.minusDays(5))
                .endDate(targetDate.plusDays(5))
                .eventDate(targetDate)
                .durationDays(2)
                .time("1:00 PM - 4:00 PM")
                .location("Barangay Multipurpose Hall")
                .description("Hands-on soap making and food processing livelihood seminar for interested residents and stay-at-home mothers.")
                .category("Community")
                .expectedAttendees(80)
                .status("Upcoming")
                .organizer("Livelihood & Gender Committee")
                .contactPerson("Kagawad Elena Cruz")
                .contactNumber("0918-333-4455")
                .requirements("Barangay Resident ID")
                .targetAudience("All Residents")
                .isFeatured(false)
                .build();

        Events e3 = Events.builder()
                .title("SK Youth Anti-Drug & Leadership Symposium")
                .startDate(targetDate.minusDays(5))
                .endDate(targetDate.plusDays(5))
                .eventDate(targetDate)
                .durationDays(1)
                .time("5:00 PM - 7:00 PM")
                .location("Barangay Covered Court")
                .description("Interactive youth empowerment seminar focusing on substance abuse prevention, mental health, and civic engagement.")
                .category("Youth")
                .expectedAttendees(150)
                .status("Upcoming")
                .organizer("Sangguniang Kabataan (SK)")
                .contactPerson("SK Chairman Mark Reyes")
                .contactNumber("0919-777-8899")
                .requirements("Valid Student ID / Resident ID")
                .targetAudience("Youth (15 - 30)")
                .isFeatured(true)
                .build();

        Events e4 = Events.builder()
                .title("Inter-Sitio Volleyball & Basketball League")
                .startDate(LocalDateTime.of(2026, 8, 1, 16, 0))
                .endDate(LocalDateTime.of(2026, 8, 5, 20, 0))
                .eventDate(LocalDateTime.of(2026, 8, 1, 16, 0))
                .durationDays(3)
                .time("4:00 PM - 8:00 PM")
                .location("Barangay Gymnasium")
                .description("Opening ceremony and 3-day inter-sitio sports tournament.")
                .category("Sports")
                .expectedAttendees(200)
                .status("Upcoming")
                .organizer("Sports Committee")
                .contactPerson("Kagawad Benjie")
                .contactNumber("0920-111-2233")
                .requirements("Team Registration Form")
                .targetAudience("Youth & Adults")
                .isFeatured(false)
                .build();

        eventsRepository.saveAll(List.of(e1, e2, e3, e4));
    }
}
