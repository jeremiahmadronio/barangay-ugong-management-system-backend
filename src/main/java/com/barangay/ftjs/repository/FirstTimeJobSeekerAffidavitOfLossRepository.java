package com.barangay.ftjs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barangay.ftjs.model.FirstTimeJobSeekerAffidavitOfLoss;

@Repository
/**
 * Spring Data JPA Repository for FTJS Affidavit of Loss records.
 */
public interface FirstTimeJobSeekerAffidavitOfLossRepository extends JpaRepository<FirstTimeJobSeekerAffidavitOfLoss, Long> {

    List<FirstTimeJobSeekerAffidavitOfLoss> findByFtjsIdOrderByCreatedAtDesc(Long ftjsId);

    List<FirstTimeJobSeekerAffidavitOfLoss> findByFtjsIdOrderByIssuanceNumberDesc(Long ftjsId);
}
