package com.actuSport.application.service;

import com.actuSport.domain.model.Sport;
import com.actuSport.domain.repository.SportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SportServiceTest {

    @Mock
    private SportRepository sportRepository;

    @InjectMocks
    private SportService sportService;

    private Sport sport;

    @BeforeEach
    void setUp() {
        sport = new Sport();
        sport.setId(1L);
        sport.setCode("FOOT");
        sport.setName("Football");
    }

    @Test
    void getAllSports_ShouldReturnList() {
        when(sportRepository.findAll()).thenReturn(Arrays.asList(sport));

        List<Sport> result = sportService.getAllSports();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Football", result.get(0).getName());
    }

    @Test
    void findByCode_ShouldReturnSport() {
        when(sportRepository.findByCode("FOOT")).thenReturn(Optional.of(sport));

        Optional<Sport> result = sportService.findByCode("FOOT");

        assertTrue(result.isPresent());
        assertEquals("Football", result.get().getName());
    }

    @Test
    void saveSport_ShouldReturnSavedSport() {
        when(sportRepository.save(any(Sport.class))).thenReturn(sport);

        Sport savedSport = sportService.saveSport(sport);

        assertNotNull(savedSport);
        assertEquals("Football", savedSport.getName());
    }

    @Test
    void initializeDefaultSports_ShouldSaveMissingSports() {
        // On définit que rien n'existe par défaut
        when(sportRepository.existsByCode(anyString())).thenReturn(false);
        // Sauf le tennis qui existe déjà
        when(sportRepository.existsByCode("tennis")).thenReturn(true);
        
        sportService.initializeDefaultSports();
        
        // Vérifie qu'on a tenté de sauvegarder au moins un sport (ex: football)
        verify(sportRepository, atLeastOnce()).save(argThat(s -> s != null && "football".equals(s.getCode())));
        
        // Vérifie spécifiquement que tennis n'a JAMAIS été sauvegardé
        verify(sportRepository, never()).save(argThat(s -> s != null && "tennis".equals(s.getCode())));
    }
}
