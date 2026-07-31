package com.barangay.cases.lupon.repository;

import com.barangay.cases.lupon.model.LuponPanelMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LuponPanelMemberRepository extends JpaRepository<LuponPanelMember, Long> {
}
