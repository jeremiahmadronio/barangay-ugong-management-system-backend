package com.barangay.cases.vawc.repository;

import com.barangay.cases.vawc.model.ViolenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ViolenceTypeRepository extends JpaRepository<ViolenceType, Long> {
    Optional<ViolenceType> findByNameIgnoreCase(String name);
}
