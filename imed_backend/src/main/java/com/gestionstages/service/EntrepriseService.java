package com.gestionstages.service;

import com.gestionstages.dto.CreateEntrepriseDTO;
import com.gestionstages.dto.EntrepriseResponseDTO;
import com.gestionstages.dto.UpdateEntrepriseDTO;
import com.gestionstages.exception.EntrepriseEmailAlreadyUsedException;
import com.gestionstages.exception.EmailAlreadyUsedException;
import com.gestionstages.exception.EntrepriseNotFoundException;
import com.gestionstages.exception.InvalidEntrepriseStatusException;
import com.gestionstages.model.Entreprise;
import com.gestionstages.model.StatutValidation;
import com.gestionstages.model.Role;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.EntrepriseRepository;
import com.gestionstages.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class EntrepriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntrepriseService.class);
    private static final String ADMIN_DEACTIVATION_REASON = "Désactivée par le super admin";

    private final EntrepriseRepository entrepriseRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public EntrepriseService(
            EntrepriseRepository entrepriseRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.entrepriseRepository = entrepriseRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public EntrepriseResponseDTO creerEntrepriseParAdmin(CreateEntrepriseDTO dto) {
        String email = normalizeEmail(dto.emailContact());
        if (entrepriseRepository.existsByEmailContact(email)) {
            throw new EntrepriseEmailAlreadyUsedException();
        }
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }

        String nom = dto.nom().trim();
        warnIfNameAlreadyExists(nom);

        Entreprise entreprise = new Entreprise();
        entreprise.setNom(nom);
        entreprise.setAdresse(clean(dto.adresse()));
        entreprise.setVille(clean(dto.ville()));
        entreprise.setSecteurActivite(clean(dto.secteurActivite()));
        entreprise.setTaille(dto.taille());
        entreprise.setEmailContact(email);
        entreprise.setTelephone(clean(dto.telephone()));
        entreprise.setSiteWeb(clean(dto.siteWeb()));
        entreprise.setStatutValidation(StatutValidation.VALIDEE);
        entreprise.setDateInscription(LocalDateTime.now());

        Entreprise saved = entrepriseRepository.save(entreprise);
        creerCompteEntreprise(dto.nom(), email, dto.motDePasse(), saved);
        return EntrepriseResponseDTO.from(saved);
    }

    @Transactional
    public EntrepriseResponseDTO modifierEntreprise(Long id, UpdateEntrepriseDTO dto) {
        Entreprise entreprise = findById(id);

        if (dto.nom() != null) {
            String nom = dto.nom().trim();
            if (!nom.equals(entreprise.getNom())) {
                warnIfNameAlreadyExists(nom);
            }
            entreprise.setNom(nom);
        }
        if (dto.adresse() != null) {
            entreprise.setAdresse(dto.adresse().trim());
        }
        if (dto.ville() != null) {
            entreprise.setVille(dto.ville().trim());
        }
        if (dto.secteurActivite() != null) {
            entreprise.setSecteurActivite(dto.secteurActivite().trim());
        }
        if (dto.taille() != null) {
            entreprise.setTaille(dto.taille());
        }
        if (dto.emailContact() != null) {
            String email = normalizeEmail(dto.emailContact());
            if (!email.equals(entreprise.getEmailContact())
                    && entrepriseRepository.existsByEmailContact(email)) {
                throw new EntrepriseEmailAlreadyUsedException();
            }
            entreprise.setEmailContact(email);
        }
        if (dto.telephone() != null) {
            entreprise.setTelephone(dto.telephone().trim());
        }
        if (dto.siteWeb() != null) {
            entreprise.setSiteWeb(dto.siteWeb().trim());
        }

        return EntrepriseResponseDTO.from(entrepriseRepository.save(entreprise));
    }

    @Transactional
    public void desactiverEntreprise(Long id) {
        Entreprise entreprise = findById(id);
        entreprise.setStatutValidation(StatutValidation.REJETEE);
        entreprise.setMotifRejet(ADMIN_DEACTIVATION_REASON);
        entrepriseRepository.save(entreprise);
        setCompteActif(entreprise.getEmailContact(), false);
    }

    @Transactional
    public EntrepriseResponseDTO validerEntreprise(Long id) {
        Entreprise entreprise = findById(id);
        entreprise.setStatutValidation(StatutValidation.VALIDEE);
        entreprise.setMotifRejet(null);
        setCompteActif(entreprise.getEmailContact(), true);
        return EntrepriseResponseDTO.from(entrepriseRepository.save(entreprise));
    }

    @Transactional(readOnly = true)
    public Page<EntrepriseResponseDTO> listerEntreprises(String statut, Pageable pageable) {
        Page<Entreprise> entreprises;
        if (statut == null || statut.isBlank()) {
            entreprises = entrepriseRepository.findAll(pageable);
        } else {
            StatutValidation statutValidation = parseStatus(statut);
            entreprises = entrepriseRepository.findAllByStatutValidation(
                    statutValidation,
                    pageable
            );
        }
        return entreprises.map(EntrepriseResponseDTO::from);
    }

    private Entreprise findById(Long id) {
        return entrepriseRepository.findById(id)
                .orElseThrow(() -> new EntrepriseNotFoundException(id));
    }

    private void setCompteActif(String email, boolean actif) {
        userRepository.findByEmail(normalizeEmail(email)).ifPresent(utilisateur -> {
            utilisateur.setActif(actif);
            if (utilisateur.getEntreprise() == null) {
                entrepriseRepository.findByEmailContact(normalizeEmail(email))
                        .ifPresent(utilisateur::setEntreprise);
            }
            userRepository.save(utilisateur);
        });
    }

    private void creerCompteEntreprise(String nom, String email, String motDePasse, Entreprise entreprise) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(nom.trim());
        utilisateur.setPrenom("Entreprise");
        utilisateur.setEmail(email);
        utilisateur.setMotDePasse(passwordEncoder.encode(motDePasse));
        utilisateur.setRole(Role.ENTREPRISE);
        utilisateur.setActif(true);
        utilisateur.setEntreprise(entreprise);
        userRepository.save(utilisateur);
    }

    private void warnIfNameAlreadyExists(String nom) {
        if (entrepriseRepository.existsByNom(nom)) {
            LOGGER.warn("Une entreprise portant le nom '{}' existe déjà", nom);
        }
    }

    private StatutValidation parseStatus(String statut) {
        try {
            return StatutValidation.valueOf(statut.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new InvalidEntrepriseStatusException(statut);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
