package com.gestionstages.controller;

import org.example.imed_backend.ImedBackendApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chaque espace n'est accessible qu'au bon rôle, et les endpoints publics valident leurs entrées.
 */
@SpringBootTest(classes = ImedBackendApplication.class)
class RoleAccessSecurityTest {

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void unAnonymeEstRefuseHorsDeLAuthentification() throws Exception {
        mockMvc.perform(get("/api/etudiant/profil")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/entreprise/offres")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/stages/1/echeances")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ENTREPRISE")
    void uneEntrepriseNAccedePasALEspaceEtudiantNiAuQcm() throws Exception {
        mockMvc.perform(get("/api/etudiant/profil"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Accès refusé"));
        mockMvc.perform(get("/api/etudiant/qcm/admissibilite/statut")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ETUDIANT")
    void unEtudiantNAccedePasALEspaceEntrepriseNiALAdministration() throws Exception {
        mockMvc.perform(get("/api/entreprise/offres")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/entreprises")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ETUDIANT")
    void unEtudiantNePeutPasSignerALaPlaceDeLEncadrant() throws Exception {
        mockMvc.perform(post("/api/stages/1/signature/encadrant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"signatureBase64\":\"aGVsbG8=\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void leSuperAdminNAccedePasAuSuiviDesStages() throws Exception {
        mockMvc.perform(get("/api/stages/1/journal")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CHEF_DEPT_STAGE")
    void unChefDeDepartementNePeutPasValiderUneEntreprise() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/entreprises/1/validation"))
                .andExpect(status().isForbidden());
    }

    @Test
    void laConnexionValideSesChampsAvantToutTraitement() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pas-un-email\",\"motDePasse\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.motDePasse").exists());
    }

    @Test
    void desIdentifiantsInconnusDonnent401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"inconnu@example.com\",\"motDePasse\":\"motdepasse1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unCorpsJsonIllisibleDonne400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{pas du json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le corps de la requête est invalide"));
    }
}
