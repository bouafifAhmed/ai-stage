package com.gestionstages.exception;

public class EtudiantNotFoundException extends RuntimeException {
    public EtudiantNotFoundException(Long id) {
        super("Étudiant introuvable avec l'identifiant " + id);
    }
}
