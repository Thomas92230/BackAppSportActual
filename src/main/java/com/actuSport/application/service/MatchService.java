package com.actuSport.application.service;

import com.actuSport.domain.model.Match;
import com.actuSport.domain.model.Sport;
import com.actuSport.domain.model.Team;
import com.actuSport.domain.repository.MatchRepository;
import com.actuSport.infrastructure.websocket.LiveMatchController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class MatchService {
    
    private static final Logger logger = LoggerFactory.getLogger(MatchService.class);
    
    @Autowired
    private MatchRepository matchRepository;
    
    @Autowired
    private SportService sportService;
    
    @Autowired
    private TeamService teamService;
    
    @Autowired
    private CompetitionService competitionService;
    
    @Autowired
    private LiveMatchController liveMatchController;
    
    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }
    
    public Optional<Match> getMatchById(Long id) {
        return matchRepository.findById(id);
    }
    
    public List<Match> getMatchesBySport(String sportCode) {
        Optional<Sport> sport = sportService.findByCode(sportCode);
        return sport.map(matchRepository::findBySport).orElse(List.of());
    }
    
    public List<Match> getLiveMatches() {
        return matchRepository.findLiveMatches();
    }
    
    public List<Match> getLiveMatchesBySport(String sportCode) {
        Optional<Sport> sport = sportService.findByCode(sportCode);
        return sport.map(matchRepository::findLiveMatchesBySport).orElse(List.of());
    }
    
    public List<Match> getMatchesByTeam(Long teamId) {
        Optional<Team> team = teamService.findById(teamId);
        if (team.isPresent()) {
            return matchRepository.findByHomeTeamOrAwayTeam(team.get(), team.get());
        }
        return List.of();
    }
    
    public List<Match> getUpcomingMatchesByTeam(Long teamId) {
        Optional<Team> team = teamService.findById(teamId);
        return team.map(t -> matchRepository.findUpcomingMatchesByTeam(t, LocalDateTime.now()))
                  .orElse(List.of());
    }
    
    public List<Match> getPastMatchesByTeam(Long teamId) {
        Optional<Team> team = teamService.findById(teamId);
        return team.map(t -> matchRepository.findPastMatchesByTeam(t, LocalDateTime.now()))
                  .orElse(List.of());
    }
    
    public Match saveMatch(Match match) {
        Match savedMatch = matchRepository.save(match);
        
        if ("LIVE".equals(match.getStatus())) {
            liveMatchController.broadcastMatchUpdate(savedMatch);
        }
        
        return savedMatch;
    }
    
    public void updateMatches(List<Match> matches) {
        matches.forEach(match -> {
            Optional<Match> existingMatch = matchRepository.findByExternalId(match.getExternalId());
            
            if (existingMatch.isPresent()) {
                Match existing = existingMatch.get();
                updateMatchData(existing, match);
                matchRepository.save(existing);
                
                if ("LIVE".equals(existing.getStatus())) {
                    liveMatchController.broadcastMatchUpdate(existing);
                }
            } else {
                saveMatch(match);
            }
        });
    }
    
    public Match updateMatchScore(Long matchId, Integer homeScore, Integer awayScore, String currentMinute) {
        Optional<Match> matchOpt = matchRepository.findById(matchId);
        if (matchOpt.isPresent()) {
            Match match = matchOpt.get();
            match.setHomeScore(homeScore);
            match.setAwayScore(awayScore);
            match.setCurrentMinute(currentMinute);
            
            Match updatedMatch = matchRepository.save(match);
            liveMatchController.broadcastScoreUpdate(updatedMatch);
            
            return updatedMatch;
        }
        return null;
    }
    
    public Match updateMatchStatus(Long matchId, String status) {
        Optional<Match> matchOpt = matchRepository.findById(matchId);
        if (matchOpt.isPresent()) {
            Match match = matchOpt.get();
            String oldStatus = match.getStatus();
            match.setStatus(status);
            
            Match updatedMatch = matchRepository.save(match);
            
            if ("LIVE".equals(status) || "LIVE".equals(oldStatus)) {
                liveMatchController.broadcastMatchUpdate(updatedMatch);
            }
            
            return updatedMatch;
        }
        return null;
    }
    
    private void updateMatchData(Match existing, Match newData) {
        existing.setHomeScore(newData.getHomeScore());
        existing.setAwayScore(newData.getAwayScore());
        existing.setStatus(newData.getStatus());
        existing.setCurrentMinute(newData.getCurrentMinute());
        existing.setMatchEvents(newData.getMatchEvents());
        existing.setVenue(newData.getVenue());
        existing.setCity(newData.getCity());
        existing.setCountry(newData.getCountry());
    }
    
    public void deleteMatch(Long id) {
        matchRepository.deleteById(id);
    }
}
