package com.gestionstages.exception;

public class InvalidEntrepriseStatusException extends RuntimeException {
    public InvalidEntrepriseStatusException(String statut) {
        super("Statut d'entreprise invalide : " + statut);
    }
}
