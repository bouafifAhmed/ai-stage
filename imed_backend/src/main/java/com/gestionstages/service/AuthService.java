package com.gestionstages.service;

import com.gestionstages.dto.AuthResponseDTO;
import com.gestionstages.dto.CreateEntrepriseDTO;
import com.gestionstages.dto.LoginRequestDTO;
import com.gestionstages.dto.RegisterRequestDTO;
import com.gestionstages.dto.UserResponseDTO;
import com.gestionstages.exception.AccountPendingApprovalException;
import com.gestionstages.exception.EmailAlreadyUsedException;
import com.gestionstages.exception.InvalidCredentialsException;
import com.gestionstages.exception.RoleNotAllowedException;
import com.gestionstages.model.Entreprise;
import com.gestionstages.model.Role;
import com.gestionstages.model.StatutValidation;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.repository.EntrepriseRepository;
import com.gestionstages.repository.UserRepository;
import com.gestionstages.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final EntrepriseRepository entrepriseRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            EntrepriseRepository entrepriseRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.entrepriseRepository = entrepriseRepository;
    }

    @Transactional
    public AuthResponseDTO register(RegisterRequestDTO request) {
        if (request.role() == Role.SUPER_ADMIN) {
            throw new RoleNotAllowedException();
        }

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException();
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(request.nom().trim());
        utilisateur.setPrenom(request.prenom().trim());
        utilisateur.setEmail(email);
        utilisateur.setMotDePasse(passwordEncoder.encode(request.motDePasse()));
        utilisateur.setRole(request.role());
        utilisateur.setActif(request.role() != Role.ENTREPRISE);

        if (request.role() == Role.ENTREPRISE) {
            if (entrepriseRepository.existsByEmailContact(email)) {
                throw new EmailAlreadyUsedException();
            }

            Entreprise entreprise = new Entreprise();
            entreprise.setNom(request.nom().trim());
            entreprise.setEmailContact(email);
            entreprise.setStatutValidation(StatutValidation.EN_ATTENTE);
            Entreprise savedEntreprise = entrepriseRepository.save(entreprise);
            utilisateur.setEntreprise(savedEntreprise);
            Utilisateur saved = userRepository.save(utilisateur);

            return new AuthResponseDTO(null, UserResponseDTO.from(saved));
        }

        Utilisateur saved = userRepository.save(utilisateur);
        String token = jwtTokenProvider.generateToken(saved);
        return new AuthResponseDTO(token, UserResponseDTO.from(saved));
    }

    @Transactional
    public AuthResponseDTO registerEntreprise(CreateEntrepriseDTO request) {
        String email = normalizeEmail(request.emailContact());
        if (userRepository.existsByEmail(email)
                || entrepriseRepository.existsByEmailContact(email)) {
            throw new EmailAlreadyUsedException();
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(request.nom().trim());
        utilisateur.setPrenom("Entreprise");
        utilisateur.setEmail(email);
        utilisateur.setMotDePasse(passwordEncoder.encode(request.motDePasse()));
        utilisateur.setRole(Role.ENTREPRISE);
        utilisateur.setActif(false);
        Entreprise entreprise = new Entreprise();
        entreprise.setNom(request.nom().trim());
        entreprise.setAdresse(clean(request.adresse()));
        entreprise.setVille(clean(request.ville()));
        entreprise.setSecteurActivite(clean(request.secteurActivite()));
        entreprise.setTaille(request.taille());
        entreprise.setEmailContact(email);
        entreprise.setTelephone(clean(request.telephone()));
        entreprise.setSiteWeb(clean(request.siteWeb()));
        entreprise.setStatutValidation(StatutValidation.EN_ATTENTE);
        Entreprise savedEntreprise = entrepriseRepository.save(entreprise);
        utilisateur.setEntreprise(savedEntreprise);
        Utilisateur savedUser = userRepository.save(utilisateur);

        return new AuthResponseDTO(null, UserResponseDTO.from(savedUser));
    }

    @Transactional
    public AuthResponseDTO login(LoginRequestDTO request) {
        String email = normalizeEmail(request.email());
        userRepository.findByEmail(email)
                .filter(user -> user.getRole() == Role.ENTREPRISE && !user.isActif())
                .ifPresent(user -> {
                    throw new AccountPendingApprovalException();
                });

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.motDePasse())
            );
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }

        Utilisateur utilisateur = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);
        if (utilisateur.getRole() == Role.ENTREPRISE
                && utilisateur.getEntreprise() == null) {
            entrepriseRepository.findByEmailContact(email).ifPresent(entreprise -> {
                utilisateur.setEntreprise(entreprise);
                userRepository.save(utilisateur);
            });
        }
        String token = jwtTokenProvider.generateToken(utilisateur);
        return new AuthResponseDTO(token, UserResponseDTO.from(utilisateur));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
