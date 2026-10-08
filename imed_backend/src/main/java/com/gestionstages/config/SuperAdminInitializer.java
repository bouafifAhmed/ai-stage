package com.gestionstages.config;

import com.gestionstages.model.Role;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
@ConditionalOnProperty(
        prefix = "app.super-admin",
        name = "enabled",
        havingValue = "true"
)
public class SuperAdminInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SuperAdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String nom;
    private final String prenom;

    public SuperAdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.super-admin.email:}") String email,
            @Value("${app.super-admin.password:}") String password,
            @Value("${app.super-admin.nom:Super}") String nom,
            @Value("${app.super-admin.prenom:Admin}") String prenom
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.nom = nom;
        this.prenom = prenom;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        validateConfiguration();
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        userRepository.findByEmail(normalizedEmail).ifPresentOrElse(existingUser -> {
            if (existingUser.getRole() != Role.SUPER_ADMIN) {
                throw new IllegalStateException(
                        "L'email du super administrateur appartient déjà à un autre rôle"
                );
            }
            LOGGER.info("Le compte SUPER_ADMIN {} existe déjà", normalizedEmail);
        }, () -> createSuperAdmin(normalizedEmail));
    }

    private void createSuperAdmin(String normalizedEmail) {
        Utilisateur superAdmin = new Utilisateur();
        superAdmin.setNom(nom.trim());
        superAdmin.setPrenom(prenom.trim());
        superAdmin.setEmail(normalizedEmail);
        superAdmin.setMotDePasse(passwordEncoder.encode(password));
        superAdmin.setRole(Role.SUPER_ADMIN);
        superAdmin.setActif(true);
        userRepository.save(superAdmin);
        LOGGER.info("Compte SUPER_ADMIN {} créé", normalizedEmail);
    }

    private void validateConfiguration() {
        if (email.isBlank() || !email.contains("@")) {
            throw new IllegalStateException("SUPER_ADMIN_EMAIL doit contenir une adresse valide");
        }
        if (password.length() < 8 || password.length() > 72) {
            throw new IllegalStateException(
                    "SUPER_ADMIN_PASSWORD doit contenir entre 8 et 72 caractères"
            );
        }
        if (nom.isBlank() || prenom.isBlank()) {
            throw new IllegalStateException(
                    "SUPER_ADMIN_NOM et SUPER_ADMIN_PRENOM ne peuvent pas être vides"
            );
        }
    }
}
