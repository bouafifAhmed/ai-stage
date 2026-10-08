package com.gestionstages.controller;

import com.gestionstages.dto.StageSuiviDTOs.EcheanceCalendrierDTO;
import com.gestionstages.dto.StageDTOs.*;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.service.EtudiantStageService;
import com.gestionstages.service.StageSuiviService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/etudiant")
public class EtudiantStageController {
    private final EtudiantStageService service;
    private final StageSuiviService stageSuiviService;

    public EtudiantStageController(
            EtudiantStageService service,
            StageSuiviService stageSuiviService
    ) {
        this.service = service;
        this.stageSuiviService = stageSuiviService;
    }

    @GetMapping("/offres")
    public Page<OffreResponse> offres(@AuthenticationPrincipal Utilisateur user,
                                     @PageableDefault(size = 10, sort = "dateCreation",
                                             direction = Sort.Direction.DESC) Pageable pageable) {
        return service.offres(user, pageable);
    }
    @GetMapping("/offres/{id}")
    public OffreResponse offre(@AuthenticationPrincipal Utilisateur user, @PathVariable Long id) {
        return service.offre(user, id);
    }
    @GetMapping("/profil")
    public ProfilResponse profil(@AuthenticationPrincipal Utilisateur user) {
        return service.profil(user);
    }
    @PutMapping("/profil")
    public ProfilResponse modifierProfil(@AuthenticationPrincipal Utilisateur user,
                                         @Valid @RequestBody ProfilRequest request) {
        return service.modifierProfil(user, request);
    }
    @PostMapping(value = "/profil/cv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CvUploadResponse enregistrerCv(@AuthenticationPrincipal Utilisateur user,
                                          @RequestParam("file") MultipartFile file) {
        return service.enregistrerCv(user, file);
    }
    @PostMapping("/profil/cv/extraction")
    public CvExtractionResponse extraireCv(@AuthenticationPrincipal Utilisateur user) {
        return service.extraireCv(user);
    }
    @GetMapping(value = "/profil/cv", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> telechargerCv(@AuthenticationPrincipal Utilisateur user) {
        EtudiantStageService.CvDocument document = service.telechargerCv(user);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(document.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(document.resource());
    }
    @DeleteMapping("/profil/cv")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimerCv(@AuthenticationPrincipal Utilisateur user) {
        service.supprimerCv(user);
    }
    @PostMapping("/candidatures")
    public ResponseEntity<CandidatureResponse> postuler(@AuthenticationPrincipal Utilisateur user,
                                                        @Valid @RequestBody CandidatureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.postuler(user, request));
    }
    @GetMapping("/candidatures")
    public Page<CandidatureResponse> candidatures(@AuthenticationPrincipal Utilisateur user,
                                                  @PageableDefault(size = 10, sort = "dateCandidature",
                                                          direction = Sort.Direction.DESC) Pageable pageable) {
        return service.candidatures(user, pageable);
    }
    @GetMapping("/notifications")
    public Page<NotificationResponse> notifications(
            @AuthenticationPrincipal Utilisateur user,
            @PageableDefault(size = 20, sort = "dateCreation",
                    direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return service.notifications(user, pageable);
    }
    @GetMapping("/notifications/non-lues")
    public NotificationCountResponse notificationsNonLues(@AuthenticationPrincipal Utilisateur user) {
        return service.notificationsNonLues(user);
    }
    @PatchMapping("/notifications/{id}/lue")
    public NotificationResponse marquerNotificationLue(
            @AuthenticationPrincipal Utilisateur user,
            @PathVariable Long id
    ) {
        return service.marquerNotificationLue(user, id);
    }

    @GetMapping("/echeances")
    public List<EcheanceCalendrierDTO> echeances(
            @AuthenticationPrincipal Utilisateur user,
            @RequestParam @Min(2000) @Max(2100) int annee,
            @RequestParam @Min(1) @Max(12) int mois
    ) {
        return stageSuiviService.echeancesEtudiant(annee, mois, user);
    }
}
