package com.barangay.cases.vawc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.barangay.cases.blotter.service.CaseFileService;
import com.barangay.cases.vawc.dto.VawcCaseDetailResponse;
import com.barangay.cases.vawc.dto.VawcStatsResponse;
import com.barangay.cases.vawc.service.VawcCaseService;

@ExtendWith(MockitoExtension.class)
class VawcCaseControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VawcCaseService vawcCaseService;

    @Mock
    private CaseFileService caseFileService;

    @InjectMocks
    private VawcCaseController vawcCaseController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(vawcCaseController).build();
    }

    @Test
    @DisplayName("GET /api/v1/vawc/stats should return 200 OK with VAWC statistics and trends")
    void getVawcStats_shouldReturnOkWithStatsResponse() throws Exception {
        // Given
        VawcStatsResponse statsResponse = new VawcStatsResponse(
                20L,
                "+15.0% vs last month",
                5L,
                12L,
                3L
        );

        when(vawcCaseService.getVawcStats()).thenReturn(statsResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/vawc/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCases").value(20))
                .andExpect(jsonPath("$.totalCasesTrend").value("+15.0% vs last month"))
                .andExpect(jsonPath("$.totalReferredCases").value(5))
                .andExpect(jsonPath("$.totalActiveBpo").value(12))
                .andExpect(jsonPath("$.totalWithdrawnCases").value(3));

        verify(vawcCaseService).getVawcStats();
    }

    @Test
    @DisplayName("GET /api/v1/vawc/{id} should return 200 OK with case detail response")
    void getVawcCaseById_shouldReturnOkWithCaseDetail() throws Exception {
        Long caseId = 225L;
        VawcCaseDetailResponse detailResponse = new VawcCaseDetailResponse(
                caseId,
                "2026-VAWC-510",
                com.barangay.zshared.enumerated.FilingType.FORMAL_COMPLAINT,
                com.barangay.zshared.enumerated.CaseStatus.PENDING,
                null,
                "09/05/2026 03:10 PM",
                21L,
                "Josefina Mendoza",
                null,
                null,
                null,
                "Physical Abuse",
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                "MODERATE",
                null,
                null,
                null,
                null
        );

        when(vawcCaseService.getVawcCaseById(caseId)).thenReturn(detailResponse);

        mockMvc.perform(get("/api/v1/vawc/{id}", caseId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(caseId))
                .andExpect(jsonPath("$.caseNumber").value("2026-VAWC-510"))
                .andExpect(jsonPath("$.caseStatus").value("PENDING"));

        verify(vawcCaseService).getVawcCaseById(caseId);
    }

    @Test
    @DisplayName("GET /api/v1/vawc/table should return 200 OK with paginated table rows")
    void getVawcTable_shouldReturnOkWithPageResponse() throws Exception {
        com.barangay.cases.vawc.dto.VawcTableRowResponse row = new com.barangay.cases.vawc.dto.VawcTableRowResponse(
                225L,
                "2026-VAWC-510",
                "Madronio, Jeremiah D.",
                "Perez, Adelaida",
                "Physical Abuse",
                com.barangay.zshared.enumerated.CaseStatus.PENDING,
                "HIGH-IMMINENT",
                "09/05/2026",
                java.time.LocalDateTime.of(2026, 9, 5, 15, 10)
        );

        org.springframework.data.domain.Page<com.barangay.cases.vawc.dto.VawcTableRowResponse> page =
                new org.springframework.data.domain.PageImpl<>(List.of(row), org.springframework.data.domain.PageRequest.of(0, 5), 1L);
        com.barangay.zshared.mapper.PageResponse<com.barangay.cases.vawc.dto.VawcTableRowResponse> pageResponse =
                com.barangay.zshared.mapper.PageResponse.of(page);

        when(vawcCaseService.getVawcCases(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/vawc/table")
                        .param("search", "Madronio")
                        .param("status", "PENDING")
                        .param("riskLevel", "HIGH-IMMINENT")
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(225))
                .andExpect(jsonPath("$.content[0].caseNumber").value("2026-VAWC-510"))
                .andExpect(jsonPath("$.content[0].complainant").value("Madronio, Jeremiah D."))
                .andExpect(jsonPath("$.content[0].respondent").value("Perez, Adelaida"))
                .andExpect(jsonPath("$.content[0].natureOfComplaint").value("Physical Abuse"))
                .andExpect(jsonPath("$.content[0].riskLevel").value("HIGH-IMMINENT"))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(vawcCaseService).getVawcCases(any());
    }
}
