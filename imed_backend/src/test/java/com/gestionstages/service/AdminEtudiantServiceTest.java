package com.gestionstages.service;

import com.gestionstages.dto.CreateEtudiantDTO;
import com.gestionstages.dto.EtudiantResponseDTO;
import com.gestionstages.dto.UpdateEtudiantDTO;
import com.gestionstages.exception.EmailAlreadyUsedException;
import com.gestionstages.model.Role;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminEtudiantServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AdminEtudiantService adminEtudiantService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        adminEtudiantService = new AdminEtudiantService(userRepository, passwordEncoder);
    }

    @Test
    void creeUnEtudiantActif() {
        CreateEtudiantDTO request = new CreateEtudiantDTO(
                "Doe",
                "Lina",
                "LINA@EXAMPLE.COM",
                "motdepasse",
                "71000000",
                "Informatique",
                "Bac+5",
                List.of("Java", "Angular")
        );
        when(userRepository.existsByEmail("lina@example.com")).thenReturn(false);
        when(passwordEncoder.encode("motdepasse")).thenReturn("encoded");
        when(userRepository.save(any(Utilisateur.class))).thenAnswer(invocation -> {
            Utilisateur etudiant = invocation.getArgument(0);
            etudiant.setId(5L);
            return etudiant;
        });

        EtudiantResponseDTO response = adminEtudiantService.creerEtudiant(request);

        assertEquals(5L, response.id());
        assertEquals("lina@example.com", response.email());
        assertTrue(response.actif());
        assertEquals(List.of("Java", "Angular"), response.competences());
    }

    @Test
    void refuseUnEmailDuplique() {
        when(userRepository.existsByEmail("lina@example.com")).thenReturn(true);
        CreateEtudiantDTO request = new CreateEtudiantDTO(
                "Doe",
                "Lina",
                "lina@example.com",
                "motdepasse",
                null,
                null,
                null,
                null
        );

        assertThrows(EmailAlreadyUsedException.class,
                () -> adminEtudiantService.creerEtudiant(request));
    }

    @Test
    void desactiveUnEtudiant() {
        Utilisateur etudiant = etudiantEntity();
        when(userRepository.findByIdAndRole(5L, Role.ETUDIANT))
                .thenReturn(Optional.of(etudiant));

        adminEtudiantService.desactiverEtudiant(5L);

        assertFalse(etudiant.isActif());
        verify(userRepository).save(etudiant);
    }

    @Test
    void activeUnEtudiant() {
        Utilisateur etudiant = etudiantEntity();
        etudiant.setActif(false);
        when(userRepository.findByIdAndRole(5L, Role.ETUDIANT))
                .thenReturn(Optional.of(etudiant));
        when(userRepository.save(etudiant)).thenReturn(etudiant);

        EtudiantResponseDTO response = adminEtudiantService.activerEtudiant(5L);

        assertTrue(response.actif());
        verify(userRepository).save(etudiant);
    }

    @Test
    void modifieUnEtudiant() {
        Utilisateur etudiant = etudiantEntity();
        when(userRepository.findByIdAndRole(5L, Role.ETUDIANT))
                .thenReturn(Optional.of(etudiant));
        when(userRepository.save(etudiant)).thenReturn(etudiant);

        EtudiantResponseDTO response = adminEtudiantService.modifierEtudiant(
                5L,
                new UpdateEtudiantDTO(
                        "Martin",
                        "Sami",
                        null,
                        "72000000",
                        "Réseaux",
                        "Bac+3",
                        List.of("Linux")
                )
        );

        assertEquals("Martin", response.nom());
        assertEquals("Sami", response.prenom());
        assertEquals("Réseaux", response.filiere());
    }

    private Utilisateur etudiantEntity() {
        Utilisateur etudiant = new Utilisateur();
        etudiant.setId(5L);
        etudiant.setNom("Doe");
        etudiant.setPrenom("Lina");
        etudiant.setEmail("lina@example.com");
        etudiant.setRole(Role.ETUDIANT);
        etudiant.setActif(true);
        return etudiant;
    }
}
