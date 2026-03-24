package com.actuSport.domain.repository;

import com.actuSport.domain.model.Competition;
import com.actuSport.domain.model.Sport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionRepository extends JpaRepository<Competition, Long> {
    
    Optional<Competition> findByCode(String code);
    
    List<Competition> findBySport(Sport sport);
    
    List<Competition> findBySportAndCategory(Sport sport, String category);
    
    @Query("SELECT c FROM Competition c WHERE c.name LIKE %:name%")
    List<Competition> findByNameContaining(@Param("name") String name);
    
    @Query("SELECT c FROM Competition c WHERE c.sport = :sport AND c.name LIKE %:name%")
    List<Competition> findBySportAndNameContaining(@Param("sport") Sport sport, @Param("name") String name);
    
    List<Competition> findBySportAndSeason(Sport sport, String season);
    
    boolean existsByCode(String code);
}
