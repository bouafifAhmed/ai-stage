package com.gestionstages.exception;

public class RoleNotAllowedException extends RuntimeException {
    public RoleNotAllowedException() {
        super("Ce rôle ne peut pas être attribué par l'inscription publique");
    }
}
