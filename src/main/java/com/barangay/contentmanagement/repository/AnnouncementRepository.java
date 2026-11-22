package com.barangay.contentmanagement.repository;

import com.barangay.contentmanagement.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing public and administrative announcements.
 */
@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query("SELECT a FROM Announcement a WHERE (a.isArchived = false OR a.isArchived IS NULL) ORDER BY a.createdAt DESC")
    List<Announcement> findAllActive();

    @Query("SELECT a FROM Announcement a WHERE a.isPublished = true AND (a.isArchived = false OR a.isArchived IS NULL) ORDER BY a.createdAt DESC")
    List<Announcement> findPublicActive();
}
