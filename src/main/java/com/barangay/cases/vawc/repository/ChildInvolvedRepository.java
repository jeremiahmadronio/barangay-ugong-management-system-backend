package com.barangay.cases.vawc.repository;

import com.barangay.cases.vawc.model.ChildInvolved;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChildInvolvedRepository extends JpaRepository<ChildInvolved, Long> {
}
