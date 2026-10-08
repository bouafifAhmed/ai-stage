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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ImedBackendApplication.class)
class AdminEtudiantSecurityTest {

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
    @WithMockUser(roles = "CHEF_DEPT_STAGE")
    void chefDepartementStageCannotListerEtudiants() throws Exception {
        mockMvc.perform(get("/api/admin/etudiants"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CHEF_DEPT_STAGE")
    void chefDepartementStageCannotCreerEtudiant() throws Exception {
        mockMvc.perform(post("/api/admin/etudiants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nom": "Doe",
                                  "prenom": "Lina",
                                  "email": "lina@example.com",
                                  "motDePasse": "motdepasse"
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}
