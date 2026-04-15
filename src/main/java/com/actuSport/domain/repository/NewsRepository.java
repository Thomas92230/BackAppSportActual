package com.actuSport.domain.repository;

import com.actuSport.domain.model.News;
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
public interface NewsRepository extends JpaRepository<News, Long> {
    
    Optional<News> findFirstByTitle(String title);
    
    List<News> findAllByTitle(String title);
    
    List<News> findBySport(Sport sport);
    
    List<News> findByTeam(Team team);
    
    List<News> findBySportOrderByPublishedAtDesc(Sport sport);
    
    @Query("SELECT n FROM News n WHERE n.sport = :sport AND n.publishedAt BETWEEN :startDate AND :endDate")
    List<News> findBySportAndDateRange(@Param("sport") Sport sport, 
                                       @Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT n FROM News n WHERE n.title LIKE %:keyword% OR n.content LIKE %:keyword%")
    List<News> findByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT n FROM News n WHERE n.sport = :sport AND (n.title LIKE %:keyword% OR n.content LIKE %:keyword%)")
    List<News> findBySportAndKeyword(@Param("sport") Sport sport, @Param("keyword") String keyword);
    
    @Query("SELECT n FROM News n WHERE n.publishedAt >= :date ORDER BY n.publishedAt DESC")
    List<News> findRecentNews(@Param("date") LocalDateTime date);
    
    Page<News> findBySportOrderByPublishedAtDesc(Sport sport, Pageable pageable);
    
    Page<News> findAllByOrderByPublishedAtDesc(Pageable pageable);
    
    @Query("SELECT n FROM News n WHERE n.source = :source ORDER BY n.publishedAt DESC")
    List<News> findBySource(@Param("source") String source);
}
