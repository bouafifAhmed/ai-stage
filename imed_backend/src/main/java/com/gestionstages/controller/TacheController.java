package com.gestionstages.controller;

import com.gestionstages.dto.CreateTacheDTO;
import com.gestionstages.dto.ProgressionDTO;
import com.gestionstages.dto.TacheResponseDTO;
import com.gestionstages.dto.UpdateTacheDTO;
import com.gestionstages.model.Utilisateur;
import com.gestionstages.service.FileStorageService;
import com.gestionstages.service.TacheService;
import jakarta.validation.Valid;
import com.gestionstages.model.Tache;
import com.gestionstages.repository.TacheRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.gestionstages.model.Candidature;
import com.gestionstages.repository.StageRepository;
import com.gestionstages.service.JournalPdfService;

@RestController
@RequestMapping("/api/stages/{stageId}/journal")
public class TacheController {

    private final TacheService tacheService;
    private final FileStorageService fileStorageService;
    private final TacheRepository tacheRepository;
    private final StageRepository stageRepository;
    private final JournalPdfService journalPdfService;
    private static final Logger logger = LoggerFactory.getLogger(TacheController.class);

    public TacheController(
            TacheService tacheService,
            FileStorageService fileStorageService,
            TacheRepository tacheRepository,
            StageRepository stageRepository,
            JournalPdfService journalPdfService
    ) {
        this.tacheService = tacheService;
        this.fileStorageService = fileStorageService;
        this.tacheRepository = tacheRepository;
        this.stageRepository = stageRepository;
        this.journalPdfService = journalPdfService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<?> creerTache(
            @PathVariable Long stageId,
            @Valid @RequestBody CreateTacheDTO dto,
            @AuthenticationPrincipal Utilisateur currentUser
    ) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Utilisateur non authentifié"));
            }
            TacheResponseDTO response = tacheService.creerTache(stageId, dto, currentUser.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{tacheId}")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<?> modifierTache(
            @PathVariable Long stageId,
            @PathVariable Long tacheId,
            @Valid @RequestBody UpdateTacheDTO dto,
            @AuthenticationPrincipal Utilisateur currentUser
    ) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Utilisateur non authentifié"));
            }
            TacheResponseDTO response = tacheService.modifierTache(tacheId, dto, currentUser.getId());
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENTREPRISE', 'CHEF_DEPT_STAGE')")
    public ResponseEntity<?> listerTaches(
            @PathVariable Long stageId,
            @AuthenticationPrincipal Utilisateur currentUser
    ) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Utilisateur non authentifié"));
            }
            String role = currentUser.getRole() != null ? currentUser.getRole().name() : "";
            List<TacheResponseDTO> taches = tacheService.listerTachesParStage(stageId, currentUser.getId(), role);
            return ResponseEntity.ok(taches);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/progression")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENTREPRISE', 'CHEF_DEPT_STAGE')")
    public ResponseEntity<?> calculerProgression(
            @PathVariable Long stageId,
            @AuthenticationPrincipal Utilisateur currentUser
    ) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Utilisateur non authentifié"));
            }
            String role = currentUser.getRole() != null ? currentUser.getRole().name() : "";
            tacheService.listerTachesParStage(stageId, currentUser.getId(), role);
            ProgressionDTO progression = tacheService.calculerProgression(stageId);
            return ResponseEntity.ok(progression);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('ETUDIANT')")
    public ResponseEntity<?> uploadPieceJointe(
            @PathVariable Long stageId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Utilisateur currentUser
    ) {
        try {
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Utilisateur non authentifié"));
            }
            tacheService.listerTachesParStage(stageId, currentUser.getId(), "ETUDIANT");
            String fileDownloadUri = fileStorageService.storeFile(file, stageId);
            return ResponseEntity.ok(Map.of("chemin", fileDownloadUri));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Erreur lors de l'upload du fichier"));
        }
    }

    @GetMapping("/{tacheId}/piece-jointe")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENTREPRISE', 'CHEF_DEPT_STAGE')")
    public ResponseEntity<Resource> telechargerPieceJointe(
            @PathVariable Long stageId,
            @PathVariable Long tacheId,
            @AuthenticationPrincipal Utilisateur currentUser
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String role = currentUser.getRole() != null ? currentUser.getRole().name() : "";
        tacheService.listerTachesParStage(stageId, currentUser.getId(), role);

        Tache tache = tacheRepository.findById(tacheId)
                .orElseThrow(() -> new IllegalArgumentException("Tâche introuvable"));

        if (tache.getPieceJointe() == null || tache.getPieceJointe().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = fileStorageService.loadFileAsResource(tache.getPieceJointe());
        String contentType = "application/octet-stream";
        String pathStr = tache.getPieceJointe().toLowerCase();
        if (pathStr.endsWith(".pdf")) {
            contentType = "application/pdf";
        } else if (pathStr.endsWith(".png")) {
            contentType = "image/png";
        } else if (pathStr.endsWith(".jpg") || pathStr.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        }

        ContentDisposition disposition = ContentDisposition.inline()
                .filename("piece-jointe-" + tacheId, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(resource);
    }

    @GetMapping("/pdf")
    @PreAuthorize("hasAnyRole('ETUDIANT', 'ENTREPRISE', 'CHEF_DEPT_STAGE')")
    public ResponseEntity<?> telechargerJournalPdf(
            @PathVariable Long stageId,
            @AuthenticationPrincipal Utilisateur currentUser
    ) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", "Utilisateur non authentifié"));
        }
        try {
            String role = currentUser.getRole() != null ? currentUser.getRole().name() : "";

            // Use optimized query with JOIN FETCH to avoid LazyInitializationException
            Candidature stage = stageRepository.findByIdWithDetails(stageId)
                    .orElseThrow(() -> new IllegalArgumentException("Stage introuvable"));

            List<Tache> taches = tacheRepository.findByStageIdOrderByDateAsc(stageId)
        .stream()
        .filter(t -> t.getDescription() != null && !t.getDescription().isBlank())
        .collect(java.util.stream.Collectors.toList());
            ProgressionDTO progression = tacheService.calculerProgression(stageId);

            logger.info("Generating journal PDF for stageId {} with {} tasks, progression {}%", stageId, taches.size(), progression.getPourcentage());
            byte[] pdf = journalPdfService.genererPdfJournal(stage, taches, progression.getPourcentage());

            ContentDisposition disposition = ContentDisposition.attachment()
                    .filename("journal-de-stage-" + stageId + ".pdf", StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .body(pdf);
        } catch (SecurityException e) {
            logger.warn("Access forbidden for journal PDF generation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid argument for journal PDF generation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error generating journal PDF for stageId {}", stageId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", "Impossible de générer le PDF : " + e.getMessage()));
        }
    }
}
