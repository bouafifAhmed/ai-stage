package com.gestionstages.service;

import com.gestionstages.dto.AuthResponseDTO;
import com.gestionstages.dto.LoginRequestDTO;
import com.gestionstages.dto.RegisterRequestDTO;
import com.gestionstages.exception.AccountPendingApprovalException;
import com.gestionstages.exception.EmailAlreadyUsedException;
import com.gestionstages.exception.InvalidCredentialsException;
import com.gestionstages.exception.RoleNotAllowedException;
import com.gestionstages.model.Role;
import com.gestionstages.model.Entreprise;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.EntrepriseRepository;
import com.gestionstages.repository.UserRepository;
import com.gestionstages.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JwtTokenProvider jwtTokenProvider;
    private EntrepriseRepository entrepriseRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authenticationManager = mock(AuthenticationManager.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        entrepriseRepository = mock(EntrepriseRepository.class);
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                authenticationManager,
                jwtTokenProvider,
                entrepriseRepository
        );
    }

    @Test
    void registerCreatesUserWithHashedPassword() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "Dupont",
                "Lina",
                "LINA@EXAMPLE.COM",
                "motdepasse",
                Role.ETUDIANT
        );
        when(userRepository.existsByEmail("lina@example.com")).thenReturn(false);
        when(passwordEncoder.encode("motdepasse")).thenReturn("bcrypt");
        when(userRepository.save(any(Utilisateur.class))).thenAnswer(invocation -> {
            Utilisateur utilisateur = invocation.getArgument(0);
            utilisateur.setId(7L);
            return utilisateur;
        });
        when(jwtTokenProvider.generateToken(any(Utilisateur.class))).thenReturn("jwt");

        AuthResponseDTO response = authService.register(request);

        assertEquals("jwt", response.token());
        assertEquals("Bearer", response.type());
        assertEquals("lina@example.com", response.user().email());
        verify(passwordEncoder).encode("motdepasse");
        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(userRepository).save(captor.capture());
        assertEquals("bcrypt", captor.getValue().getMotDePasse());
    }

    @Test
    void registerRejectsExistingEmail() {
        when(userRepository.existsByEmail("lina@example.com")).thenReturn(true);
        RegisterRequestDTO request = new RegisterRequestDTO(
                "Dupont",
                "Lina",
                "lina@example.com",
                "motdepasse",
                Role.ETUDIANT
        );

        assertThrows(EmailAlreadyUsedException.class, () -> authService.register(request));
    }

    @Test
    void registerRejectsPublicSuperAdminCreation() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "Admin",
                "Super",
                "super@example.com",
                "motdepasse",
                Role.SUPER_ADMIN
        );

        assertThrows(RoleNotAllowedException.class, () -> authService.register(request));
    }

    @Test
    void registerEntrepriseCreatesInactiveAccountPendingApproval() {
        RegisterRequestDTO request = new RegisterRequestDTO(
                "Acme",
                "Contact",
                "contact@acme.tn",
                "motdepasse",
                Role.ENTREPRISE
        );
        when(passwordEncoder.encode("motdepasse")).thenReturn("bcrypt");
        when(userRepository.save(any(Utilisateur.class))).thenAnswer(invocation -> {
            Utilisateur utilisateur = invocation.getArgument(0);
            utilisateur.setId(8L);
            return utilisateur;
        });
        when(entrepriseRepository.save(any(Entreprise.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponseDTO response = authService.register(request);

        assertNull(response.token());
        ArgumentCaptor<Utilisateur> userCaptor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(userRepository).save(userCaptor.capture());
        assertFalse(userCaptor.getValue().isActif());
        assertNotNull(userCaptor.getValue().getEntreprise());
        verify(entrepriseRepository).save(any(Entreprise.class));
    }

    @Test
    void loginRejectsEntreprisePendingApproval() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setRole(Role.ENTREPRISE);
        utilisateur.setActif(false);
        when(userRepository.findByEmail("contact@acme.tn"))
                .thenReturn(Optional.of(utilisateur));

        assertThrows(
                AccountPendingApprovalException.class,
                () -> authService.login(
                        new LoginRequestDTO("contact@acme.tn", "motdepasse")
                )
        );
    }

    @Test
    void loginRejectsInvalidCredentials() {
        LoginRequestDTO request = new LoginRequestDTO("lina@example.com", "incorrect");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("invalid"));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void loginReturnsTokenForAuthenticatedUser() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setId(7L);
        utilisateur.setNom("Dupont");
        utilisateur.setPrenom("Lina");
        utilisateur.setEmail("lina@example.com");
        utilisateur.setMotDePasse("bcrypt");
        utilisateur.setRole(Role.ETUDIANT);
        when(userRepository.findByEmail("lina@example.com")).thenReturn(Optional.of(utilisateur));
        when(jwtTokenProvider.generateToken(utilisateur)).thenReturn("jwt");

        AuthResponseDTO response =
                authService.login(new LoginRequestDTO("lina@example.com", "motdepasse"));

        assertEquals("jwt", response.token());
        assertEquals(7L, response.user().id());
    }

}
