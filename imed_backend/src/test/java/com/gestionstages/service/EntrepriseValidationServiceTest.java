package com.gestionstages.service;

import com.gestionstages.dto.EntrepriseResponseDTO;
import com.gestionstages.exception.EntrepriseNotFoundException;
import com.gestionstages.exception.InvalidEntrepriseStatusException;
import com.gestionstages.model.Entreprise;
import com.gestionstages.model.Role;
import com.gestionstages.model.StatutValidation;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.EntrepriseRepository;
import com.gestionstages.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Validation, désactivation et liste des entreprises par le super admin.
 */
class EntrepriseValidationServiceTest {
    private EntrepriseRepository entrepriseRepository;
    private UserRepository userRepository;
    private EntrepriseService service;
    private Entreprise entreprise;
    private Utilisateur compte;

    @BeforeEach
    void setUp() {
        entrepriseRepository = mock(EntrepriseRepository.class);
        userRepository = mock(UserRepository.class);
        service = new EntrepriseService(entrepriseRepository, userRepository, mock(PasswordEncoder.class));
        when(entrepriseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        entreprise = new Entreprise();
        entreprise.setId(10L);
        entreprise.setNom("Acme");
        entreprise.setEmailContact("contact@acme.tn");
        entreprise.setStatutValidation(StatutValidation.EN_ATTENTE);
        compte = new Utilisateur();
        compte.setEmail("contact@acme.tn");
        compte.setRole(Role.ENTREPRISE);
        compte.setActif(false);

        when(entrepriseRepository.findById(10L)).thenReturn(Optional.of(entreprise));
        when(userRepository.findByEmail("contact@acme.tn")).thenReturn(Optional.of(compte));
        when(entrepriseRepository.findByEmailContact("contact@acme.tn")).thenReturn(Optional.of(entreprise));
    }

    @Test
    void laValidationActiveLeCompteEtEffaceLeMotifDeRejet() {
        entreprise.setMotifRejet("Dossier incomplet");

        EntrepriseResponseDTO dto = service.validerEntreprise(10L);

        assertEquals(StatutValidation.VALIDEE, dto.statutValidation());
        assertNull(dto.motifRejet());
        assertTrue(compte.isActif());
        assertSame(entreprise, compte.getEntreprise());
        verify(userRepository).save(compte);
    }

    @Test
    void laDesactivationRejetteLEntrepriseEtBloqueLeCompte() {
        compte.setActif(true);

        service.desactiverEntreprise(10L);

        assertEquals(StatutValidation.REJETEE, entreprise.getStatutValidation());
        assertNotNull(entreprise.getMotifRejet());
        assertFalse(compte.isActif());
    }

    @Test
    void uneEntrepriseInexistanteRenvoieUneErreur() {
        when(entrepriseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntrepriseNotFoundException.class, () -> service.validerEntreprise(99L));
        assertThrows(EntrepriseNotFoundException.class, () -> service.desactiverEntreprise(99L));
    }

    @Test
    void laListeFiltreParStatutSansTenirCompteDeLaCasse() {
        PageRequest page = PageRequest.of(0, 10);
        when(entrepriseRepository.findAllByStatutValidation(StatutValidation.EN_ATTENTE, page))
                .thenReturn(new PageImpl<>(List.of(entreprise)));
        when(entrepriseRepository.findAll(page)).thenReturn(new PageImpl<>(List.of(entreprise, entreprise)));

        Page<EntrepriseResponseDTO> enAttente = service.listerEntreprises(" en_attente ", page);
        Page<EntrepriseResponseDTO> toutes = service.listerEntreprises("", page);

        assertEquals(1, enAttente.getTotalElements());
        assertEquals(2, toutes.getTotalElements());
    }

    @Test
    void unStatutInconnuEstRefuse() {
        assertThrows(InvalidEntrepriseStatusException.class,
                () -> service.listerEntreprises("SUPPRIMEE", PageRequest.of(0, 10)));
    }
}
