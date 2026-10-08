package com.gestionstages.service;

import com.gestionstages.dto.StageDTOs.CvExtractionResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CvExtractionServiceTest {
    private final CvExtractionService service = new CvExtractionService();

    @Test
    void extraitLesInformationsPrincipalesDunCvTexte() throws Exception {
        byte[] pdf = createPdf(List.of(
                "Lina Doe",
                "+216 20 123 456",
                "Filiere: Genie logiciel",
                "Master en Genie logiciel",
                "Competences: Java, Spring Boot, Angular, TypeScript, MySQL, Docker"
        ));

        CvExtractionResponse result = service.extract(pdf);

        assertTrue(result.texteDetecte());
        assertEquals("+216 20123456", result.telephone());
        assertEquals("Genie logiciel", result.filiere());
        assertEquals("Master", result.niveauEtudes());
        assertTrue(result.competences().containsAll(
                List.of("Java", "Spring Boot", "Angular", "TypeScript", "MySQL", "Docker")
        ));
    }

    @Test
    void retourneDesChampsVidesQuandAucuneInformationNestReconnue() throws Exception {
        CvExtractionResponse result = service.extract(
                createPdf(List.of("Curriculum Vitae", "Experiences professionnelles"))
        );

        assertTrue(result.texteDetecte());
        assertNull(result.telephone());
        assertNull(result.filiere());
        assertNull(result.niveauEtudes());
        assertTrue(result.competences().isEmpty());
    }

    @Test
    void signaleUnPdfSansTexte() throws Exception {
        CvExtractionResponse result = service.extract(createPdf(List.of()));

        assertFalse(result.texteDetecte());
        assertTrue(result.competences().isEmpty());
    }

    private byte[] createPdf(List<String> lines) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            if (!lines.isEmpty()) {
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    content.setLeading(16);
                    content.newLineAtOffset(50, 750);
                    for (String line : lines) {
                        content.showText(line);
                        content.newLine();
                    }
                    content.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
