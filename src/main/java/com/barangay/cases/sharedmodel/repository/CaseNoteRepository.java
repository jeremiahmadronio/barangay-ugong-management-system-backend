package com.barangay.cases.sharedmodel.repository;

import com.barangay.cases.sharedmodel.CaseNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaseNoteRepository extends JpaRepository<CaseNote, Long> {
}
