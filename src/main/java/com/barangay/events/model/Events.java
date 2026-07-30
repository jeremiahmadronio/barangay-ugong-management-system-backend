package com.barangay.events.model;


import com.barangay.user_management.user.model.User;
import com.barangay.zshared.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "events")
@Getter @Setter
@AllArgsConstructor @NoArgsConstructor
@Builder
public class Events extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "event_date")
    private LocalDateTime eventDate;

    @Builder.Default
    @Column(name = "duration_days")
    private Integer durationDays = 1;

    @Column(nullable = false)
    private String time;

    @Column(nullable = false)
    private String location;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(name = "expected_attendees")
    private Integer expectedAttendees;

    @Column
    private String status; // Upcoming, Ongoing, Completed, Cancelled

    private String organizer;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Column(name = "target_audience")
    private String targetAudience;

    @Builder.Default
    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_id")
    private User updatedBy;


}
