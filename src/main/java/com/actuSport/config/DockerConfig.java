package com.actuSport.config;

import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("docker")
public class DockerConfig {
    
    static {
        // Désactiver complètement Spring Security pour le profil docker
        System.setProperty("spring.security.enabled", "false");
    }
}
