package com.actuSport.application.service;

import com.actuSport.domain.model.Sport;
import com.actuSport.domain.model.Team;
import com.actuSport.domain.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;
    
    @Mock
    private SportService sportService;

    @InjectMocks
    private TeamService teamService;

    private Team team;
    private Sport sport;

    @BeforeEach
    void setUp() {
        sport = new Sport();
        sport.setCode("FOOT");

        team = new Team();
        team.setId(1L);
        team.setName("Test Team");
        team.setSport(sport);
    }

    @Test
    void getTeamsBySport_ShouldReturnTeamList() {
        when(sportService.findByCode("FOOT")).thenReturn(Optional.of(sport));
        when(teamRepository.findBySport(sport)).thenReturn(Collections.singletonList(team));

        List<Team> result = teamService.getTeamsBySport("FOOT");

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Test Team", result.get(0).getName());
    }

    @Test
    void searchTeamsByName_ShouldReturnTeamList() {
        when(teamRepository.findByNameContaining("Test")).thenReturn(Collections.singletonList(team));

        List<Team> result = teamService.searchTeamsByName("Test");

        assertFalse(result.isEmpty());
        assertEquals("Test Team", result.get(0).getName());
    }
}
