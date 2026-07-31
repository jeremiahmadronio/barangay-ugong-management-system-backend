package com.barangay.cases.lupon.repository;

import com.barangay.cases.lupon.model.LuponReferral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LuponReferralRepository extends JpaRepository<LuponReferral, Long> {
}
