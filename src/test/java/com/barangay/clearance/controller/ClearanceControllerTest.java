package com.barangay.clearance.controller;

import com.barangay.clearance.dto.*;
import com.barangay.clearance.service.ClearanceService;
import com.barangay.clearance.service.ClearanceTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClearanceControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ClearanceTemplateService clearanceTemplateService;

    @Mock
    private ClearanceService clearanceService;

    @InjectMocks
    private ClearanceController clearanceController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(clearanceController).build();
    }

    @Test
    @DisplayName("GET /api/v1/clearance/search-resident should return 200 and search results")
    void shouldReturnResidentSearchResults() throws Exception {
        ClearanceResidentSearchResponse profile = new ClearanceResidentSearchResponse(
                1L,
                10L,
                "Juan",
                "Protacio",
                "Rizal",
                null,
                "Juan Rizal",
                30,
                "MALE",
                "Single",
                "09123456789",
                "123 F. Legaspi St., Ugong, Valenzuela City",
                null,
                "Valenzuela City",
                "2020-01-01",
                List.of(),
                List.of()
        );

        when(clearanceTemplateService.searchResidents("Juan")).thenReturn(List.of(profile));

        mockMvc.perform(get("/api/v1/clearance/search-resident")
                        .param("query", "Juan")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Juan Rizal"))
                .andExpect(jsonPath("$[0].residentId").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/clearance/resident/{id} should return 200 and profile")
    void shouldReturnResidentProfileById() throws Exception {
        ClearanceResidentSearchResponse profile = new ClearanceResidentSearchResponse(
                5L,
                50L,
                "Maria",
                "Clara",
                "Santos",
                null,
                "Maria Santos",
                28,
                "FEMALE",
                "Married",
                "09987654321",
                "456 San Diego St., Ugong, Valenzuela City",
                null,
                "Valenzuela City",
                "2018-06-15",
                List.of(),
                List.of()
        );

        when(clearanceTemplateService.getResidentClearanceProfile(5L)).thenReturn(profile);

        mockMvc.perform(get("/api/v1/clearance/resident/5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Maria Santos"))
                .andExpect(jsonPath("$.residentId").value(5));
    }

    @Test
    @DisplayName("POST /api/v1/clearance/issue-clearance should return 200 and issue response")
    void shouldIssueClearanceSuccessfully() throws Exception {
        IssueClearanceRequest request = new IssueClearanceRequest(
                2L,
                5L,
                "Maria Santos",
                objectMapper.createObjectNode().put("typeOfApplication", "Renovation"),
                "9824102",
                "10294812",
                "Building Permit"
        );

        IssueClearanceResponse response = new IssueClearanceResponse(
                true,
                "Clearance certificate issued successfully.",
                "BC-2026-00001",
                50L
        );

        when(clearanceTemplateService.issueClearance(any(IssueClearanceRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/clearance/issue-clearance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.certificateNumber").value("BC-2026-00001"))
                .andExpect(jsonPath("$.issuedId").value(50));
    }

    @Test
    @DisplayName("GET /api/v1/clearance/stats should return 200 and stats response")
    void shouldReturnClearanceStats() throws Exception {
        ClearanceStatsResponse stats = new ClearanceStatsResponse(
                15L,
                4L,
                "Barangay Clearance",
                "Local Employment"
        );

        when(clearanceService.getClearanceStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/clearance/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIssued").value(15))
                .andExpect(jsonPath("$.issuedThisMonth").value(4))
                .andExpect(jsonPath("$.mostIssuedCert").value("Barangay Clearance"))
                .andExpect(jsonPath("$.mostCommonPurpose").value("Local Employment"));
    }
}
