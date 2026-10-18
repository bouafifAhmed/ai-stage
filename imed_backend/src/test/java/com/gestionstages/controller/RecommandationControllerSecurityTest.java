package com.gestionstages.controller;

import org.example.imed_backend.ImedBackendApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ImedBackendApplication.class)
class RecommandationControllerSecurityTest {

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
    void anonymeRecoit401SurLesTroisEndpoints() throws Exception {
        mockMvc.perform(get("/api/recommandations")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/recommandations/adequation/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/recommandations/health")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ENTREPRISE")
    void entrepriseRecoit403() throws Exception {
        mockMvc.perform(get("/api/recommandations").header("Authorization", "Bearer x"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/recommandations/health")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ETUDIANT")
    void etudiantPeutInterrogerLaSanteDuMoteur() throws Exception {
        // 200 que le service Python tourne ou non : le corps vaut alors true ou false
        mockMvc.perform(get("/api/recommandations/health")).andExpect(status().isOk());
    }
}
