package com.gestionstages.exception;

public class EntrepriseEmailAlreadyUsedException extends RuntimeException {
    public EntrepriseEmailAlreadyUsedException() {
        super("Une entreprise utilise déjà cet email de contact");
    }
}
