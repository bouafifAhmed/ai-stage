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
class TacheControllerSecurityTest {

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
    void unauthenticatedUserReturns401() throws Exception {
        mockMvc.perform(get("/api/stages/1/journal"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void etudiantUserCanAccessJournal() throws Exception {
        com.gestionstages.model.Utilisateur user = new com.gestionstages.model.Utilisateur();
        user.setId(1L);
        user.setEmail("etudiant@test.com");
        user.setRole(com.gestionstages.model.Role.ETUDIANT);

        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        user, null, user.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(get("/api/stages/1/journal").principal(auth))
                .andExpect(status().isNotFound());
    }
}
