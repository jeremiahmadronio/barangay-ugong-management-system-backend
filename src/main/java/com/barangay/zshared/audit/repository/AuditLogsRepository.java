package com.barangay.zshared.audit.repository;

import com.barangay.zshared.audit.model.AuditLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogsRepository extends JpaRepository<AuditLogs, Long>, JpaSpecificationExecutor<AuditLogs> {
}
