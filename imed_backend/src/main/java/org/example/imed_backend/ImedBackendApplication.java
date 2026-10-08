package org.example.imed_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"org.example.imed_backend", "com.gestionstages"})
@EntityScan(basePackages = "com.gestionstages.model")
@EnableScheduling
public class ImedBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImedBackendApplication.class, args);
    }

}
