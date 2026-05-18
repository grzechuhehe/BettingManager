package com.grzechuhehe.SportsBettingManagerApp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grzechuhehe.SportsBettingManagerApp.config.AuthEntryPointJwt;
import com.grzechuhehe.SportsBettingManagerApp.dto.EvCalculationRequest;
import com.grzechuhehe.SportsBettingManagerApp.dto.EvCalculationResponse;
import com.grzechuhehe.SportsBettingManagerApp.service.EvCalculationService;
import com.grzechuhehe.SportsBettingManagerApp.util.JwtUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvController.class)
@AutoConfigureMockMvc(addFilters = false)
class EvControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EvCalculationService evCalculationService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private AuthEntryPointJwt authEntryPointJwt;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "testuser")
    void calculateEv_ShouldReturnOk_WhenRequestIsValid() throws Exception {
        // Given
        EvCalculationRequest request = new EvCalculationRequest("Test Event", new BigDecimal("2.50"));
        EvCalculationResponse response = new EvCalculationResponse(
                "Test Event",
                new BigDecimal("2.50"),
                new BigDecimal("0.45"),
                new BigDecimal("12.5"),
                true
        );

        Mockito.when(evCalculationService.calculateExpectedValue(any(EvCalculationRequest.class)))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/ev/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventName").value("Test Event"))
                .andExpect(jsonPath("$.bookmakerOdds").value(2.50))
                .andExpect(jsonPath("$.trueProbability").value(0.45))
                .andExpect(jsonPath("$.expectedValuePercentage").value(12.5))
                .andExpect(jsonPath("$.positiveEv").value(true));
    }
}
