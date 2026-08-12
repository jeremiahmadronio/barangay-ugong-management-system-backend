package com.barangay.cases.shared.repository;

import com.barangay.cases.shared.model.CaseNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseNoteRepository extends JpaRepository<CaseNote, Long> {
    List<CaseNote> findByCasesIdOrderByCreatedAtDesc(Long caseId);
}

