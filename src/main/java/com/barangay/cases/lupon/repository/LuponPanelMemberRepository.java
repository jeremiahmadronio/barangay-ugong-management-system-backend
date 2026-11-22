package com.barangay.cases.lupon.repository;

import com.barangay.cases.lupon.model.LuponPanelMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for managing {@link LuponPanelMember} entity operations.
 */
@Repository
public interface LuponPanelMemberRepository extends JpaRepository<LuponPanelMember, Long> {

    List<LuponPanelMember> findByCasesId(Long caseId);
}
