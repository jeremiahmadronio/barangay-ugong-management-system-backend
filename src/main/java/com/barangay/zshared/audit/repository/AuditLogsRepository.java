package com.barangay.zshared.audit.repository;

import com.barangay.zshared.audit.model.AuditLogs;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing {@link AuditLogs} persistence and queries.
 */
@Repository
public interface AuditLogsRepository extends JpaRepository<AuditLogs, Long>, JpaSpecificationExecutor<AuditLogs> {

    List<AuditLogs> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AuditLogs> findByOrderByCreatedAtDesc(Pageable pageable);
}
