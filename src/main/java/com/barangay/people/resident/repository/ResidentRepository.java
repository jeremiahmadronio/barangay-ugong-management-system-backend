package com.barangay.people.resident.repository;

import com.barangay.people.resident.model.Resident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResidentRepository extends JpaRepository<Resident, Long> {
    Optional<Resident> findByBarangayIdNumber(String barangayIdNumber);
    Optional<Resident> findByPersonId(Long personId);
    boolean existsByBarangayIdNumber(String barangayIdNumber);
}
