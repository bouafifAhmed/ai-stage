package com.gestionstages.controller;

import com.gestionstages.dto.CreateEtudiantDTO;
import com.gestionstages.dto.EtudiantResponseDTO;
import com.gestionstages.dto.UpdateEtudiantDTO;
import com.gestionstages.service.AdminEtudiantService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/etudiants")
public class AdminEtudiantController {

    private final AdminEtudiantService adminEtudiantService;

    public AdminEtudiantController(AdminEtudiantService adminEtudiantService) {
        this.adminEtudiantService = adminEtudiantService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<EtudiantResponseDTO> creerEtudiant(
            @Valid @RequestBody CreateEtudiantDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminEtudiantService.creerEtudiant(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<EtudiantResponseDTO> modifierEtudiant(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEtudiantDTO dto
    ) {
        return ResponseEntity.ok(adminEtudiantService.modifierEtudiant(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> desactiverEtudiant(@PathVariable Long id) {
        adminEtudiantService.desactiverEtudiant(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activation")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<EtudiantResponseDTO> activerEtudiant(@PathVariable Long id) {
        return ResponseEntity.ok(adminEtudiantService.activerEtudiant(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<EtudiantResponseDTO>> listerEtudiants(
            @RequestParam(required = false) Boolean actif,
            @RequestParam(required = false) String filiere,
            @PageableDefault(size = 10, sort = "nom", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(adminEtudiantService.listerEtudiants(actif, filiere, pageable));
    }
}
