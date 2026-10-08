package com.gestionstages.exception;

public class AccountPendingApprovalException extends RuntimeException {

    public AccountPendingApprovalException() {
        super("Votre compte entreprise est en attente de validation par le super administrateur");
    }
}
