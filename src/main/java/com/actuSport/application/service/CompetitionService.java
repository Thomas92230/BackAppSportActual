package com.actuSport.application.service;

import com.actuSport.domain.model.Competition;
import com.actuSport.domain.model.Sport;
import com.actuSport.domain.repository.CompetitionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CompetitionService {
    
    @Autowired
    private CompetitionRepository competitionRepository;
    
    @Autowired
    private SportService sportService;
    
    public List<Competition> getAllCompetitions() {
        return competitionRepository.findAll();
    }
    
    public Optional<Competition> findById(Long id) {
        return competitionRepository.findById(id);
    }
    
    public Optional<Competition> findByCode(String code) {
        return competitionRepository.findByCode(code);
    }
    
    public List<Competition> getCompetitionsBySport(String sportCode) {
        Optional<Sport> sport = sportService.findByCode(sportCode);
        return sport.map(competitionRepository::findBySport).orElse(List.of());
    }
    
    public List<Competition> getCompetitionsBySportAndCategory(String sportCode, String category) {
        Optional<Sport> sport = sportService.findByCode(sportCode);
        return sport.map(s -> competitionRepository.findBySportAndCategory(s, category)).orElse(List.of());
    }
    
    public List<Competition> getCompetitionsBySportAndSeason(String sportCode, String season) {
        Optional<Sport> sport = sportService.findByCode(sportCode);
        return sport.map(s -> competitionRepository.findBySportAndSeason(s, season)).orElse(List.of());
    }
    
    public List<Competition> searchCompetitionsByName(String name) {
        return competitionRepository.findByNameContaining(name);
    }
    
    public List<Competition> searchCompetitionsBySportAndName(String sportCode, String name) {
        Optional<Sport> sport = sportService.findByCode(sportCode);
        return sport.map(s -> competitionRepository.findBySportAndNameContaining(s, name)).orElse(List.of());
    }
    
    public Competition saveCompetition(Competition competition) {
        return competitionRepository.save(competition);
    }
    
    public Competition updateCompetition(Long id, Competition competitionDetails) {
        Optional<Competition> competitionOpt = competitionRepository.findById(id);
        if (competitionOpt.isPresent()) {
            Competition competition = competitionOpt.get();
            competition.setName(competitionDetails.getName());
            competition.setCode(competitionDetails.getCode());
            competition.setCategory(competitionDetails.getCategory());
            competition.setSeason(competitionDetails.getSeason());
            competition.setLogoUrl(competitionDetails.getLogoUrl());
            
            return competitionRepository.save(competition);
        }
        return null;
    }
    
    public void deleteCompetition(Long id) {
        competitionRepository.deleteById(id);
    }
    
    public boolean existsByCode(String code) {
        return competitionRepository.existsByCode(code);
    }
}
