package com.actuSport.interfaces.rest;

import com.actuSport.application.service.SportService;
import com.actuSport.domain.model.Sport;
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

@WebMvcTest(SportController.class)
@AutoConfigureMockMvc(addFilters = false)
class SportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SportService sportService;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getAllSports_ShouldReturnList() throws Exception {
        Sport sport = new Sport();
        sport.setCode("FOOT");
        when(sportService.getAllSports()).thenReturn(Collections.singletonList(sport));

        mockMvc.perform(get("/api/sports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("FOOT"));
    }

    @Test
    void getSportByCode_ShouldReturnSport() throws Exception {
        Sport sport = new Sport();
        sport.setCode("FOOT");
        when(sportService.findByCode("FOOT")).thenReturn(Optional.of(sport));

        mockMvc.perform(get("/api/sports/code/FOOT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("FOOT"));
    }

    @Test
    void createSport_ShouldReturnSport() throws Exception {
        Sport sport = new Sport();
        sport.setCode("NEW");
        
        when(sportService.existsByCode("NEW")).thenReturn(false);
        when(sportService.saveSport(any(Sport.class))).thenReturn(sport);

        mockMvc.perform(post("/api/sports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sport)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NEW"));
    }
}
