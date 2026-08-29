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

    /**
     * Finds all assigned Pangkat panel members for a specific case by case ID.
     *
     * @param caseId primary key of the case
     * @return list of {@link LuponPanelMember} entities assigned to the case
     */
    List<LuponPanelMember> findByCasesId(Long caseId);

    /**
     * Finds all assigned Pangkat panel members for a specific case by case number.
     *
     * @param caseNumber official case tracking number
     * @return list of {@link LuponPanelMember} entities assigned to the case
     */
    List<LuponPanelMember> findByCasesCaseNumber(String caseNumber);
}
