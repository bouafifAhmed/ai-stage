package com.gestionstages.exception;

import org.springframework.http.HttpStatus;

public class StageBusinessException extends RuntimeException {
    private final HttpStatus status;

    public StageBusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() { return status; }
}
