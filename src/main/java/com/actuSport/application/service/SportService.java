package com.actuSport.application.service;

import com.actuSport.domain.model.Sport;
import com.actuSport.domain.repository.SportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional
public class SportService {
    
    @Autowired
    private SportRepository sportRepository;
    
    private static final List<String> DEFAULT_SPORTS = Arrays.asList(
        "football", "basketball", "tennis", "hockey", 
        "rugby", "cycling", "formula1", "judo", "swimming"
    );
    
    public List<Sport> getAllSports() {
        return sportRepository.findAll();
    }
    
    public Optional<Sport> findById(Long id) {
        return sportRepository.findById(id);
    }
    
    public Optional<Sport> findByCode(String code) {
        return sportRepository.findByCode(code);
    }
    
    public Optional<Sport> findByName(String name) {
        return sportRepository.findByName(name);
    }
    
    public Sport saveSport(Sport sport) {
        return sportRepository.save(sport);
    }
    
    public void initializeDefaultSports() {
        DEFAULT_SPORTS.forEach(sportCode -> {
            if (!sportRepository.existsByCode(sportCode)) {
                Sport sport = new Sport();
                sport.setCode(sportCode);
                sport.setName(capitalizeSportName(sportCode));
                sport.setDescription("Actualités et matchs de " + sport.getName());
                sportRepository.save(sport);
            }
        });
    }
    
    private String capitalizeSportName(String sportCode) {
        switch (sportCode.toLowerCase(Locale.ROOT)) {
            case "football": return "Football";
            case "basketball": return "Basketball";
            case "tennis": return "Tennis";
            case "hockey": return "Hockey sur glace";
            case "rugby": return "Rugby";
            case "cycling": return "Cyclisme";
            case "formula1": return "Formule 1";
            case "judo": return "Judo";
            case "swimming": return "Natation";
            default: return sportCode.substring(0, 1).toUpperCase(Locale.ROOT) + sportCode.substring(1);
        }
    }
    
    public Sport updateSport(Long id, Sport sportDetails) {
        Optional<Sport> sportOpt = sportRepository.findById(id);
        if (sportOpt.isPresent()) {
            Sport sport = sportOpt.get();
            sport.setName(sportDetails.getName());
            sport.setCode(sportDetails.getCode());
            sport.setDescription(sportDetails.getDescription());
            sport.setLogoUrl(sportDetails.getLogoUrl());
            
            return sportRepository.save(sport);
        }
        return null;
    }
    
    public void deleteSport(Long id) {
        sportRepository.deleteById(id);
    }
    
    public boolean existsByCode(String code) {
        return sportRepository.existsByCode(code);
    }
    
    public boolean existsByName(String name) {
        return sportRepository.existsByName(name);
    }
}
