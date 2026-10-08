package com.gestionstages.service;

import com.gestionstages.dto.StageDTOs.CvExtractionResponse;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CvExtractionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CvExtractionService.class);
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?<!\\d)(?:(?:\\+|00)\\s?216[\\s.-]?)?([24579](?:[\\s.-]?\\d){7})(?!\\d)"
    );
    private static final Pattern EXPLICIT_FIELD_PATTERN = Pattern.compile(
            "(?iu)^(?:fili[eè]re|sp[eé]cialit[eé]|domaine)\\s*[:\\-]\\s*(.{2,150})$"
    );
    private static final Pattern STUDY_FIELD_PATTERN = Pattern.compile(
            "(?iu)\\b(?:master|mast[eè]re|licence|bachelor|ing[eé]nieur|doctorat|bts)\\b.*?\\b(?:en|de)\\s+(.+)$"
    );
    private static final List<LevelRule> LEVEL_RULES = List.of(
            new LevelRule("Doctorat", Pattern.compile("(?iu)\\b(?:doctorat|ph\\.?d)\\b")),
            new LevelRule("Master", Pattern.compile("(?iu)\\b(?:master|mast[eè]re|bac\\s*\\+\\s*5)\\b")),
            new LevelRule("Cycle ingénieur", Pattern.compile("(?iu)\\b(?:ing[eé]nieur|cycle\\s+ing[eé]nieur)\\b")),
            new LevelRule("Licence", Pattern.compile("(?iu)\\b(?:licence|bachelor|bac\\s*\\+\\s*3)\\b")),
            new LevelRule("BTS", Pattern.compile("(?iu)\\b(?:bts|bac\\s*\\+\\s*2)\\b")),
            new LevelRule("Baccalauréat", Pattern.compile("(?iu)\\bbaccalaur[eé]at\\b"))
    );
    private static final List<SkillRule> SKILL_RULES = List.of(
            skill("Java", "java"),
            skill("Spring Boot", "spring boot", "springboot"),
            skill("Angular", "angular"),
            skill("TypeScript", "typescript"),
            skill("JavaScript", "javascript"),
            skill("Python", "python"),
            skill("C++", "c++"),
            skill("C#", "c#"),
            skill(".NET", ".net", "dotnet"),
            skill("SQL", "sql"),
            skill("MySQL", "mysql"),
            skill("PostgreSQL", "postgresql", "postgres"),
            skill("MongoDB", "mongodb"),
            skill("Docker", "docker"),
            skill("Git", "git"),
            skill("HTML", "html", "html5"),
            skill("CSS", "css", "css3"),
            skill("React", "react", "reactjs"),
            skill("Vue.js", "vue.js", "vuejs"),
            skill("Node.js", "node.js", "nodejs"),
            skill("PHP", "php"),
            skill("Symfony", "symfony"),
            skill("Laravel", "laravel"),
            skill("Machine Learning", "machine learning"),
            skill("Power BI", "power bi", "powerbi"),
            skill("Excel", "excel"),
            skill("AWS", "aws"),
            skill("Azure", "azure"),
            skill("Linux", "linux"),
            skill("API REST", "api rest", "rest api"),
            skill("Scrum", "scrum")
    );

    public CvExtractionResponse extract(InputStream pdfInput) {
        try {
            return extract(pdfInput.readAllBytes());
        } catch (IOException exception) {
            LOGGER.warn("Impossible de lire le CV pour l'extraction", exception);
            return CvExtractionResponse.empty();
        }
    }

    public CvExtractionResponse extract(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            String text = new PDFTextStripper().getText(document);
            if (text == null || text.isBlank()) {
                return CvExtractionResponse.empty();
            }
            String normalizedText = normalizeWhitespace(text);
            return new CvExtractionResponse(
                    extractPhone(normalizedText),
                    extractStudyField(text),
                    extractStudyLevel(normalizedText),
                    extractSkills(normalizedText),
                    true
            );
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Impossible d'extraire le texte du CV", exception);
            return CvExtractionResponse.empty();
        }
    }

    private String extractPhone(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String localNumber = matcher.group(1).replaceAll("\\D", "");
        String fullMatch = matcher.group().replaceAll("\\s", "");
        return fullMatch.startsWith("+216") || fullMatch.startsWith("00216")
                ? "+216 " + localNumber
                : localNumber;
    }

    private String extractStudyField(String text) {
        for (String rawLine : text.split("\\R")) {
            String line = normalizeWhitespace(rawLine);
            if (line.isBlank()) {
                continue;
            }
            Matcher explicitMatcher = EXPLICIT_FIELD_PATTERN.matcher(line);
            if (explicitMatcher.find()) {
                return cleanStudyField(explicitMatcher.group(1));
            }
            Matcher degreeMatcher = STUDY_FIELD_PATTERN.matcher(line);
            if (degreeMatcher.find()) {
                return cleanStudyField(degreeMatcher.group(1));
            }
        }
        return null;
    }

    private String extractStudyLevel(String text) {
        for (LevelRule rule : LEVEL_RULES) {
            if (rule.pattern().matcher(text).find()) {
                return rule.label();
            }
        }
        return null;
    }

    private List<String> extractSkills(String text) {
        String searchableText = removeAccents(text).toLowerCase(Locale.ROOT);
        Set<String> skills = new LinkedHashSet<>();
        for (SkillRule rule : SKILL_RULES) {
            if (rule.aliases().stream().anyMatch(alias -> containsTerm(searchableText, alias))) {
                skills.add(rule.label());
            }
        }
        return new ArrayList<>(skills);
    }

    private boolean containsTerm(String text, String term) {
        String normalizedTerm = removeAccents(term).toLowerCase(Locale.ROOT);
        Pattern pattern = Pattern.compile(
                "(?<![\\p{L}\\p{N}])" + Pattern.quote(normalizedTerm) + "(?![\\p{L}\\p{N}])"
        );
        return pattern.matcher(text).find();
    }

    private String cleanStudyField(String value) {
        String cleaned = value
                .replaceFirst("(?iu)\\s+(?:à|au|chez)\\s+.*$", "")
                .replaceFirst("\\s*[|•–—]\\s*.*$", "")
                .replaceFirst("\\s+\\b(?:19|20)\\d{2}\\b.*$", "")
                .trim();
        if (cleaned.length() > 150) {
            cleaned = cleaned.substring(0, 150).trim();
        }
        return cleaned.isBlank() ? null : cleaned;
    }

    private String normalizeWhitespace(String value) {
        return value == null ? "" : value.replace('\u00a0', ' ').replaceAll("[\\t ]+", " ").trim();
    }

    private String removeAccents(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private static SkillRule skill(String label, String... aliases) {
        return new SkillRule(label, List.of(aliases));
    }

    private record LevelRule(String label, Pattern pattern) {}
    private record SkillRule(String label, List<String> aliases) {}
}
