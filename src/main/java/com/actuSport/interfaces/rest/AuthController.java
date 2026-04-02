package com.actuSport.interfaces.rest;

import com.actuSport.infrastructure.security.JwtTokenUtil;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenUtil jwtTokenUtil,
                          UserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody Map<String, String> authenticationRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    authenticationRequest.get("username"),
                    authenticationRequest.get("password")
                )
            );

            final UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            final String jwt = jwtTokenUtil.generateToken(userDetails.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("type", "Bearer");
            response.put("username", userDetails.getUsername());
            response.put("authorities", userDetails.getAuthorities());

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication failed", "message", "Invalid username or password"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authorization) {
        try {
            String token = authorization.substring(7);
            String username = jwtTokenUtil.getUsernameFromToken(token);

            if (jwtTokenUtil.validateToken(token, username)) {
                String newToken = jwtTokenUtil.generateToken(username);
                return ResponseEntity.ok(Map.of("token", newToken, "type", "Bearer"));
            }
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));

        } catch (JwtException e) {
            return ResponseEntity.status(401).body(Map.of("error", "Token refresh failed"));
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authorization) {
        try {
            String token = authorization.substring(7);
            boolean isValid = jwtTokenUtil.validateToken(token);

            if (isValid) {
                String username = jwtTokenUtil.getUsernameFromToken(token);
                return ResponseEntity.ok(Map.of("valid", true, "username", username));
            }
            return ResponseEntity.status(401).body(Map.of("valid", false));

        } catch (JwtException e) {
            return ResponseEntity.status(401).body(Map.of("valid", false, "error", "Token validation failed"));
        }
    }
}
