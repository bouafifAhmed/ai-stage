package com.gestionstages.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilisateurTest {

    @Test
    void exposesRoleWithSpringSecurityPrefix() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail("chef@example.com");
        utilisateur.setMotDePasse("hash");
        utilisateur.setRole(Role.CHEF_DEPT_STAGE);
        utilisateur.setActif(true);

        assertEquals(
                "ROLE_CHEF_DEPT_STAGE",
                utilisateur.getAuthorities().iterator().next().getAuthority()
        );
        assertEquals("chef@example.com", utilisateur.getUsername());
        assertTrue(utilisateur.isEnabled());
    }
}
