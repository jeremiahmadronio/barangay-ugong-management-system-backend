package com.barangay.cases.lupon.repository;

import com.barangay.cases.lupon.model.PanelAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PanelAttendanceRepository extends JpaRepository<PanelAttendance, Long> {
}
