package com.gestionstages.dto;

public record AuthResponseDTO(
        String token,
        String type,
        UserResponseDTO user
) {
    public AuthResponseDTO(String token, UserResponseDTO user) {
        this(token, "Bearer", user);
    }
}
