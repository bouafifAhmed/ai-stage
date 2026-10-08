package com.gestionstages.controller;

import com.gestionstages.dto.CreateEntrepriseDTO;
import com.gestionstages.dto.EntrepriseResponseDTO;
import com.gestionstages.dto.UpdateEntrepriseDTO;
import com.gestionstages.service.EntrepriseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/admin/entreprises")
public class AdminEntrepriseController {

    private final EntrepriseService entrepriseService;

    public AdminEntrepriseController(EntrepriseService entrepriseService) {
        this.entrepriseService = entrepriseService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<EntrepriseResponseDTO> creerEntreprise(
            @Valid @RequestBody CreateEntrepriseDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(entrepriseService.creerEntrepriseParAdmin(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<EntrepriseResponseDTO> modifierEntreprise(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEntrepriseDTO dto
    ) {
        return ResponseEntity.ok(entrepriseService.modifierEntreprise(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> desactiverEntreprise(@PathVariable Long id) {
        entrepriseService.desactiverEntreprise(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/validation")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<EntrepriseResponseDTO> validerEntreprise(@PathVariable Long id) {
        return ResponseEntity.ok(entrepriseService.validerEntreprise(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Page<EntrepriseResponseDTO>> listerEntreprises(
            @RequestParam(required = false) String statut,
            @PageableDefault(size = 10, sort = "nom") Pageable pageable
    ) {
        return ResponseEntity.ok(entrepriseService.listerEntreprises(statut, pageable));
    }
}
