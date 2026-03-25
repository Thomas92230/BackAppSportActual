package com.actuSport.interfaces.rest;

import com.actuSport.application.service.CompetitionService;
import com.actuSport.domain.model.Competition;
import com.actuSport.infrastructure.security.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CompetitionController.class)
@AutoConfigureMockMvc(addFilters = false)
class CompetitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompetitionService competitionService;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllCompetitions_ShouldReturnList() throws Exception {
        Competition competition = new Competition();
        competition.setName("Test Competition");
        when(competitionService.getAllCompetitions()).thenReturn(Collections.singletonList(competition));

        mockMvc.perform(get("/api/competitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Competition"));
    }

    @Test
    void getCompetitionByCode_ShouldReturnCompetition() throws Exception {
        Competition competition = new Competition();
        competition.setCode("LIGUE1");
        when(competitionService.findByCode("LIGUE1")).thenReturn(Optional.of(competition));

        mockMvc.perform(get("/api/competitions/code/LIGUE1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("LIGUE1"));
    }
}
