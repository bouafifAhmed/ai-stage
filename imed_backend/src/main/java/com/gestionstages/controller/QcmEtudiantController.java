package com.gestionstages.controller;

import com.gestionstages.dto.QcmDTOs.QcmExamenResponse;
import com.gestionstages.dto.QcmDTOs.QcmStatutResponse;
import com.gestionstages.dto.QcmDTOs.SoumettreQcmRequest;
import com.gestionstages.dto.QcmDTOs.SoumettreQcmResponse;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.service.QcmEtudiantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/etudiant/qcm/admissibilite")
@PreAuthorize("hasRole('ETUDIANT')")
public class QcmEtudiantController {

    private final QcmEtudiantService qcmEtudiantService;

    public QcmEtudiantController(QcmEtudiantService qcmEtudiantService) {
        this.qcmEtudiantService = qcmEtudiantService;
    }

    @GetMapping
    public QcmExamenResponse obtenirQcm(@AuthenticationPrincipal Utilisateur utilisateur) {
        return qcmEtudiantService.obtenirQcmAdmissibilite(utilisateur);
    }

    @GetMapping("/statut")
    public QcmStatutResponse statut(@AuthenticationPrincipal Utilisateur utilisateur) {
        return qcmEtudiantService.statutAdmissibilite(utilisateur);
    }

    @PostMapping("/soumettre")
    public ResponseEntity<SoumettreQcmResponse> soumettre(
            @AuthenticationPrincipal Utilisateur utilisateur,
            @Valid @RequestBody SoumettreQcmRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(qcmEtudiantService.soumettreAdmissibilite(utilisateur, request));
    }
}
