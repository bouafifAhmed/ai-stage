package com.gestionstages.exception;

import com.gestionstages.exception.GlobalExceptionHandler.ApiError;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void lesExceptionsMetierDonnentLeBonCodeHttp() {
        assertStatut(HttpStatus.CONFLICT, handler.handleEmailAlreadyUsed(new EmailAlreadyUsedException()));
        assertStatut(HttpStatus.CONFLICT, handler.handleEntrepriseEmailAlreadyUsed(new EntrepriseEmailAlreadyUsedException()));
        assertStatut(HttpStatus.NOT_FOUND, handler.handleEntrepriseNotFound(new EntrepriseNotFoundException(4L)));
        assertStatut(HttpStatus.BAD_REQUEST, handler.handleInvalidEntrepriseStatus(new InvalidEntrepriseStatusException("X")));
        assertStatut(HttpStatus.NOT_FOUND, handler.handleEtudiantNotFound(new EtudiantNotFoundException(5L)));
        assertStatut(HttpStatus.UNAUTHORIZED, handler.handleInvalidCredentials(new InvalidCredentialsException()));
        assertStatut(HttpStatus.FORBIDDEN, handler.handleAccountPendingApproval(new AccountPendingApprovalException()));
        assertStatut(HttpStatus.FORBIDDEN, handler.handleRoleNotAllowed(new RoleNotAllowedException()));
    }

    @Test
    void lAccesRefuseRenvoieLeMessageAttenduParLeFrontend() {
        ResponseEntity<ApiError> reponse = handler.handleAccessDenied();

        assertStatut(HttpStatus.FORBIDDEN, reponse);
        assertEquals("Accès refusé", reponse.getBody().message());
    }

    @Test
    void uneStageBusinessExceptionGardeSonStatutEtSonMessage() {
        ResponseEntity<ApiError> reponse = handler.handleStageBusiness(
                new StageBusinessException(HttpStatus.CONFLICT, "Plus de places"));

        assertStatut(HttpStatus.CONFLICT, reponse);
        assertEquals("Plus de places", reponse.getBody().message());
        assertNotNull(reponse.getBody().timestamp());
    }

    @Test
    void laValidationRenvoieUneErreurParChampAvecLePremierMessage() throws NoSuchMethodException {
        BeanPropertyBindingResult resultat = new BeanPropertyBindingResult(new Object(), "requete");
        resultat.addError(new org.springframework.validation.FieldError("requete", "email", "L'email est invalide"));
        resultat.addError(new org.springframework.validation.FieldError("requete", "email", "Deuxième message"));
        resultat.addError(new org.springframework.validation.FieldError("requete", "nom", "Le nom est obligatoire"));
        MethodParameter parametre = new MethodParameter(getClass().getDeclaredMethod("methodeFactice", String.class), 0);

        ResponseEntity<ApiError> reponse = handler.handleValidation(new MethodArgumentNotValidException(parametre, resultat));

        assertStatut(HttpStatus.BAD_REQUEST, reponse);
        assertEquals(Map.of("email", "L'email est invalide", "nom", "Le nom est obligatoire"),
                reponse.getBody().fieldErrors());
    }

    @Test
    void lesErreursDeFormatEtDeTailleOntUnMessageLisible() {
        ResponseEntity<ApiError> illisible = handler.handleUnreadableRequest();
        assertStatut(HttpStatus.BAD_REQUEST, illisible);
        assertEquals("Le corps de la requête est invalide", illisible.getBody().message());

        ResponseEntity<ApiError> tropGros = handler.handleMaxUploadSize();
        assertEquals(413, tropGros.getStatusCode().value());
        assertTrue(tropGros.getBody().message().contains("20 Mo"));
    }

    @Test
    void uneViolationDIntegriteNExposePasLeDetailSql() {
        ResponseEntity<ApiError> generique = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("x", new SQLException("Duplicate entry 'a@b.c' for key 'uk_email'")));
        assertStatut(HttpStatus.CONFLICT, generique);
        assertFalse(generique.getBody().message().contains("Duplicate"));

        ResponseEntity<ApiError> notification = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("x", new SQLException("violates uk_notification_candidature")));
        assertTrue(notification.getBody().message().contains("notifier"));
    }

    @Test
    void uneErreurInattendueDonne500() {
        assertStatut(HttpStatus.INTERNAL_SERVER_ERROR, handler.handleGeneralException(new IllegalStateException("boom")));
    }

    @SuppressWarnings("unused")
    private void methodeFactice(String valeur) {
    }

    private void assertStatut(HttpStatus attendu, ResponseEntity<ApiError> reponse) {
        assertEquals(attendu.value(), reponse.getStatusCode().value());
        assertNotNull(reponse.getBody());
        assertEquals(attendu.value(), reponse.getBody().status());
    }
}
