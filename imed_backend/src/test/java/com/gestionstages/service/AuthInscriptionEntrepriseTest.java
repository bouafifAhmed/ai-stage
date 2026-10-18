package com.gestionstages.service;

import com.gestionstages.dto.AuthResponseDTO;
import com.gestionstages.dto.CreateEntrepriseDTO;
import com.gestionstages.exception.EmailAlreadyUsedException;
import com.gestionstages.model.Entreprise;
import com.gestionstages.model.Role;
import com.gestionstages.model.StatutValidation;
import com.gestionstages.model.TailleEntreprise;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.EntrepriseRepository;
import com.gestionstages.repository.UserRepository;
import com.gestionstages.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Inscription publique d'une entreprise : compte inactif tant qu'elle n'est pas validée.
 */
class AuthInscriptionEntrepriseTest {
    private UserRepository userRepository;
    private EntrepriseRepository entrepriseRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        entrepriseRepository = mock(EntrepriseRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new AuthService(userRepository, passwordEncoder, mock(AuthenticationManager.class),
                mock(JwtTokenProvider.class), entrepriseRepository);
        when(passwordEncoder.encode("motdepasse1")).thenReturn("hash");
        when(entrepriseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void lInscriptionCreeUneEntrepriseEnAttenteEtUnCompteInactifSansJeton() {
        AuthResponseDTO reponse = service.registerEntreprise(demande(" Contact@Acme.TN "));

        assertNull(reponse.token());
        assertEquals(Role.ENTREPRISE, reponse.user().role());
        ArgumentCaptor<Entreprise> entreprise = ArgumentCaptor.forClass(Entreprise.class);
        verify(entrepriseRepository).save(entreprise.capture());
        assertEquals(StatutValidation.EN_ATTENTE, entreprise.getValue().getStatutValidation());
        assertEquals("contact@acme.tn", entreprise.getValue().getEmailContact());
        assertEquals("Tunis", entreprise.getValue().getVille());
        ArgumentCaptor<Utilisateur> compte = ArgumentCaptor.forClass(Utilisateur.class);
        verify(userRepository).save(compte.capture());
        assertFalse(compte.getValue().isActif());
        assertEquals("hash", compte.getValue().getMotDePasse());
        assertSame(entreprise.getValue(), compte.getValue().getEntreprise());
    }

    @Test
    void unEmailDejaUtiliseParUnCompteOuUneEntrepriseEstRefuse() {
        when(userRepository.existsByEmail("contact@acme.tn")).thenReturn(true);
        assertThrows(EmailAlreadyUsedException.class, () -> service.registerEntreprise(demande("contact@acme.tn")));

        when(userRepository.existsByEmail("rh@acme.tn")).thenReturn(false);
        when(entrepriseRepository.existsByEmailContact("rh@acme.tn")).thenReturn(true);
        assertThrows(EmailAlreadyUsedException.class, () -> service.registerEntreprise(demande("rh@acme.tn")));

        verify(entrepriseRepository, never()).save(any());
    }

    private CreateEntrepriseDTO demande(String email) {
        return new CreateEntrepriseDTO(" Acme ", "1 rue", " Tunis ", "Informatique", TailleEntreprise.PME,
                email, "70000000", "https://acme.tn", "motdepasse1");
    }
}
