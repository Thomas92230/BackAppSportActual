package com.actuSport.interfaces.rest;

import com.actuSport.application.service.MatchService;
import com.actuSport.domain.model.Match;
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
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchController.class)
@AutoConfigureMockMvc(addFilters = false)
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchService matchService;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllMatches_ShouldReturnList() throws Exception {
        Match match = new Match();
        match.setId(1L);
        when(matchService.getAllMatches()).thenReturn(Collections.singletonList(match));

        mockMvc.perform(get("/api/matches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void updateMatchScore_ShouldReturnUpdatedMatch() throws Exception {
        Match match = new Match();
        match.setId(1L);
        match.setHomeScore(2);
        match.setAwayScore(1);

        Map<String, Object> scoreData = new HashMap<>();
        scoreData.put("homeScore", 2);
        scoreData.put("awayScore", 1);
        scoreData.put("currentMinute", "45'");

        when(matchService.updateMatchScore(anyLong(), anyInt(), anyInt(), anyString())).thenReturn(match);

        mockMvc.perform(put("/api/matches/1/score")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(scoreData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homeScore").value(2))
                .andExpect(jsonPath("$.awayScore").value(1));
    }
}
