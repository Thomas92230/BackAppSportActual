package com.actuSport.domain.repository;

import com.actuSport.domain.model.Sport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SportRepository extends JpaRepository<Sport, Long> {
    
    Optional<Sport> findByCode(String code);
    
    Optional<Sport> findByName(String name);
    
    @Query("SELECT s FROM Sport s WHERE s.code IN :codes")
    List<Sport> findByCodeIn(List<String> codes);
    
    boolean existsByCode(String code);
    
    boolean existsByName(String name);
}
