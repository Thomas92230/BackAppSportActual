package com.actuSport.domain.repository;

import com.actuSport.domain.model.Team;
import com.actuSport.domain.model.Sport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    
    Optional<Team> findByCode(String code);
    
    List<Team> findBySport(Sport sport);
    
    List<Team> findBySportAndCountry(Sport sport, String country);
    
    @Query("SELECT t FROM Team t WHERE t.name LIKE %:name%")
    List<Team> findByNameContaining(@Param("name") String name);
    
    @Query("SELECT t FROM Team t WHERE t.sport = :sport AND t.name LIKE %:name%")
    List<Team> findBySportAndNameContaining(@Param("sport") Sport sport, @Param("name") String name);
    
    boolean existsByCode(String code);
    
    boolean existsByName(String name);
}
