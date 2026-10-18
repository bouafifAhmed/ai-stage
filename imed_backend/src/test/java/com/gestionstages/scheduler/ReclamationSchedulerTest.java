package com.gestionstages.scheduler;

import com.gestionstages.service.ReclamationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class ReclamationSchedulerTest {

    @Test
    void leJobLanceLaClotureAutomatique() {
        ReclamationService service = mock(ReclamationService.class);

        new ReclamationScheduler(service).clotureAutomatiqueReclamations();

        verify(service).clotureAutomatique();
    }

    @Test
    void uneErreurDuJobNeRemontePasAuPlanificateur() {
        ReclamationService service = mock(ReclamationService.class);
        doThrow(new IllegalStateException("base indisponible")).when(service).clotureAutomatique();

        assertDoesNotThrow(() -> new ReclamationScheduler(service).clotureAutomatiqueReclamations());
    }
}
