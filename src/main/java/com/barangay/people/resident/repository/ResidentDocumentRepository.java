package com.barangay.people.resident.repository;

import com.barangay.people.resident.model.ResidentDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResidentDocumentRepository extends JpaRepository<ResidentDocument, Long> {
    List<ResidentDocument> findByResidentResidentId(Long residentId);
}
