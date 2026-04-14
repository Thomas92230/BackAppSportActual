package com.actuSport.infrastructure.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private DataSource dataSource;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Solution temporaire : utilisateurs codés en dur avec hash BCrypt valides
        if ("admin".equals(username)) {
            // Hash BCrypt pour "password"
            return User.builder()
                    .username("admin")
                    .password("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG")
                    .roles("ADMIN")
                    .build();
        }
        
        if ("user".equals(username) || "testuser".equals(username)) {
            // Hash BCrypt pour "password"
            return User.builder()
                    .username(username)
                    .password("$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG")
                    .roles("USER")
                    .build();
        }
        
        // Si la base de données fonctionne, essayer de l'utiliser
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(
                 "SELECT username, password, role FROM users WHERE username = ?")) {
            
            statement.setString(1, username);
            var resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                String dbUsername = resultSet.getString("username");
                String password = resultSet.getString("password");
                String role = resultSet.getString("role");
                
                return User.builder()
                        .username(dbUsername)
                        .password(password)
                        .roles(role)
                        .build();
            }
        } catch (Exception e) {
            // Ignorer les erreurs de base de données pour le moment
        }
        
        throw new UsernameNotFoundException("User not found with username: " + username);
    }
}
