package com.barangay.ftjs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barangay.ftjs.model.FirstTimeJobSeekerNotes;

@Repository
/**
 * Spring Data JPA Repository for FTJS administrative notes.
 */
public interface FirstTimeJobSeekerNotesRepository extends JpaRepository<FirstTimeJobSeekerNotes, Long> {

    List<FirstTimeJobSeekerNotes> findByFtjsIdOrderByCreatedAtDesc(Long ftjsId);


}
