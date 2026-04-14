package com.actuSport.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Profile({"docker", "dev", "demo"})
public class DevSecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.securityContext(org.springframework.security.config.annotation.web.configurers.SecurityContextConfigurer::disable)
            .sessionManagement(org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer::disable)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/sports/**").permitAll()
                .requestMatchers("/api/matches/**").permitAll()
                .requestMatchers("/api/news/**").permitAll()
                .requestMatchers("/api/teams/**").permitAll()
                .requestMatchers("/api/competitions/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                                .requestMatchers("/static/**", "/public/**", "/resources/**", "/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().authenticated()
            )
            .cors(cors -> cors.disable())
            .csrf(csrf -> csrf.disable());
        
        return http.build();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
