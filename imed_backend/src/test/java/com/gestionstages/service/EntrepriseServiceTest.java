package com.gestionstages.service;

import com.gestionstages.dto.CreateEntrepriseDTO;
import com.gestionstages.dto.EntrepriseResponseDTO;
import com.gestionstages.dto.UpdateEntrepriseDTO;
import com.gestionstages.exception.EntrepriseEmailAlreadyUsedException;
import com.gestionstages.model.Entreprise;
import com.gestionstages.model.StatutValidation;
import com.gestionstages.model.TailleEntreprise;
import com.gestionstages.repository.EntrepriseRepository;
import com.gestionstages.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntrepriseServiceTest {

    private EntrepriseRepository entrepriseRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private EntrepriseService entrepriseService;

    @BeforeEach
    void setUp() {
        entrepriseRepository = mock(EntrepriseRepository.class);
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        entrepriseService = new EntrepriseService(
                entrepriseRepository,
                userRepository,
                passwordEncoder
        );
    }

    @Test
    void createsValidatedEntreprise() {
        CreateEntrepriseDTO request = new CreateEntrepriseDTO(
                "Acme",
                "1 rue Centrale",
                "Tunis",
                "Technologie",
                TailleEntreprise.PME,
                "CONTACT@ACME.TN",
                "71000000",
                "https://acme.tn",
                "motdepasse"
        );
        when(entrepriseRepository.existsByEmailContact("contact@acme.tn"))
                .thenReturn(false);
        when(entrepriseRepository.existsByNom("Acme")).thenReturn(false);
        when(entrepriseRepository.save(any(Entreprise.class))).thenAnswer(invocation -> {
            Entreprise entreprise = invocation.getArgument(0);
            entreprise.setId(10L);
            return entreprise;
        });

        EntrepriseResponseDTO response =
                entrepriseService.creerEntrepriseParAdmin(request);

        assertEquals(10L, response.id());
        assertEquals("contact@acme.tn", response.emailContact());
        assertEquals(StatutValidation.VALIDEE, response.statutValidation());
        assertNotNull(response.dateInscription());
    }

    @Test
    void rejectsDuplicateContactEmail() {
        when(entrepriseRepository.existsByEmailContact("contact@acme.tn"))
                .thenReturn(true);
        CreateEntrepriseDTO request = new CreateEntrepriseDTO(
                "Acme",
                null,
                null,
                null,
                null,
                "contact@acme.tn",
                null,
                null,
                "motdepasse"
        );

        assertThrows(
                EntrepriseEmailAlreadyUsedException.class,
                () -> entrepriseService.creerEntrepriseParAdmin(request)
        );
    }

    @Test
    void updatesOnlyProvidedFields() {
        Entreprise entreprise = existingEntreprise();
        when(entrepriseRepository.findById(10L)).thenReturn(Optional.of(entreprise));
        when(entrepriseRepository.save(entreprise)).thenReturn(entreprise);
        UpdateEntrepriseDTO request = new UpdateEntrepriseDTO(
                null,
                null,
                "Sfax",
                null,
                null,
                null,
                null,
                null
        );

        EntrepriseResponseDTO response =
                entrepriseService.modifierEntreprise(10L, request);

        assertEquals("Acme", response.nom());
        assertEquals("Sfax", response.ville());
        assertEquals("contact@acme.tn", response.emailContact());
    }

    private Entreprise existingEntreprise() {
        Entreprise entreprise = new Entreprise();
        entreprise.setId(10L);
        entreprise.setNom("Acme");
        entreprise.setVille("Tunis");
        entreprise.setEmailContact("contact@acme.tn");
        entreprise.setStatutValidation(StatutValidation.VALIDEE);
        entreprise.setDateInscription(LocalDateTime.now());
        return entreprise;
    }
}
