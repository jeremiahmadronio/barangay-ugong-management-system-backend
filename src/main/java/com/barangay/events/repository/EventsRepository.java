package com.barangay.events.repository;

import com.barangay.events.model.Events;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventsRepository extends JpaRepository<Events, Long> {

    long countByStatus(String status);

    long countByStartDateBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT e FROM Events e WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           " LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.location) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.category) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(e.organizer) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:category IS NULL OR :category = '' OR :category = 'all' OR LOWER(e.category) = LOWER(:category)) AND " +
           "(:status IS NULL OR :status = '' OR :status = 'all' OR LOWER(e.status) = LOWER(:status)) " +
           "ORDER BY e.startDate ASC")
    List<Events> findFilteredEvents(
            @Param("search") String search,
            @Param("category") String category,
            @Param("status") String status
    );

    @Query("SELECT e FROM Events e WHERE e.startDate >= :now ORDER BY e.startDate ASC")
    List<Events> findUpcomingEventsFrom(@Param("now") LocalDateTime now);
}
