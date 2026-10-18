package com.gestionstages.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

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
     * Les délais de connexion et de lecture viennent de recommandation.service.timeout-seconds
     * (10 s par défaut) : sans eux, un service Python bloqué bloquerait aussi la requête de l'étudiant.
     *
     * @return RestTemplate configuré et prêt à l'emploi
     */
    @Bean
    public RestTemplate restTemplate(@Value("${recommandation.service.timeout-seconds:10}") long timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        return new RestTemplate(factory);
    }
}
