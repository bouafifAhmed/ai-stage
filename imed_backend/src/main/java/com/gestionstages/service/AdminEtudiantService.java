package com.gestionstages.service;

import com.gestionstages.dto.CreateEtudiantDTO;
import com.gestionstages.dto.EtudiantResponseDTO;
import com.gestionstages.dto.UpdateEtudiantDTO;
import com.gestionstages.exception.EmailAlreadyUsedException;
import com.gestionstages.exception.EtudiantNotFoundException;
import com.gestionstages.model.Role;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AdminEtudiantService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminEtudiantService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public EtudiantResponseDTO creerEtudiant(CreateEtudiantDTO dto) {
        String email = normalizeEmail(dto.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }

        Utilisateur etudiant = new Utilisateur();
        etudiant.setNom(dto.nom().trim());
        etudiant.setPrenom(dto.prenom().trim());
        etudiant.setEmail(email);
        etudiant.setMotDePasse(passwordEncoder.encode(dto.motDePasse()));
        etudiant.setRole(Role.ETUDIANT);
        etudiant.setActif(true);
        etudiant.setTelephone(clean(dto.telephone()));
        etudiant.setFiliere(clean(dto.filiere()));
        etudiant.setNiveauEtudes(clean(dto.niveauEtudes()));
        etudiant.setCompetences(normalizeCompetences(dto.competences()));

        return EtudiantResponseDTO.from(userRepository.save(etudiant));
    }

    @Transactional
    public EtudiantResponseDTO modifierEtudiant(Long id, UpdateEtudiantDTO dto) {
        Utilisateur etudiant = findEtudiant(id);

        if (dto.nom() != null) {
            etudiant.setNom(dto.nom().trim());
        }
        if (dto.prenom() != null) {
            etudiant.setPrenom(dto.prenom().trim());
        }
        if (dto.email() != null) {
            String email = normalizeEmail(dto.email());
            if (!email.equals(etudiant.getEmail()) && userRepository.existsByEmail(email)) {
                throw new EmailAlreadyUsedException();
            }
            etudiant.setEmail(email);
        }
        if (dto.telephone() != null) {
            etudiant.setTelephone(dto.telephone().trim());
        }
        if (dto.filiere() != null) {
            etudiant.setFiliere(dto.filiere().trim());
        }
        if (dto.niveauEtudes() != null) {
            etudiant.setNiveauEtudes(dto.niveauEtudes().trim());
        }
        if (dto.competences() != null) {
            etudiant.setCompetences(normalizeCompetences(dto.competences()));
        }

        return EtudiantResponseDTO.from(userRepository.save(etudiant));
    }

    @Transactional
    public void desactiverEtudiant(Long id) {
        Utilisateur etudiant = findEtudiant(id);
        etudiant.setActif(false);
        userRepository.save(etudiant);
    }

    @Transactional
    public EtudiantResponseDTO activerEtudiant(Long id) {
        Utilisateur etudiant = findEtudiant(id);
        etudiant.setActif(true);
        return EtudiantResponseDTO.from(userRepository.save(etudiant));
    }

    @Transactional(readOnly = true)
    public Page<EtudiantResponseDTO> listerEtudiants(Boolean actif, String filiere, Pageable pageable) {
        Page<Utilisateur> etudiants;
        boolean hasActif = actif != null;
        boolean hasFiliere = filiere != null && !filiere.isBlank();

        if (hasActif && hasFiliere) {
            etudiants = userRepository.findByRoleAndActifAndFiliereContainingIgnoreCase(
                    Role.ETUDIANT,
                    actif,
                    filiere.trim(),
                    pageable
            );
        } else if (hasActif) {
            etudiants = userRepository.findByRoleAndActif(Role.ETUDIANT, actif, pageable);
        } else if (hasFiliere) {
            etudiants = userRepository.findByRoleAndFiliereContainingIgnoreCase(
                    Role.ETUDIANT,
                    filiere.trim(),
                    pageable
            );
        } else {
            etudiants = userRepository.findByRole(Role.ETUDIANT, pageable);
        }

        return etudiants.map(EtudiantResponseDTO::from);
    }

    private Utilisateur findEtudiant(Long id) {
        return userRepository.findByIdAndRole(id, Role.ETUDIANT)
                .orElseThrow(() -> new EtudiantNotFoundException(id));
    }

    private List<String> normalizeCompetences(List<String> competences) {
        if (competences == null) {
            return new ArrayList<>();
        }
        return competences.stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
