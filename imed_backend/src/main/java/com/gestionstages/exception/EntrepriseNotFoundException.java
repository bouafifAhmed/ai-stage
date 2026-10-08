package com.gestionstages.exception;

public class EntrepriseNotFoundException extends RuntimeException {
    public EntrepriseNotFoundException(Long id) {
        super("Entreprise introuvable avec l'identifiant " + id);
    }
}
