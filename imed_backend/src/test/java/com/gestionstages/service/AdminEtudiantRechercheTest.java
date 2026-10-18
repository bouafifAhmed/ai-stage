package com.gestionstages.service;

import com.gestionstages.model.Role;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Liste des étudiants côté super admin : chaque combinaison de filtres utilise la bonne requête.
 */
class AdminEtudiantRechercheTest {
    private final PageRequest page = PageRequest.of(0, 20);
    private UserRepository userRepository;
    private AdminEtudiantService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new AdminEtudiantService(userRepository, mock(PasswordEncoder.class));
        PageImpl<Utilisateur> un = new PageImpl<>(List.of(etudiant()));
        when(userRepository.findByRoleAndActifAndFiliereContainingIgnoreCase(Role.ETUDIANT, true, "info", page)).thenReturn(un);
        when(userRepository.findByRoleAndActif(Role.ETUDIANT, false, page)).thenReturn(un);
        when(userRepository.findByRoleAndFiliereContainingIgnoreCase(Role.ETUDIANT, "info", page)).thenReturn(un);
        when(userRepository.findByRole(Role.ETUDIANT, page)).thenReturn(un);
    }

    @Test
    void filtreParActifEtFiliere() {
        assertEquals(1, service.listerEtudiants(true, " info ", page).getTotalElements());
        verify(userRepository).findByRoleAndActifAndFiliereContainingIgnoreCase(Role.ETUDIANT, true, "info", page);
    }

    @Test
    void filtreParActifSeulement() {
        service.listerEtudiants(false, "  ", page);
        verify(userRepository).findByRoleAndActif(Role.ETUDIANT, false, page);
    }

    @Test
    void filtreParFiliereSeulement() {
        service.listerEtudiants(null, "info", page);
        verify(userRepository).findByRoleAndFiliereContainingIgnoreCase(Role.ETUDIANT, "info", page);
    }

    @Test
    void sansFiltreListeTousLesEtudiants() {
        assertEquals("lina@example.com", service.listerEtudiants(null, null, page).getContent().get(0).email());
        verify(userRepository).findByRole(Role.ETUDIANT, page);
    }

    private Utilisateur etudiant() {
        Utilisateur u = new Utilisateur();
        u.setId(2L);
        u.setRole(Role.ETUDIANT);
        u.setNom("Doe");
        u.setPrenom("Lina");
        u.setEmail("lina@example.com");
        u.setActif(true);
        return u;
    }
}
