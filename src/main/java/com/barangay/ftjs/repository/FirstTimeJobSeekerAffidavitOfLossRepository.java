package com.barangay.ftjs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barangay.ftjs.model.FirstTimeJobSeekerAffidavitOfLoss;

@Repository
public interface FirstTimeJobSeekerAffidavitOfLossRepository extends JpaRepository<FirstTimeJobSeekerAffidavitOfLoss, Long> {

    List<FirstTimeJobSeekerAffidavitOfLoss> findByFtjsIdOrderByCreatedAtDesc(Long ftjsId);
}
