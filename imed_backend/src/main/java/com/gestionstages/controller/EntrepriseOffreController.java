package com.gestionstages.controller;

import com.gestionstages.dto.StageDTOs.*;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.service.EntrepriseStageService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entreprise")
public class EntrepriseOffreController {
    private final EntrepriseStageService service;
    public EntrepriseOffreController(EntrepriseStageService service) { this.service = service; }

    @PostMapping("/offres")
    public ResponseEntity<OffreResponse> creer(@AuthenticationPrincipal Utilisateur user,
                                               @Valid @RequestBody OffreRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(user, request));
    }
    @GetMapping("/offres")
    public Page<OffreResponse> lister(@AuthenticationPrincipal Utilisateur user,
                                     @PageableDefault(size = 10, sort = "dateCreation",
                                             direction = Sort.Direction.DESC) Pageable pageable) {
        return service.lister(user, pageable);
    }
    @GetMapping("/offres/{id}")
    public OffreResponse detail(@AuthenticationPrincipal Utilisateur user, @PathVariable Long id) {
        return service.detail(user, id);
    }
    @PutMapping("/offres/{id}")
    public OffreResponse modifier(@AuthenticationPrincipal Utilisateur user, @PathVariable Long id,
                                  @Valid @RequestBody OffreRequest request) {
        return service.modifier(user, id, request);
    }
    @DeleteMapping("/offres/{id}")
    public ResponseEntity<Void> supprimer(@AuthenticationPrincipal Utilisateur user, @PathVariable Long id) {
        service.supprimer(user, id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/offres/{offreId}/candidatures")
    public Page<CandidatureResponse> candidatures(@AuthenticationPrincipal Utilisateur user,
                                                  @PathVariable Long offreId,
                                                  @PageableDefault(size = 10, sort = "dateCandidature",
                                                          direction = Sort.Direction.DESC) Pageable pageable) {
        return service.candidatures(user, offreId, pageable);
    }
    @GetMapping("/candidatures/{id}")
    public CandidatureResponse candidature(@AuthenticationPrincipal Utilisateur user, @PathVariable Long id) {
        return service.candidature(user, id);
    }
    @PatchMapping("/candidatures/{id}/statut")
    public CandidatureResponse modifierStatutCandidature(
            @AuthenticationPrincipal Utilisateur user,
            @PathVariable Long id,
            @Valid @RequestBody CandidatureStatutRequest request
    ) {
        return service.modifierStatutCandidature(user, id, request);
    }
}
