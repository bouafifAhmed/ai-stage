package com.gestionstages.controller;

import com.gestionstages.dto.AuthResponseDTO;
import com.gestionstages.dto.CreateEntrepriseDTO;
import com.gestionstages.dto.LoginRequestDTO;
import com.gestionstages.dto.RegisterRequestDTO;
import com.gestionstages.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @Valid @RequestBody RegisterRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/register/entreprise")
    public ResponseEntity<AuthResponseDTO> registerEntreprise(
            @Valid @RequestBody CreateEntrepriseDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerEntreprise(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }
}
