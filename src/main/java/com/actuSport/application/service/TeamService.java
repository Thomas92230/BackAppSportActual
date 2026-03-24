package com.actuSport.application.service;

import com.actuSport.domain.model.Team;
import com.actuSport.domain.model.Sport;
import com.actuSport.domain.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TeamService {
    
    @Autowired
    private TeamRepository teamRepository;
    
    @Autowired
    private SportService sportService;
    
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }
    
    public Optional<Team> findById(Long id) {
        return teamRepository.findById(id);
    }
    
    public Optional<Team> findByCode(String code) {
        return teamRepository.findByCode(code);
    }
    
    public List<Team> getTeamsBySport(String sportCode) {
        Optional<Sport> sport = sportService.findByCode(sportCode);
        return sport.map(teamRepository::findBySport).orElse(List.of());
    }
    
    public List<Team> getTeamsBySportAndCountry(String sportCode, String country) {
        Optional<Sport> sport = sportService.findByCode(sportCode);
        return sport.map(s -> teamRepository.findBySportAndCountry(s, country)).orElse(List.of());
    }
    
    public List<Team> searchTeamsByName(String name) {
        return teamRepository.findByNameContaining(name);
    }
    
    public List<Team> searchTeamsBySportAndName(String sportCode, String name) {
        Optional<Sport> sport = sportService.findByCode(sportCode);
        return sport.map(s -> teamRepository.findBySportAndNameContaining(s, name)).orElse(List.of());
    }
    
    public Team saveTeam(Team team) {
        return teamRepository.save(team);
    }
    
    public Team updateTeam(Long id, Team teamDetails) {
        Optional<Team> teamOpt = teamRepository.findById(id);
        if (teamOpt.isPresent()) {
            Team team = teamOpt.get();
            team.setName(teamDetails.getName());
            team.setCode(teamDetails.getCode());
            team.setLogoUrl(teamDetails.getLogoUrl());
            team.setCountry(teamDetails.getCountry());
            team.setFoundedYear(teamDetails.getFoundedYear());
            
            return teamRepository.save(team);
        }
        return null;
    }
    
    public void deleteTeam(Long id) {
        teamRepository.deleteById(id);
    }
    
    public boolean existsByCode(String code) {
        return teamRepository.existsByCode(code);
    }
    
    public boolean existsByName(String name) {
        return teamRepository.existsByName(name);
    }
}
