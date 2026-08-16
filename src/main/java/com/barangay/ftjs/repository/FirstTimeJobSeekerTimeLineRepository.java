package com.barangay.ftjs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.barangay.ftjs.model.FirstTimeJobSeekerTimeLine;

@Repository
/**
 * Spring Data JPA Repository for FTJS event timeline entries.
 */
public interface FirstTimeJobSeekerTimeLineRepository extends JpaRepository<FirstTimeJobSeekerTimeLine, Long> {

    List<FirstTimeJobSeekerTimeLine> findByFtjsIdOrderByEventDateDesc(Long ftjsId);

}
