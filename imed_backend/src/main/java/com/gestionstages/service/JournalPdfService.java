package com.gestionstages.service;

import com.gestionstages.exception.StageBusinessException;
import com.gestionstages.model.Candidature;
import com.gestionstages.model.Tache;
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
public class JournalPdfService {
    private static final float MARGIN = 50f;
    private static final float LINE = 16f;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH);

    private final SignatureStorageService signatureStorage;

    public JournalPdfService(SignatureStorageService signatureStorage) {
        this.signatureStorage = signatureStorage;
    }

    public byte[] genererPdfJournal(Candidature stage, List<Tache> taches, double progressionPourcentage) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float y = page.getMediaBox().getHeight() - MARGIN;

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                y = writeLine(content, titleFont, 18, MARGIN, y, "JOURNAL DE STAGE");
                y -= 5;
                
                // Safe access to offre and its properties
                String stageTitre = "N/A";
                String entrepriseNom = "N/A";
                try {
                    if (stage.getOffre() != null) {
                        stageTitre = stage.getOffre().getTitre() != null ? stage.getOffre().getTitre() : "N/A";
                        if (stage.getOffre().getEntreprise() != null) {
                            entrepriseNom = stage.getOffre().getEntreprise().getNom() != null ? stage.getOffre().getEntreprise().getNom() : "N/A";
                        }
                    }
                } catch (Exception e) {
                    // Lazy loading exception - keep default values
                }
                
                y = writeLine(content, bodyFont, 11, MARGIN, y, "Stage : " + stageTitre);
                y = writeLine(content, bodyFont, 11, MARGIN, y, "Entreprise : " + entrepriseNom);
                
                // Safe access to etudiant
                String etudiantNom = "N/A";
                try {
                    if (stage.getEtudiant() != null) {
                        String prenom = stage.getEtudiant().getPrenom() != null ? stage.getEtudiant().getPrenom() : "";
                        String nom = stage.getEtudiant().getNom() != null ? stage.getEtudiant().getNom() : "";
                        etudiantNom = (prenom + " " + nom).trim();
                        if (etudiantNom.isEmpty()) etudiantNom = "N/A";
                    }
                } catch (Exception e) {
                    // Lazy loading exception - keep default value
                }
                y = writeLine(content, bodyFont, 11, MARGIN, y, "Etudiant : " + etudiantNom);
                
                // Safe access to encadrant
                try {
                    if (stage.getEncadrant() != null) {
                        String prenomEnc = stage.getEncadrant().getPrenom() != null ? stage.getEncadrant().getPrenom() : "";
                        String nomEnc = stage.getEncadrant().getNom() != null ? stage.getEncadrant().getNom() : "";
                        String encadrantNom = (prenomEnc + " " + nomEnc).trim();
                        if (!encadrantNom.isEmpty()) {
                            y = writeLine(content, bodyFont, 11, MARGIN, y, "Encadrant : " + encadrantNom);
                        }
                    }
                } catch (Exception e) {
                    // Lazy loading exception - skip encadrant line
                }
                
                y = writeLine(content, bodyFont, 11, MARGIN, y,
                        String.format(Locale.FRENCH, "Progression globale : %.1f%%", progressionPourcentage));
                y -= LINE;

                y = writeLine(content, titleFont, 14, MARGIN, y,
                        "Entrees du Journal (" + taches.size() + " tache(s))");
                y -= 5;

                for (Tache tache : taches) {
                    if (y < 150) {
                        break;
                    }
                    String dateStr = tache.getDate() != null ? tache.getDate().format(DATE_FORMATTER) : "-";
                    y = writeLine(content, titleFont, 11, MARGIN, y,
                            "[" + dateStr + "] " + tache.getTitre());
                    if (tache.getDescription() != null && !tache.getDescription().isBlank()) {
                        y = writeLine(content, bodyFont, 10, MARGIN + 15, y, tache.getDescription());
                    }
                    if (tache.getCommentaireEncadrant() != null && !tache.getCommentaireEncadrant().isBlank()) {
                        y = writeLine(content, bodyFont, 9, MARGIN + 15, y,
                                "Avis encadrant : " + tache.getCommentaireEncadrant());
                    }
                    y -= 6;
                }

                y = Math.min(y - 15, 140);
                writeLine(content, titleFont, 12, MARGIN, y, "Validation & Signatures");
                y -= 10;

                drawSignatureBlock(content, document, bodyFont, MARGIN, y,
                        "Signature Etudiant", stage.getSignatureEtudiantPath(), stage.getDateSignatureEtudiant());
                drawSignatureBlock(content, document, bodyFont, MARGIN + 260, y,
                        "Signature Entreprise", stage.getSignatureEncadrantPath(), stage.getDateSignatureEncadrant());
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new StageBusinessException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible de generer le PDF : " + exception.getMessage()
            );
        }
    }

    private void drawSignatureBlock(
            PDPageContentStream content,
            PDDocument document,
            PDType1Font font,
            float x,
            float y,
            String label,
            String storageKey,
            LocalDateTime dateSignature
    ) throws IOException {
        writeLine(content, font, 10, x, y, label);
        if (storageKey != null && !storageKey.isBlank()) {
            try {
                byte[] png = signatureStorage.load(storageKey);
                PDImageXObject image = PDImageXObject.createFromByteArray(document, png, label);
                content.drawImage(image, x, y - 55, 160, 45);
                if (dateSignature != null) {
                    writeLine(content, font, 9, x, y - 68, "Signe le " + dateSignature.format(DATE_TIME_FORMATTER));
                }
            } catch (Exception ignored) {
                writeLine(content, font, 9, x, y - 20, "(Signature enregistree)");
            }
        } else {
            writeLine(content, font, 9, x, y - 20, "Non signe");
        }
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
        content.showText(truncate(normalizeText(text), 90));
        content.endText();
        return y - LINE;
    }

    /**
     * Supprime les accents et caracteres speciaux incompatibles avec PDType1Font (Helvetica/WinAnsiEncoding).
     * La decomposition NFD separe les lettres de leurs diacritiques, puis on supprime ces diacritiques.
     */
    private String normalizeText(String text) {
        if (text == null) return "";
        // Decompose: e.g. "é" -> "e" + combining accent
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD);
        // Keep only printable ASCII characters (0x20 to 0x7E)
        return decomposed.replaceAll("[^\\x20-\\x7E]", "");
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        String value = text.replace('\n', ' ').replace('\r', ' ');
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }
}
