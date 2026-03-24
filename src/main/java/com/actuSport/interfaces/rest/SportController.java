package com.actuSport.interfaces.rest;

import com.actuSport.domain.model.Sport;
import com.actuSport.application.service.SportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/sports")
@CrossOrigin(origins = "*")
public class SportController {
    
    @Autowired
    private SportService sportService;
    
    @GetMapping
    public ResponseEntity<List<Sport>> getAllSports() {
        List<Sport> sports = sportService.getAllSports();
        return ResponseEntity.ok(sports);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Sport> getSportById(@PathVariable Long id) {
        Optional<Sport> sport = sportService.findById(id);
        return sport.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/code/{code}")
    public ResponseEntity<Sport> getSportByCode(@PathVariable String code) {
        Optional<Sport> sport = sportService.findByCode(code);
        return sport.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Sport> createSport(@RequestBody Sport sport) {
        if (sportService.existsByCode(sport.getCode())) {
            return ResponseEntity.badRequest().build();
        }
        
        Sport createdSport = sportService.saveSport(sport);
        return ResponseEntity.ok(createdSport);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Sport> updateSport(@PathVariable Long id, @RequestBody Sport sportDetails) {
        Sport updatedSport = sportService.updateSport(id, sportDetails);
        return updatedSport != null ? ResponseEntity.ok(updatedSport) 
                                    : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSport(@PathVariable Long id) {
        sportService.deleteSport(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/initialize")
    public ResponseEntity<String> initializeDefaultSports() {
        sportService.initializeDefaultSports();
        return ResponseEntity.ok("Default sports initialized successfully");
    }
}
