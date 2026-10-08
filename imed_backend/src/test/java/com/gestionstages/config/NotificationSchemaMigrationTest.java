package com.gestionstages.config;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationSchemaMigrationTest {

    @Test
    void dropsForeignKeyBeforeObsoleteUniqueIndex() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(NotificationSchemaMigration.UNIQUE_INDEXES_SQL, String.class))
                .thenReturn(List.of("uk_notification_candidature"));
        when(jdbc.queryForList(NotificationSchemaMigration.FOREIGN_KEYS_SQL, String.class))
                .thenReturn(List.of("FKnnt5q3oen5l060pe9n8p9y6fb"));
        when(jdbc.queryForObject(NotificationSchemaMigration.REMAINING_INDEXES_SQL, Number.class))
                .thenReturn(0);

        new NotificationSchemaMigration(jdbc).migrate();

        InOrder order = inOrder(jdbc);
        order.verify(jdbc).execute(
                "ALTER TABLE notifications_etudiant DROP FOREIGN KEY `FKnnt5q3oen5l060pe9n8p9y6fb`"
        );
        order.verify(jdbc).execute(
                "ALTER TABLE notifications_etudiant DROP INDEX `uk_notification_candidature`"
        );
        order.verify(jdbc).execute(
                "ALTER TABLE notifications_etudiant ADD INDEX idx_notification_candidature (candidature_id)"
        );
        order.verify(jdbc).execute(
                "ALTER TABLE notifications_etudiant ADD CONSTRAINT fk_notification_candidature "
                        + "FOREIGN KEY (candidature_id) REFERENCES candidatures (id)"
        );
    }

    @Test
    void doesNothingWhenUniqueIndexIsAlreadyGone() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForList(NotificationSchemaMigration.UNIQUE_INDEXES_SQL, String.class))
                .thenReturn(List.of());

        new NotificationSchemaMigration(jdbc).migrate();

        verify(jdbc, never()).execute(anyString());
        verify(jdbc, never()).queryForList(eq(NotificationSchemaMigration.FOREIGN_KEYS_SQL), eq(String.class));
    }
}
