package com.gestionstages.service;

import com.gestionstages.dto.CreateTacheDTO;
import com.gestionstages.dto.ProgressionDTO;
import com.gestionstages.dto.UpdateTacheDTO;
import com.gestionstages.model.Candidature;
import com.gestionstages.model.OffreStage;
import com.gestionstages.model.StatutStage;
import com.gestionstages.model.StatutApprobation;
import com.gestionstages.model.Tache;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.StageRepository;
import com.gestionstages.repository.TacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TacheServiceTest {

    @Mock
    private TacheRepository tacheRepository;

    @Mock
    private StageRepository stageRepository;

    @InjectMocks
    private TacheService tacheService;

    private Candidature mockStage;
    private Utilisateur mockEtudiant;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockEtudiant = new Utilisateur();
        mockEtudiant.setId(1L);

        mockStage = new Candidature();
        mockStage.setId(1L);
        mockStage.setEtudiant(mockEtudiant);
        mockStage.setStatutStage(StatutStage.ACTIF);
        
        OffreStage offre = new OffreStage();
        offre.setDateDebut(LocalDate.now().minusDays(10));
        offre.setDateFin(LocalDate.now().plusDays(10));
        mockStage.setOffre(offre);

        when(stageRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(mockStage));
        when(stageRepository.findById(1L)).thenReturn(Optional.of(mockStage));
    }

    @Test
    void creerTache_Success() {
        CreateTacheDTO dto = new CreateTacheDTO();
        dto.setDate(LocalDate.now());
        dto.setTitre("Test");
        dto.setDescription("Desc");

        when(stageRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(mockStage));
        when(stageRepository.findById(1L)).thenReturn(Optional.of(mockStage));
        
        Tache savedTache = new Tache();
        savedTache.setId(10L);
        savedTache.setStage(mockStage);
        savedTache.setDate(dto.getDate());
        
        when(tacheRepository.save(any(Tache.class))).thenReturn(savedTache);

        assertDoesNotThrow(() -> {
            tacheService.creerTache(1L, dto, 1L);
        });
        verify(tacheRepository, times(1)).save(any(Tache.class));
    }

    @Test
    void creerTache_NotOwner_ThrowsException() {
        CreateTacheDTO dto = new CreateTacheDTO();
        dto.setDate(LocalDate.now());
        
        when(stageRepository.findById(1L)).thenReturn(Optional.of(mockStage));

        SecurityException ex = assertThrows(SecurityException.class, () -> {
            tacheService.creerTache(1L, dto, 2L); // id etudiant = 2L (différent de 1L)
        });
        assertTrue(ex.getMessage().contains("403"));
    }

    @Test
    void creerTache_NotInDateRange_ThrowsException() {
        CreateTacheDTO dto = new CreateTacheDTO();
        dto.setDate(LocalDate.now().plusDays(20)); // Hors du stage
        
        when(stageRepository.findById(1L)).thenReturn(Optional.of(mockStage));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            tacheService.creerTache(1L, dto, 1L);
        });
        assertTrue(ex.getMessage().contains("400: La date doit être comprise"));
    }

    @Test
    void modifierTache_NotRejected_ThrowsException() {
        UpdateTacheDTO dto = new UpdateTacheDTO();
        dto.setDate(LocalDate.now());

        Tache tache = new Tache();
        tache.setId(10L);
        tache.setStage(mockStage);
        tache.setStatutApprobation(StatutApprobation.EN_ATTENTE);

        when(tacheRepository.findById(10L)).thenReturn(Optional.of(tache));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            tacheService.modifierTache(10L, dto, 1L);
        });
        assertTrue(ex.getMessage().contains("400: Seule une tâche rejetée peut être modifiée"));
    }

    @Test
    void calculerProgression_Success() {
        when(stageRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(mockStage));
        when(stageRepository.findById(1L)).thenReturn(Optional.of(mockStage));
        
        Tache t1 = new Tache(); t1.setDate(LocalDate.now().minusDays(1)); t1.setStatutApprobation(StatutApprobation.APPROUVEE);
        Tache t2 = new Tache(); t2.setDate(LocalDate.now().minusDays(1)); t2.setStatutApprobation(StatutApprobation.APPROUVEE); // même jour
        Tache t3 = new Tache(); t3.setDate(LocalDate.now().minusDays(2)); t3.setStatutApprobation(StatutApprobation.APPROUVEE);
        
        when(tacheRepository.findByStageIdOrderByDateAsc(1L))
            .thenReturn(Arrays.asList(t1, t2, t3));
        when(tacheRepository.findByStageIdAndStatutApprobation(1L, StatutApprobation.APPROUVEE))
            .thenReturn(Arrays.asList(t1, t2, t3));

        ProgressionDTO prog = tacheService.calculerProgression(1L);
        
        // joursCouverts = 2 car (t1 et t2) ont la même date, (t3) a une date diff.
        assertEquals(2, prog.getNombreJoursCouverts());
        assertTrue(prog.getNombreJoursTotal() > 0);
    }
}
