package com.actuSport.infrastructure.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Pour l'instant, on accepte n'importe quel utilisateur avec le mot de passe "password"
        // À remplacer plus tard par une recherche en base de données (ex: userRepository.findByUsername)
        if ("admin".equals(username) || "user".equals(username)) {
            return new User(username, "$2a$10$8z19.8z19.8z19.8z19.8z19.8z19.8z19.8z19.8z19.8z19.8z19.", // password encodé en BCrypt
                    Collections.emptyList());
        }
        
        throw new UsernameNotFoundException("User not found with username: " + username);
    }
}
