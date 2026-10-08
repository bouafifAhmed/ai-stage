package com.gestionstages.controller;

import com.gestionstages.dto.CompleterTacheDTO;
import com.gestionstages.dto.CreateTacheAssigneeDTO;
import com.gestionstages.dto.TacheAssigneeResponseDTO;
import com.gestionstages.dto.ValiderTacheDTO;
import com.gestionstages.model.StatutTacheAssignee;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.service.TacheAssigneeService;
import com.gestionstages.service.TacheAttachmentStorageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/stages/{stageId}/taches-assignees")
public class TacheAssigneeController {
    private final TacheAssigneeService service;

    public TacheAssigneeController(TacheAssigneeService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ENTREPRISE')")
    public ResponseEntity<TacheAssigneeResponseDTO> assigner(
            @PathVariable Long stageId,
            @Valid @RequestBody CreateTacheAssigneeDTO dto,
            @AuthenticationPrincipal Utilisateur utilisateur
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.assignerTache(stageId, dto, utilisateur));
    }

    @PutMapping(value = "/{tacheId}/terminer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ETUDIANT')")
    public TacheAssigneeResponseDTO terminer(
            @PathVariable Long stageId,
            @PathVariable Long tacheId,
            @RequestParam(name = "commentaireEtudiant", required = false)
            @Size(max = 20000, message = "Le commentaire ne doit pas dépasser 20000 caractères")
            String commentaireEtudiant,
            @RequestPart(name = "pieceJointe", required = false) MultipartFile pieceJointe,
            @AuthenticationPrincipal Utilisateur utilisateur
    ) {
        CompleterTacheDTO dto = new CompleterTacheDTO(commentaireEtudiant, null);
        return service.marquerCommeTerminee(stageId, tacheId, dto, pieceJointe, utilisateur);
    }

    @PutMapping("/{tacheId}/valider")
    @PreAuthorize("hasRole('ENTREPRISE')")
    public TacheAssigneeResponseDTO valider(
            @PathVariable Long stageId,
            @PathVariable Long tacheId,
            @Valid @RequestBody ValiderTacheDTO dto,
            @AuthenticationPrincipal Utilisateur utilisateur
    ) {
        return service.validerTache(stageId, tacheId, dto, utilisateur);
    }

    @PutMapping("/{tacheId}/rejeter")
    @PreAuthorize("hasRole('ENTREPRISE')")
    public TacheAssigneeResponseDTO rejeter(
            @PathVariable Long stageId,
            @PathVariable Long tacheId,
            @Valid @RequestBody ValiderTacheDTO dto,
            @AuthenticationPrincipal Utilisateur utilisateur
    ) {
        return service.rejeterTache(stageId, tacheId, dto, utilisateur);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ETUDIANT','ENTREPRISE','CHEF_DEPT_STAGE')")
    public List<TacheAssigneeResponseDTO> lister(
            @PathVariable Long stageId,
            @RequestParam(required = false) StatutTacheAssignee statut,
            @AuthenticationPrincipal Utilisateur utilisateur
    ) {
        return service.listerTachesParStage(stageId, statut, utilisateur);
    }

    @GetMapping("/{tacheId}/piece-jointe")
    @PreAuthorize("hasAnyRole('ETUDIANT','ENTREPRISE','CHEF_DEPT_STAGE')")
    public ResponseEntity<Resource> telechargerPieceJointe(
            @PathVariable Long stageId,
            @PathVariable Long tacheId,
            @AuthenticationPrincipal Utilisateur utilisateur
    ) {
        TacheAttachmentStorageService.AttachmentDocument document =
                service.telechargerPieceJointe(stageId, tacheId, utilisateur);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(document.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(document.resource());
    }
}
