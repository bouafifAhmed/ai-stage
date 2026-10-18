package com.gestionstages.security;

import com.gestionstages.model.Role;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtSecurityTest {
    private static final String SECRET = "Z2VzdGlvbi1zdGFnZXMtanVuaXQtdGVzdC1zZWNyZXQtMjAyNg==";

    private JwtTokenProvider provider;
    private UserRepository users;
    private JwtAuthenticationFilter filter;
    private Utilisateur etudiant;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, 60_000);
        users = mock(UserRepository.class);
        filter = new JwtAuthenticationFilter(provider, new UserDetailsServiceImpl(users));
        etudiant = utilisateur("lina@example.com", true);
        when(users.findByEmail("lina@example.com")).thenReturn(Optional.of(etudiant));
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void unJetonGenereEstValidePourSonUtilisateur() {
        String jeton = provider.generateToken(etudiant);

        assertEquals("lina@example.com", provider.extractUsername(jeton));
        assertTrue(provider.validateToken(jeton, etudiant));
    }

    @Test
    void unJetonNEstPasValidePourUnAutreUtilisateurNiPourUnCompteDesactive() {
        String jeton = provider.generateToken(etudiant);

        assertFalse(provider.validateToken(jeton, utilisateur("autre@example.com", true)));
        assertFalse(provider.validateToken(jeton, utilisateur("lina@example.com", false)));
    }

    @Test
    void unJetonModifieOuExpireEstRefuse() {
        String jeton = provider.generateToken(etudiant);
        String modifie = jeton.substring(0, jeton.length() - 2) + (jeton.endsWith("AA") ? "BB" : "AA");
        String expire = new JwtTokenProvider(SECRET, -1_000).generateToken(etudiant);

        assertFalse(provider.validateToken(modifie, etudiant));
        assertFalse(provider.validateToken(expire, etudiant));
        assertThrows(JwtException.class, () -> provider.extractUsername("pas-un-jeton"));
    }

    @Test
    void unJetonSigneAvecUneAutreCleEstRefuse() {
        JwtTokenProvider autreCle = new JwtTokenProvider(
                "YXV0cmUtc2VjcmV0LWRlLWRldmVsb3BwZW1lbnQtMzItb2N0ZXRz", 60_000);

        assertFalse(provider.validateToken(autreCle.generateToken(etudiant), etudiant));
    }

    @Test
    void leFiltreAuthentifieUneRequeteAvecUnJetonValide() throws ServletException, IOException {
        MockFilterChain chaine = new MockFilterChain();

        filter.doFilter(requete("Bearer " + provider.generateToken(etudiant)), new MockHttpServletResponse(), chaine);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertSame(etudiant, auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ETUDIANT")));
        assertNotNull(chaine.getRequest());
    }

    @Test
    void leFiltreLaissePasserSansAuthentifierUnJetonInvalideOuAbsent() throws ServletException, IOException {
        MockFilterChain invalide = new MockFilterChain();
        filter.doFilter(requete("Bearer n-importe-quoi"), new MockHttpServletResponse(), invalide);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(invalide.getRequest());

        MockFilterChain sansEntete = new MockFilterChain();
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), sansEntete);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(sansEntete.getRequest());

        filter.doFilter(requete("Basic abc"), new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void leFiltreRefuseUnCompteDesactiveOuInconnu() throws ServletException, IOException {
        Utilisateur desactive = utilisateur("off@example.com", false);
        when(users.findByEmail("off@example.com")).thenReturn(Optional.of(desactive));
        filter.doFilter(requete("Bearer " + provider.generateToken(desactive)), new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        String jetonInconnu = provider.generateToken(utilisateur("inconnu@example.com", true));
        filter.doFilter(requete("Bearer " + jetonInconnu), new MockHttpServletResponse(), new MockFilterChain());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void leFiltreNeRemplacePasUneAuthentificationExistante() throws ServletException, IOException {
        UsernamePasswordAuthenticationToken existante =
                new UsernamePasswordAuthenticationToken("deja", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(existante);

        filter.doFilter(requete("Bearer " + provider.generateToken(etudiant)), new MockHttpServletResponse(), new MockFilterChain());

        assertSame(existante, SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(users);
    }

    @Test
    void leChargementDUtilisateurNormaliseLEmail() {
        UserDetailsServiceImpl service = new UserDetailsServiceImpl(users);

        assertSame(etudiant, service.loadUserByUsername("  Lina@Example.com "));
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("absent@example.com"));
    }

    private MockHttpServletRequest requete(String authorization) {
        MockHttpServletRequest requete = new MockHttpServletRequest();
        requete.addHeader("Authorization", authorization);
        return requete;
    }

    private Utilisateur utilisateur(String email, boolean actif) {
        Utilisateur u = new Utilisateur();
        u.setEmail(email);
        u.setRole(Role.ETUDIANT);
        u.setActif(actif);
        return u;
    }
}
