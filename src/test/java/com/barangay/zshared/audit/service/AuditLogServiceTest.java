package com.barangay.zshared.audit.service;

import com.barangay.zshared.audit.dto.AuditLogResponse;
import com.barangay.zshared.audit.model.AuditLogs;
import com.barangay.zshared.audit.repository.AuditLogsRepository;
import com.barangay.zshared.mapper.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogsRepository auditLogsRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AuditLogService auditLogService;

    private AuditLogs sampleLog;

    @BeforeEach
    void setUp() {
        sampleLog = AuditLogs.builder()
                .id(1L)
                .actionTaken("TEST_ACTION")
                .ipAddress("127.0.0.1")
                .build();
    }

    @Test
    void getAuditLogs_shouldReturnPageResponseWithCorrectPagination() {
        Page<AuditLogs> page = new PageImpl<>(List.of(sampleLog), org.springframework.data.domain.PageRequest.of(0, 5), 10);
        when(auditLogsRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        PageResponse<AuditLogResponse> response = auditLogService.getAuditLogs(null, null, null, null, 0, 5);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(0, response.getPageNumber());
        assertEquals(0, response.getNumber());
        assertEquals(5, response.getPageSize());
        assertEquals(5, response.getSize());
        assertEquals(10, response.getTotalElements());
        assertEquals(2, response.getTotalPages());
    }
}
