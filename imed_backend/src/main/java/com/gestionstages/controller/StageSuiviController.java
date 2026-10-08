package com.gestionstages.controller;

import com.gestionstages.dto.StageSuiviDTOs.EcheanceCalendrierDTO;
import com.gestionstages.dto.StageSuiviDTOs.SignerStageRequest;
import com.gestionstages.dto.StageSuiviDTOs.StageClotureResponseDTO;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.service.StageSuiviService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/stages/{stageId}")
public class StageSuiviController {
    private final StageSuiviService service;

    public StageSuiviController(StageSuiviService service) {
        this.service = service;
    }

    @GetMapping("/echeances")
    @PreAuthorize("hasAnyRole('ETUDIANT','ENTREPRISE','CHEF_DEPT_STAGE')")
    public List<EcheanceCalendrierDTO> echeances(
            @PathVariable Long stageId,
            @RequestParam @Min(2000) @Max(2100) int annee,
            @RequestParam @Min(1) @Max(12) int mois,
            @AuthenticationPrincipal Utilisateur utilisateur
    ) {
        return service.echeancesStage(stageId, annee, mois, utilisateur);
    }

    @GetMapping(value = "/rapport-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ETUDIANT','ENTREPRISE','CHEF_DEPT_STAGE')")
    public ResponseEntity<byte[]> rapportPdf(
            @PathVariable Long stageId,
            @AuthenticationPrincipal Utilisateur utilisateur
    ) {
        byte[] pdf = service.telechargerRapport(stageId, utilisateur);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("rapport-stage-" + stageId + ".pdf", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(pdf);
    }

    @GetMapping("/cloture")
    @PreAuthorize("hasAnyRole('ETUDIANT','ENTREPRISE','CHEF_DEPT_STAGE')")
    public StageClotureResponseDTO cloture(
            @PathVariable Long stageId,
            @AuthenticationPrincipal Utilisateur utilisateur
    ) {
        return service.etatCloture(stageId, utilisateur);
    }

    @PostMapping("/signature/etudiant")
    @PreAuthorize("hasRole('ETUDIANT')")
    public StageClotureResponseDTO signerEtudiant(
            @PathVariable Long stageId,
            @Valid @RequestBody SignerStageRequest request,
            @AuthenticationPrincipal Utilisateur utilisateur
    ) {
        return service.signerEtudiant(stageId, request, utilisateur);
    }

    @PostMapping("/signature/encadrant")
    @PreAuthorize("hasRole('ENTREPRISE')")
    public StageClotureResponseDTO signerEncadrant(
            @PathVariable Long stageId,
            @Valid @RequestBody SignerStageRequest request,
            @AuthenticationPrincipal Utilisateur utilisateur
    ) {
        return service.signerEncadrant(stageId, request, utilisateur);
    }
}
