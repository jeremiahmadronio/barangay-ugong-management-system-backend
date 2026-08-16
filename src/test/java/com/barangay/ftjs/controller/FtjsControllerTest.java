package com.barangay.ftjs.controller;

import com.barangay.ftjs.dto.*;
import com.barangay.ftjs.service.*;
import com.barangay.zshared.enumerated.FtjsStatus;
import com.barangay.zshared.mapper.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FtjsControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private FtjsService ftjsService;

    @Mock
    private FtjsReIssuanceService ftjsReIssuanceService;

    @Mock
    private FtjsNoteService ftjsNoteService;

    @Mock
    private FtjsArchiveAndDeleteService archiveAndDeleteService;

    @Mock
    private FtjsRetentionService retentionService;

    @Mock
    private FtjsReportsService ftjsReportsService;

    @InjectMocks
    private FtjsController ftjsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(ftjsController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/ftjs/search-resident - shouldReturnOkWithList")
    void searchResidents_shouldReturnOkWithList() throws Exception {
        // Given
        when(ftjsService.searchResidents("Juan")).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/ftjs/search-resident")
                        .param("query", "Juan")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        verify(ftjsService).searchResidents("Juan");
    }

    @Test
    @DisplayName("GET /api/v1/ftjs/stats - shouldReturnOkWithStatsResponse")
    void getStats_shouldReturnOkWithStatsResponse() throws Exception {
        // Given
        FtjsStatsResponse response = new FtjsStatsResponse(100L, 10L, 50.0, "+50.0% vs last month", 80L, 20L);
        when(ftjsService.getStats()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/ftjs/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCertificatesIssued").value(100))
                .andExpect(jsonPath("$.totalCertificatedThisMonth").value(10))
                .andExpect(jsonPath("$.originalIssuances").value(80))
                .andExpect(jsonPath("$.reIssuances").value(20));
        verify(ftjsService).getStats();
    }

    @Test
    @DisplayName("GET /api/v1/ftjs/summary - shouldReturnOk")
    void getSummaryTable_shouldReturnOk() throws Exception {
        // Given
        FtjsSummaryResponse item = new FtjsSummaryResponse(
                1L, "2026-FTJS-0001", "Maria Clara", 1L, FtjsStatus.ISSUED, "Employment", java.time.LocalDateTime.now()
        );
        when(ftjsService.getSummaryTable(any(), any(), any(), any(), any())).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/v1/ftjs/summary")
                        .param("search", "Maria")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/ftjs/resident/{residentId} - shouldReturnResidentProfile")
    void getResidentFtjsProfile_shouldReturnResidentProfile() throws Exception {
        // Given
        Long residentId = 10L;

        // When & Then
        mockMvc.perform(get("/api/v1/ftjs/resident/{residentId}", residentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(ftjsService).getResidentFtjsProfile(residentId);
    }

    @Test
    @DisplayName("POST /api/v1/ftjs/entry - shouldCreateFtjsEntry")
    void createFtjsEntry_shouldCreateFtjsEntry() throws Exception {
        // Given
        FtjsRequestEntry request = new FtjsRequestEntry(
                1L, "First Job Requirement", java.time.LocalDateTime.now(), "High School", "Senior High", "NHS", "Barangay ID", "Admin", null, null
        );

        // When & Then
        mockMvc.perform(post("/api/v1/ftjs/entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        verify(ftjsService).createFtjsEntry(any(FtjsRequestEntry.class));
    }

    @Test
    @DisplayName("GET /api/v1/ftjs/detail/{id} - shouldReturnApplicantDetail")
    void getFtjsDetail_shouldReturnApplicantDetail() throws Exception {
        // Given
        Long id = 5L;

        // When & Then
        mockMvc.perform(get("/api/v1/ftjs/detail/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(ftjsService).getFtjsApplicantDetail(id);
    }

    @Test
    @DisplayName("POST /api/v1/ftjs/re-issuance-request/{ftjsId} - shouldCreateReIssuanceRequest")
    void createReIssuanceRequest_shouldCreateReIssuanceRequest() throws Exception {
        // Given
        Long ftjsId = 2L;
        ReIssuanceRequestDto request = new ReIssuanceRequestDto(
                ftjsId, "Lost original copy", LocalDate.now(), "OR-8888", null
        );
        when(ftjsReIssuanceService.createReIssuanceRequest(eq(ftjsId), any())).thenReturn(request);

        // When & Then
        mockMvc.perform(post("/api/v1/ftjs/re-issuance-request/{ftjsId}", ftjsId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
        verify(ftjsReIssuanceService).createReIssuanceRequest(eq(ftjsId), any());
    }

    @Test
    @DisplayName("GET /api/v1/ftjs/{ftjsId}/re-issuances/history - shouldReturnReIssuanceHistory")
    void getReIssuanceHistory_shouldReturnReIssuanceHistory() throws Exception {
        // Given
        Long ftjsId = 1L;
        when(ftjsReIssuanceService.getReIssuanceHistory(ftjsId)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/ftjs/{ftjsId}/re-issuances/history", ftjsId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(ftjsReIssuanceService).getReIssuanceHistory(ftjsId);
    }

    @Test
    @DisplayName("GET /api/v1/ftjs/{ftjsId}/timeline/ - shouldReturnTimeline")
    void getTimeline_shouldReturnTimeline() throws Exception {
        // Given
        Long ftjsId = 1L;
        when(ftjsReIssuanceService.getFtjsTimeline(ftjsId)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/ftjs/{ftjsId}/timeline/", ftjsId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(ftjsReIssuanceService).getFtjsTimeline(ftjsId);
    }

    @Test
    @DisplayName("POST /api/v1/ftjs/notes-entry/{ftjsId} - shouldAddNote")
    void addNote_shouldAddNote() throws Exception {
        // Given
        Long ftjsId = 1L;
        FtjsNoteRequest request = new FtjsNoteRequest("Test note content");

        // When & Then
        mockMvc.perform(post("/api/v1/ftjs/notes-entry/{ftjsId}", ftjsId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
        verify(ftjsNoteService).addNote(eq(ftjsId), any());
    }

    @Test
    @DisplayName("GET /api/v1/ftjs/notes-history/{ftjsId} - shouldReturnNotesHistory")
    void getNotes_shouldReturnNotesHistory() throws Exception {
        // Given
        Long ftjsId = 1L;
        when(ftjsNoteService.getNotesByFtjsId(ftjsId)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/v1/ftjs/notes-history/{ftjsId}", ftjsId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(ftjsNoteService).getNotesByFtjsId(ftjsId);
    }

    @Test
    @DisplayName("POST /api/v1/ftjs/archive/{id} - shouldArchiveFtjs")
    void archiveFtjs_shouldArchiveFtjs() throws Exception {
        // Given
        Long id = 1L;

        // When & Then
        mockMvc.perform(post("/api/v1/ftjs/archive/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(archiveAndDeleteService).archiveFtjs(id);
    }

    @Test
    @DisplayName("POST /api/v1/ftjs/restore/{id} - shouldRestoreFtjs")
    void restoreFtjs_shouldRestoreFtjs() throws Exception {
        // Given
        Long id = 1L;

        // When & Then
        mockMvc.perform(post("/api/v1/ftjs/restore/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(archiveAndDeleteService).restoreFtjs(id);
    }

    @Test
    @DisplayName("GET /api/v1/ftjs/archive/stats - shouldReturnArchiveStats")
    void getArchiveAndDeleteStats_shouldReturnArchiveStats() throws Exception {
        // Given
        FtjsArchiveStatsResponse response = new FtjsArchiveStatsResponse(10L, 2L, "+20%", 8L, 1L);
        when(archiveAndDeleteService.getArchiveStats()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/ftjs/archive/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalArchived").value(10));
        verify(archiveAndDeleteService).getArchiveStats();
    }

    @Test
    @DisplayName("GET /api/v1/ftjs/archives/summary - shouldReturnArchivedSummaryTable")
    void getArchivedSummaryTable_shouldReturnArchivedSummaryTable() throws Exception {
        // Given
        FtjsSummaryResponse item = new FtjsSummaryResponse(
                1L, "2026-FTJS-0001", "Maria Clara", 1L, FtjsStatus.ISSUED, "Employment", java.time.LocalDateTime.now()
        );
        when(archiveAndDeleteService.getArchivedSummaryTable(any(), any(), any(), any(), any())).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/v1/ftjs/archives/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/ftjs/trash/summary - shouldReturnDeletedSummaryTable")
    void getDeletedSummaryTable_shouldReturnDeletedSummaryTable() throws Exception {
        // Given
        when(archiveAndDeleteService.getDeletedSummaryTable(any(), any(), any(), any(), any())).thenReturn(PageResponse.of(new org.springframework.data.domain.PageImpl<>(Collections.emptyList())));

        // When & Then
        mockMvc.perform(get("/api/v1/ftjs/trash/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/v1/ftjs/display-retention - shouldReturnRetentionSetting")
    void getRetentionSetting_shouldReturnRetentionSetting() throws Exception {
        // Given
        RetentionSettingDto.Response response = new RetentionSettingDto.Response("30");
        when(retentionService.getCurrentRetentionSetting()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/ftjs/display-retention")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.retentionPeriod").value("30"));
        verify(retentionService).getCurrentRetentionSetting();
    }

    @Test
    @DisplayName("PUT /api/v1/ftjs/update-retention - shouldUpdateRetentionSetting")
    void updateRetentionSetting_shouldUpdateRetentionSetting() throws Exception {
        // Given
        RetentionSettingDto.Request request = new RetentionSettingDto.Request("30");

        // When & Then
        mockMvc.perform(put("/api/v1/ftjs/update-retention")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
        verify(retentionService).updateRetentionSetting(any());
    }

    @Test
    @DisplayName("DELETE /api/v1/ftjs/{id} - shouldSoftDelete")
    void softDelete_shouldSoftDelete() throws Exception {
        // Given
        Long id = 5L;

        // When & Then
        mockMvc.perform(delete("/api/v1/ftjs/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(archiveAndDeleteService).softDelete(id);
    }

    @Test
    @DisplayName("DELETE /api/v1/ftjs/trash/{id}/permanent - shouldPermanentlyDelete")
    void permanentlyDelete_shouldPermanentlyDelete() throws Exception {
        // Given
        Long id = 5L;

        // When & Then
        mockMvc.perform(delete("/api/v1/ftjs/trash/{id}/permanent", id)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(archiveAndDeleteService).permanentlyDelete(id);
    }

    @Test
    @DisplayName("DELETE /api/v1/ftjs/trash/permanent/batch - shouldPermanentlyDeleteBatch")
    void permanentlyDeleteBatch_shouldPermanentlyDeleteBatch() throws Exception {
        // Given
        List<Long> ids = List.of(1L, 2L, 3L);

        // When & Then
        mockMvc.perform(delete("/api/v1/ftjs/trash/permanent/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ids)))
                .andExpect(status().isNoContent());
        verify(archiveAndDeleteService).permanentlyDeleteBatch(ids);
    }

    @Test
    @DisplayName("GET /api/v1/ftjs/trend - shouldReturnTrendReport")
    void getTrendReport_shouldReturnTrendReport() throws Exception {
        // Given
        TrendReportResponse response = new TrendReportResponse("Monthly", "Last 30 days", Collections.emptyList(), null, null);
        when(ftjsReportsService.generateAdaptiveTrendReport(any())).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/ftjs/trend")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(ftjsReportsService).generateAdaptiveTrendReport(any());
    }

    @Test
    @DisplayName("GET /api/v1/ftjs/education-distribution - shouldReturnEducationDistribution")
    void getEducationDistribution_shouldReturnEducationDistribution() throws Exception {
        // Given
        EducationDistributionResponse response = new EducationDistributionResponse(Collections.emptyList(), null, 0, 0);
        when(ftjsReportsService.generateEducationDistribution(any())).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/ftjs/education-distribution")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(ftjsReportsService).generateEducationDistribution(any());
    }

    @Test
    @DisplayName("GET /api/v1/ftjs/purpose-distribution - shouldReturnPurposeDistribution")
    void getPurposeDistribution_shouldReturnPurposeDistribution() throws Exception {
        // Given
        PurposeDistributionResponse response = new PurposeDistributionResponse(Collections.emptyList(), 0);
        when(ftjsReportsService.generatePurposeDistribution(any())).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/ftjs/purpose-distribution")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(ftjsReportsService).generatePurposeDistribution(any());
    }

    @Test
    @DisplayName("GET /api/v1/ftjs/issuance-distribution - shouldReturnIssuanceDistribution")
    void getIssuanceTypeDistribution_shouldReturnIssuanceDistribution() throws Exception {
        // Given
        IssuanceTypeDistributionResponse response = new IssuanceTypeDistributionResponse(Collections.emptyList(), 0);
        when(ftjsReportsService.generateIssuanceTypeDistribution(any())).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/ftjs/issuance-distribution")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(ftjsReportsService).generateIssuanceTypeDistribution(any());
    }

    @Test
    @DisplayName("GET /api/v1/ftjs/reports/table - shouldReturnReportTable")
    void getReportTable_shouldReturnReportTable() throws Exception {
        // Given
        FtjsReportTableResponse item = new FtjsReportTableResponse(
                1L, "2026-FTJS-0001", "Maria Clara", 1L, "Employment", FtjsStatus.ISSUED, java.time.LocalDateTime.now()
        );
        when(ftjsReportsService.generateReportTable(any(), any(), any())).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/api/v1/ftjs/reports/table")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(ftjsReportsService).generateReportTable(any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/v1/ftjs/reports/kpi - shouldReturnReportKpi")
    void getReportKpi_shouldReturnReportKpi() throws Exception {
        // Given
        FtjsReportKpiResponse response = new FtjsReportKpiResponse(100, "subtitle", 80, 80.0, "subtitle", "Employment", 50.0, "subtitle", "18-24", 70.0, "subtitle");
        when(ftjsReportsService.generateReportKpi(any())).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/ftjs/reports/kpi")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(ftjsReportsService).generateReportKpi(any());
    }

    @Test
    @DisplayName("GET /api/v1/ftjs/dashboard-stats - shouldReturnDashboardSummary")
    void getSummaryStats_shouldReturnDashboardSummary() throws Exception {
        // Given
        FtjsDashboardSummaryDto dto = new FtjsDashboardSummaryDto(10L, 50L, 30L, 60.0, 25L, 50.0, 100L);
        when(ftjsService.getDashboardSummary()).thenReturn(dto);

        // When & Then
        mockMvc.perform(get("/api/v1/ftjs/dashboard-stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(ftjsService).getDashboardSummary();
    }
}
