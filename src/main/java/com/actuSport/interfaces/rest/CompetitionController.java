package com.actuSport.interfaces.rest;

import com.actuSport.domain.model.Competition;
import com.actuSport.application.service.CompetitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/competitions")
@CrossOrigin(origins = "*")
public class CompetitionController {
    
    @Autowired
    private CompetitionService competitionService;
    
    @GetMapping
    public ResponseEntity<List<Competition>> getAllCompetitions() {
        List<Competition> competitions = competitionService.getAllCompetitions();
        return ResponseEntity.ok(competitions);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Competition> getCompetitionById(@PathVariable Long id) {
        Optional<Competition> competition = competitionService.findById(id);
        return competition.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/code/{code}")
    public ResponseEntity<Competition> getCompetitionByCode(@PathVariable String code) {
        Optional<Competition> competition = competitionService.findByCode(code);
        return competition.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/sport/{sportCode}")
    public ResponseEntity<List<Competition>> getCompetitionsBySport(@PathVariable String sportCode) {
        List<Competition> competitions = competitionService.getCompetitionsBySport(sportCode);
        return ResponseEntity.ok(competitions);
    }
    
    @GetMapping("/sport/{sportCode}/category/{category}")
    public ResponseEntity<List<Competition>> getCompetitionsBySportAndCategory(
            @PathVariable String sportCode, 
            @PathVariable String category) {
        List<Competition> competitions = competitionService.getCompetitionsBySportAndCategory(sportCode, category);
        return ResponseEntity.ok(competitions);
    }
    
    @GetMapping("/sport/{sportCode}/season/{season}")
    public ResponseEntity<List<Competition>> getCompetitionsBySportAndSeason(
            @PathVariable String sportCode, 
            @PathVariable String season) {
        List<Competition> competitions = competitionService.getCompetitionsBySportAndSeason(sportCode, season);
        return ResponseEntity.ok(competitions);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Competition>> searchCompetitionsByName(@RequestParam String name) {
        List<Competition> competitions = competitionService.searchCompetitionsByName(name);
        return ResponseEntity.ok(competitions);
    }
    
    @GetMapping("/search/{sportCode}")
    public ResponseEntity<List<Competition>> searchCompetitionsBySportAndName(
            @PathVariable String sportCode, 
            @RequestParam String name) {
        List<Competition> competitions = competitionService.searchCompetitionsBySportAndName(sportCode, name);
        return ResponseEntity.ok(competitions);
    }
    
    @PostMapping
    public ResponseEntity<Competition> createCompetition(@RequestBody Competition competition) {
        if (competitionService.existsByCode(competition.getCode())) {
            return ResponseEntity.badRequest().build();
        }
        
        Competition createdCompetition = competitionService.saveCompetition(competition);
        return ResponseEntity.ok(createdCompetition);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Competition> updateCompetition(@PathVariable Long id, @RequestBody Competition competitionDetails) {
        Competition updatedCompetition = competitionService.updateCompetition(id, competitionDetails);
        return updatedCompetition != null ? ResponseEntity.ok(updatedCompetition) 
                                         : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompetition(@PathVariable Long id) {
        competitionService.deleteCompetition(id);
        return ResponseEntity.noContent().build();
    }
}
