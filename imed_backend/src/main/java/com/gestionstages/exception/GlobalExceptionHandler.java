package com.gestionstages.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyUsed(EmailAlreadyUsedException exception) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), null);
    }

    @ExceptionHandler(EntrepriseEmailAlreadyUsedException.class)
    public ResponseEntity<ApiError> handleEntrepriseEmailAlreadyUsed(
            EntrepriseEmailAlreadyUsedException exception
    ) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), null);
    }

    @ExceptionHandler(EntrepriseNotFoundException.class)
    public ResponseEntity<ApiError> handleEntrepriseNotFound(
            EntrepriseNotFoundException exception
    ) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    @ExceptionHandler(InvalidEntrepriseStatusException.class)
    public ResponseEntity<ApiError> handleInvalidEntrepriseStatus(
            InvalidEntrepriseStatusException exception
    ) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), null);
    }

    @ExceptionHandler(EtudiantNotFoundException.class)
    public ResponseEntity<ApiError> handleEtudiantNotFound(
            EtudiantNotFoundException exception
    ) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException exception) {
        return error(HttpStatus.UNAUTHORIZED, exception.getMessage(), null);
    }

    @ExceptionHandler(AccountPendingApprovalException.class)
    public ResponseEntity<ApiError> handleAccountPendingApproval(
            AccountPendingApprovalException exception
    ) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage(), null);
    }

    @ExceptionHandler(RoleNotAllowedException.class)
    public ResponseEntity<ApiError> handleRoleNotAllowed(RoleNotAllowedException exception) {
        return error(HttpStatus.FORBIDDEN, exception.getMessage(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied() {
        return error(HttpStatus.FORBIDDEN, "Accès refusé", null);
    }

    @ExceptionHandler(StageBusinessException.class)
    public ResponseEntity<ApiError> handleStageBusiness(StageBusinessException exception) {
        return error(exception.getStatus(), exception.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(fieldError ->
                        fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "Les données envoyées sont invalides", fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                fields.putIfAbsent(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ));
        return error(HttpStatus.BAD_REQUEST, "Les données envoyées sont invalides", fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableRequest() {
        return error(HttpStatus.BAD_REQUEST, "Le corps de la requête est invalide", null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        log.warn("Violation d'intégrité des données", exception);
        String detail = exception.getMostSpecificCause().getMessage();
        String message = "Une contrainte de données empêche cette opération.";
        if (detail != null && detail.contains("uk_notification_candidature")) {
            message = "Impossible de notifier l'étudiant : la base n'a pas encore retiré l'ancienne contrainte unique.";
        }
        return error(HttpStatus.CONFLICT, message, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSize() {
        return error(HttpStatus.CONTENT_TOO_LARGE, "Le fichier ne doit pas dépasser 20 Mo", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneralException(Exception exception) {
        log.error("Erreur serveur non geree [{}]: {}", exception.getClass().getSimpleName(), exception.getMessage(), exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne : " + exception.getMessage(), null);
    }

    private ResponseEntity<ApiError> error(
            HttpStatus status,
            String message,
            Map<String, String> fieldErrors
    ) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), message, fieldErrors));
    }

    public record ApiError(
            Instant timestamp,
            int status,
            String message,
            Map<String, String> fieldErrors
    ) {
    }
}
