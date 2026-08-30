package com.barangay.cases.lupon.repository;

import com.barangay.cases.lupon.model.PanelAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PanelAttendanceRepository extends JpaRepository<PanelAttendance, Long> {

    List<PanelAttendance> findBySessionId(Long sessionId);

    List<PanelAttendance> findBySessionIdIn(List<Long> sessionIds);

    Optional<PanelAttendance> findBySessionIdAndPanelMemberId(Long sessionId, Long panelMemberId);
}
