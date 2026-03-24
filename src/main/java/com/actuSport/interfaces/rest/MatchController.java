package com.actuSport.interfaces.rest;

import com.actuSport.domain.model.Match;
import com.actuSport.application.service.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/matches")
@CrossOrigin(origins = "*")
public class MatchController {
    
    @Autowired
    private MatchService matchService;
    
    @GetMapping
    public ResponseEntity<List<Match>> getAllMatches() {
        List<Match> matches = matchService.getAllMatches();
        return ResponseEntity.ok(matches);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Match> getMatchById(@PathVariable Long id) {
        Optional<Match> match = matchService.getMatchById(id);
        return match.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/sport/{sportCode}")
    public ResponseEntity<List<Match>> getMatchesBySport(@PathVariable String sportCode) {
        List<Match> matches = matchService.getMatchesBySport(sportCode);
        return ResponseEntity.ok(matches);
    }
    
    @GetMapping("/live")
    public ResponseEntity<List<Match>> getLiveMatches() {
        List<Match> matches = matchService.getLiveMatches();
        return ResponseEntity.ok(matches);
    }
    
    @GetMapping("/live/{sportCode}")
    public ResponseEntity<List<Match>> getLiveMatchesBySport(@PathVariable String sportCode) {
        List<Match> matches = matchService.getLiveMatchesBySport(sportCode);
        return ResponseEntity.ok(matches);
    }
    
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<Match>> getMatchesByTeam(@PathVariable Long teamId) {
        List<Match> matches = matchService.getMatchesByTeam(teamId);
        return ResponseEntity.ok(matches);
    }
    
    @GetMapping("/team/{teamId}/upcoming")
    public ResponseEntity<List<Match>> getUpcomingMatchesByTeam(@PathVariable Long teamId) {
        List<Match> matches = matchService.getUpcomingMatchesByTeam(teamId);
        return ResponseEntity.ok(matches);
    }
    
    @GetMapping("/team/{teamId}/past")
    public ResponseEntity<List<Match>> getPastMatchesByTeam(@PathVariable Long teamId) {
        List<Match> matches = matchService.getPastMatchesByTeam(teamId);
        return ResponseEntity.ok(matches);
    }
    
    @PostMapping
    public ResponseEntity<Match> createMatch(@RequestBody Match match) {
        Match createdMatch = matchService.saveMatch(match);
        return ResponseEntity.ok(createdMatch);
    }
    
    @PutMapping("/{id}/score")
    public ResponseEntity<Match> updateMatchScore(@PathVariable Long id, @RequestBody Map<String, Object> scoreData) {
        Integer homeScore = (Integer) scoreData.get("homeScore");
        Integer awayScore = (Integer) scoreData.get("awayScore");
        String currentMinute = (String) scoreData.get("currentMinute");
        
        Match updatedMatch = matchService.updateMatchScore(id, homeScore, awayScore, currentMinute);
        return updatedMatch != null ? ResponseEntity.ok(updatedMatch) 
                                    : ResponseEntity.notFound().build();
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<Match> updateMatchStatus(@PathVariable Long id, @RequestBody Map<String, String> statusData) {
        String status = statusData.get("status");
        
        Match updatedMatch = matchService.updateMatchStatus(id, status);
        return updatedMatch != null ? ResponseEntity.ok(updatedMatch) 
                                    : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long id) {
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }
}
