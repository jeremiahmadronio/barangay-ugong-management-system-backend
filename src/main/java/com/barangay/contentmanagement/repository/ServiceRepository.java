package com.barangay.contentmanagement.repository;

import com.barangay.contentmanagement.model.BarangayService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<BarangayService, Long> {

    @Query("SELECT s FROM BarangayService s WHERE (s.isArchived = false OR s.isArchived IS NULL) ORDER BY s.createdAt DESC")
    List<BarangayService> findAllActive();

    @Query("SELECT s FROM BarangayService s WHERE s.isPublished = true AND (s.isArchived = false OR s.isArchived IS NULL) ORDER BY s.createdAt DESC")
    List<BarangayService> findPublicActive();
}
