package com.actuSport.interfaces.rest;

import com.actuSport.domain.model.Team;
import com.actuSport.application.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/teams")
@CrossOrigin(origins = "*")
public class TeamController {
    
    @Autowired
    private TeamService teamService;
    
    @GetMapping
    public ResponseEntity<List<Team>> getAllTeams() {
        List<Team> teams = teamService.getAllTeams();
        return ResponseEntity.ok(teams);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Team> getTeamById(@PathVariable Long id) {
        Optional<Team> team = teamService.findById(id);
        return team.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/code/{code}")
    public ResponseEntity<Team> getTeamByCode(@PathVariable String code) {
        Optional<Team> team = teamService.findByCode(code);
        return team.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/sport/{sportCode}")
    public ResponseEntity<List<Team>> getTeamsBySport(@PathVariable String sportCode) {
        List<Team> teams = teamService.getTeamsBySport(sportCode);
        return ResponseEntity.ok(teams);
    }
    
    @GetMapping("/sport/{sportCode}/country/{country}")
    public ResponseEntity<List<Team>> getTeamsBySportAndCountry(
            @PathVariable String sportCode, 
            @PathVariable String country) {
        List<Team> teams = teamService.getTeamsBySportAndCountry(sportCode, country);
        return ResponseEntity.ok(teams);
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Team>> searchTeamsByName(@RequestParam String name) {
        List<Team> teams = teamService.searchTeamsByName(name);
        return ResponseEntity.ok(teams);
    }
    
    @GetMapping("/search/{sportCode}")
    public ResponseEntity<List<Team>> searchTeamsBySportAndName(
            @PathVariable String sportCode, 
            @RequestParam String name) {
        List<Team> teams = teamService.searchTeamsBySportAndName(sportCode, name);
        return ResponseEntity.ok(teams);
    }
    
    @PostMapping
    public ResponseEntity<Team> createTeam(@RequestBody Team team) {
        if (teamService.existsByCode(team.getCode())) {
            return ResponseEntity.badRequest().build();
        }
        
        Team createdTeam = teamService.saveTeam(team);
        return ResponseEntity.ok(createdTeam);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Team> updateTeam(@PathVariable Long id, @RequestBody Team teamDetails) {
        Team updatedTeam = teamService.updateTeam(id, teamDetails);
        return updatedTeam != null ? ResponseEntity.ok(updatedTeam) 
                                  : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long id) {
        teamService.deleteTeam(id);
        return ResponseEntity.noContent().build();
    }
}
