package com.gestionstages.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MySQL refuse de supprimer l'index unique {@code uk_notification_candidature}
 * tant que la clé étrangère sur {@code candidature_id} s'appuie dessus.
 * On retire d'abord la FK, on supprime l'unique, puis on recrée un index
 * non unique et la FK — ce qui autorise plusieurs notifications par stage.
 */
@Component
@Order(0)
public class NotificationSchemaMigration implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(NotificationSchemaMigration.class);
    private static final String TABLE = "notifications_etudiant";

    static final String UNIQUE_INDEXES_SQL = """
            SELECT DISTINCT INDEX_NAME
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND LOWER(TABLE_NAME) = 'notifications_etudiant'
              AND LOWER(COLUMN_NAME) = 'candidature_id'
              AND NON_UNIQUE = 0
              AND INDEX_NAME <> 'PRIMARY'
            """;

    static final String FOREIGN_KEYS_SQL = """
            SELECT DISTINCT CONSTRAINT_NAME
            FROM information_schema.KEY_COLUMN_USAGE
            WHERE TABLE_SCHEMA = DATABASE()
              AND LOWER(TABLE_NAME) = 'notifications_etudiant'
              AND LOWER(COLUMN_NAME) = 'candidature_id'
              AND REFERENCED_TABLE_NAME IS NOT NULL
            """;

    static final String REMAINING_INDEXES_SQL = """
            SELECT COUNT(*)
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND LOWER(TABLE_NAME) = 'notifications_etudiant'
              AND LOWER(COLUMN_NAME) = 'candidature_id'
              AND INDEX_NAME <> 'PRIMARY'
            """;

    private final JdbcTemplate jdbcTemplate;

    public NotificationSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            migrate();
        } catch (DataAccessException | IllegalStateException exception) {
            log.warn(
                    "Migration des notifications ignorée : {}",
                    exception instanceof DataAccessException dataAccess
                            ? dataAccess.getMostSpecificCause().getMessage()
                            : exception.getMessage()
            );
        }
    }

    void migrate() {
        List<String> uniqueIndexes = jdbcTemplate.queryForList(UNIQUE_INDEXES_SQL, String.class);
        if (uniqueIndexes.isEmpty()) {
            log.debug("Aucun index unique obsolète sur candidature_id.");
            return;
        }

        List<String> foreignKeys = jdbcTemplate.queryForList(FOREIGN_KEYS_SQL, String.class);
        for (String foreignKey : foreignKeys) {
            jdbcTemplate.execute("ALTER TABLE " + TABLE + " DROP FOREIGN KEY " + quote(foreignKey));
            log.info("Clé étrangère {} retirée le temps de migrer les notifications.", foreignKey);
        }

        for (String uniqueIndex : uniqueIndexes) {
            jdbcTemplate.execute("ALTER TABLE " + TABLE + " DROP INDEX " + quote(uniqueIndex));
            log.info("Index unique obsolète {} supprimé.", uniqueIndex);
        }

        Number remainingIndexes = jdbcTemplate.queryForObject(REMAINING_INDEXES_SQL, Number.class);
        if (remainingIndexes == null || remainingIndexes.intValue() == 0) {
            jdbcTemplate.execute(
                    "ALTER TABLE " + TABLE + " ADD INDEX idx_notification_candidature (candidature_id)"
            );
        }

        jdbcTemplate.execute(
                "ALTER TABLE " + TABLE
                        + " ADD CONSTRAINT fk_notification_candidature "
                        + "FOREIGN KEY (candidature_id) REFERENCES candidatures (id)"
        );
        log.info("Plusieurs notifications par stage sont maintenant autorisées.");
    }

    static String quote(String identifier) {
        if (identifier == null || !identifier.matches("[A-Za-z0-9_]+")) {
            throw new IllegalStateException("Identifiant SQL inattendu : " + identifier);
        }
        return "`" + identifier + "`";
    }
}
