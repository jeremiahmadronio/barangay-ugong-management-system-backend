package com.barangay.contentmanagement.repository;

import com.barangay.contentmanagement.model.Official;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfficialRepository extends JpaRepository<Official, Long> {

    List<Official> findAllByOrderByCreatedAtDesc();

    @Query("SELECT o FROM Official o WHERE (o.isArchived = false OR o.isArchived IS NULL) ORDER BY o.createdAt DESC")
    List<Official> findAllActive();
}
