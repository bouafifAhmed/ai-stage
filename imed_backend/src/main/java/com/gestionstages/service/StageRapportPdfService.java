package com.gestionstages.service;

import com.gestionstages.exception.StageBusinessException;
import com.gestionstages.model.Candidature;
import com.gestionstages.model.StatutTacheAssignee;
import com.gestionstages.model.TacheAssignee;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class StageRapportPdfService {
    private static final float MARGIN = 50f;
    private static final float LINE = 16f;
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH);

    private final SignatureStorageService signatureStorage;

    public StageRapportPdfService(SignatureStorageService signatureStorage) {
        this.signatureStorage = signatureStorage;
    }

    public byte[] generer(Candidature stage, List<TacheAssignee> taches) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float y = page.getMediaBox().getHeight() - MARGIN;

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                y = writeLine(content, titleFont, 18, MARGIN, y, "Rapport de stage");
                y -= LINE;
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        "Stage : " + stage.getOffre().getTitre());
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        "Entreprise : " + stage.getOffre().getEntreprise().getNom());
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        "Étudiant : " + nomComplet(stage.getEtudiant().getPrenom(), stage.getEtudiant().getNom()));
                if (stage.getEncadrant() != null) {
                    y = writeLine(content, bodyFont, 11, MARGIN, y,
                            "Encadrant : " + nomComplet(
                                    stage.getEncadrant().getPrenom(),
                                    stage.getEncadrant().getNom()
                            ));
                }
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        "Statut stage : " + stage.getStatutStage().name());
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        "Date candidature : " + formatDateTime(stage.getDateCandidature()));
                y -= LINE;

                long assignees = taches.stream()
                        .filter(t -> t.getStatut() == StatutTacheAssignee.ASSIGNEE)
                        .count();
                long terminees = taches.stream()
                        .filter(t -> t.getStatut() == StatutTacheAssignee.TERMINEE)
                        .count();
                long validees = taches.stream()
                        .filter(t -> t.getStatut() == StatutTacheAssignee.VALIDEE)
                        .count();
                long rejetees = taches.stream()
                        .filter(t -> t.getStatut() == StatutTacheAssignee.REJETEE)
                        .count();

                y = writeLine(content, titleFont, 13, MARGIN, y, "Synthèse des tâches");
                y = writeLine(content, bodyFont, 11, MARGIN, y, "Total : " + taches.size());
                y = writeLine(content, bodyFont, 11, MARGIN, y, "Assignées : " + assignees);
                y = writeLine(content, bodyFont, 11, MARGIN, y, "Terminées : " + terminees);
                y = writeLine(content, bodyFont, 11, MARGIN, y, "Validées : " + validees);
                y = writeLine(content, bodyFont, 11, MARGIN, y, "Rejetées : " + rejetees);
                y -= LINE;

                y = writeLine(content, titleFont, 13, MARGIN, y, "Détail des tâches");
                for (TacheAssignee tache : taches) {
                    if (y < 120) {
                        break;
                    }
                    y = writeLine(content, bodyFont, 10, MARGIN, y,
                            "- " + tache.getTitre()
                                    + " | échéance " + formatDate(tache.getDateEcheance())
                                    + " | " + tache.getStatut().name());
                }

                y -= LINE;
                y = writeLine(content, titleFont, 13, MARGIN, y, "Signatures de fin de stage");
                y = drawSignatureBlock(
                        content,
                        document,
                        bodyFont,
                        MARGIN,
                        y,
                        "Étudiant",
                        stage.getSignatureEtudiantPath(),
                        stage.getDateSignatureEtudiant()
                );
                y = drawSignatureBlock(
                        content,
                        document,
                        bodyFont,
                        MARGIN + 250,
                        y + 80,
                        "Encadrant",
                        stage.getSignatureEncadrantPath(),
                        stage.getDateSignatureEncadrant()
                );

                if (stage.getDateCloture() != null) {
                    y -= 20;
                    writeLine(content, bodyFont, 10, MARGIN, y,
                            "Clôturé le " + formatDateTime(stage.getDateCloture()));
                }
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new StageBusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Le rapport PDF n'a pas pu être généré"
            );
        }
    }

    private float drawSignatureBlock(
            PDPageContentStream content,
            PDDocument document,
            PDType1Font font,
            float x,
            float y,
            String label,
            String storageKey,
            LocalDateTime dateSignature
    ) throws IOException {
        float currentY = y;
        currentY = writeLine(content, font, 10, x, currentY, label);
        if (storageKey != null) {
            byte[] png = signatureStorage.load(storageKey);
            PDImageXObject image = PDImageXObject.createFromByteArray(document, png, label);
            content.drawImage(image, x, currentY - 60, 180, 50);
            currentY -= 70;
            if (dateSignature != null) {
                currentY = writeLine(content, font, 9, x, currentY,
                        "Signé le " + formatDateTime(dateSignature));
            }
        } else {
            currentY = writeLine(content, font, 9, x, currentY, "Non signé");
        }
        return currentY;
    }

    private float writeLine(
            PDPageContentStream content,
            PDType1Font font,
            float size,
            float x,
            float y,
            String text
    ) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(truncate(normalizeText(text), 95));
        content.endText();
        return y - LINE;
    }

    /**
     * Strips accents and non-ASCII chars so PDType1Font (WinAnsiEncoding)
     * can render the text without throwing IllegalArgumentException.
     * e.g. "Étudiant" → "Etudiant", "Synthèse" → "Synthese"
     */
    private String normalizeText(String text) {
        if (text == null) return "";
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        return decomposed.replaceAll("[^\\x20-\\x7E]", "");
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        String value = text.replace('\n', ' ').replace('\r', ' ');
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }

    private String nomComplet(String prenom, String nom) {
        return (prenom + " " + nom).trim();
    }

    private String formatDate(java.time.LocalDate date) {
        return date == null ? "-" : date.format(DATE);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DATE_TIME);
    }
}
