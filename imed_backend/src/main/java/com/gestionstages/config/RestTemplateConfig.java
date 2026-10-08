package com.gestionstages.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration de RestTemplate pour les appels HTTP aux services externes.
 *
 * Notamment utilisé par RecommandationClientService pour communiquer avec
 * le microservice Python de recommandation.
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Crée un bean RestTemplate pour les appels HTTP.
     *
     * @return RestTemplate configuré et prêt à l'emploi
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
