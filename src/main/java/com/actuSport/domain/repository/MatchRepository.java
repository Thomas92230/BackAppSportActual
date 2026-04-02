package com.actuSport.domain.repository;

import com.actuSport.domain.model.Match;
import com.actuSport.domain.model.Sport;
import com.actuSport.domain.model.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    
    Optional<Match> findByExternalId(String externalId);
    
    List<Match> findBySport(Sport sport);
    
    List<Match> findByHomeTeamOrAwayTeam(Team homeTeam, Team awayTeam);
    
    // Correction: Renommé pour être explicite sur le filtrage par le code de la compétition
    List<Match> findBySportAndCompetition_Code(Sport sport, String competitionCode);
    
    @Query("SELECT m FROM Match m WHERE m.sport = :sport AND m.matchDate BETWEEN :startDate AND :endDate")
    List<Match> findBySportAndDateRange(@Param("sport") Sport sport, 
                                       @Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT m FROM Match m WHERE m.status = 'LIVE'")
    List<Match> findLiveMatches();
    
    @Query("SELECT m FROM Match m WHERE m.sport = :sport AND m.status = 'LIVE'")
    List<Match> findLiveMatchesBySport(@Param("sport") Sport sport);
    
    @Query("SELECT m FROM Match m WHERE (m.homeTeam = :team OR m.awayTeam = :team) AND m.matchDate >= :date")
    List<Match> findUpcomingMatchesByTeam(@Param("team") Team team, @Param("date") LocalDateTime date);
    
    @Query("SELECT m FROM Match m WHERE (m.homeTeam = :team OR m.awayTeam = :team) AND m.matchDate < :date")
    List<Match> findPastMatchesByTeam(@Param("team") Team team, @Param("date") LocalDateTime date);
    
    Page<Match> findBySportOrderByMatchDateDesc(Sport sport, Pageable pageable);
    
    boolean existsByExternalId(String externalId);
}
