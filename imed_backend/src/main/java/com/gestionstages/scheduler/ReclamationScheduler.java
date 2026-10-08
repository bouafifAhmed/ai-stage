package com.gestionstages.scheduler;

import com.gestionstages.service.ReclamationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReclamationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReclamationScheduler.class);

    private final ReclamationService reclamationService;

    public ReclamationScheduler(ReclamationService reclamationService) {
        this.reclamationService = reclamationService;
    }

    /**
     * Exécution quotidienne à 2h du matin pour clôturer automatiquement
     * les réclamations résolues depuis plus de 5 jours
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void clotureAutomatiqueReclamations() {
        logger.info("=== Démarrage du job de clôture automatique des réclamations ===");
        try {
            reclamationService.clotureAutomatique();
            logger.info("=== Job de clôture automatique terminé avec succès ===");
        } catch (Exception e) {
            logger.error("Erreur lors de l'exécution du job de clôture automatique", e);
        }
    }
}
