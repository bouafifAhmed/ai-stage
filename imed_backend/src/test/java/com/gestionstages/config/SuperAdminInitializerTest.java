package com.gestionstages.config;

import com.gestionstages.model.Role;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuperAdminInitializerTest {

    @Test
    void createsSuperAdminWhenAccountDoesNotExist() {
        UserRepository repository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(repository.findByEmail("root@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("motdepasse")).thenReturn("bcrypt");

        SuperAdminInitializer initializer = new SuperAdminInitializer(
                repository,
                passwordEncoder,
                "ROOT@EXAMPLE.COM",
                "motdepasse",
                "Administrateur",
                "Principal"
        );
        initializer.run(null);

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(repository).save(captor.capture());
        Utilisateur saved = captor.getValue();
        assertEquals("root@example.com", saved.getEmail());
        assertEquals("bcrypt", saved.getMotDePasse());
        assertEquals(Role.SUPER_ADMIN, saved.getRole());
    }
}
