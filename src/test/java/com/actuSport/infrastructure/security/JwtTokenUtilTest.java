package com.actuSport.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenUtilTest {

    @InjectMocks
    private JwtTokenUtil jwtTokenUtil;

    @BeforeEach
    void setUp() {
        jwtTokenUtil = new JwtTokenUtil();
        // Inject values for @Value annotated fields
        // Using a valid Base64 encoded key for HS256 (32 bytes = 256 bits)
        // "bXlTZWNyZXRLZXkxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=" decodes to a 32+ byte key
        ReflectionTestUtils.setField(jwtTokenUtil, "secret", "bXlTZWNyZXRLZXkxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=");
        ReflectionTestUtils.setField(jwtTokenUtil, "expiration", 3600L);
    }

    @Test
    void generateToken_ShouldReturnToken() {
        String token = jwtTokenUtil.generateToken("testUser");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void validateToken_ShouldReturnTrueForValidToken() {
        String token = jwtTokenUtil.generateToken("testUser");
        assertTrue(jwtTokenUtil.validateToken(token, "testUser"));
    }

    @Test
    void validateToken_ShouldReturnFalseForInvalidUser() {
        String token = jwtTokenUtil.generateToken("testUser");
        assertFalse(jwtTokenUtil.validateToken(token, "otherUser"));
    }

    @Test
    void getUsernameFromToken_ShouldReturnCorrectUsername() {
        String token = jwtTokenUtil.generateToken("testUser");
        assertEquals("testUser", jwtTokenUtil.getUsernameFromToken(token));
    }

    @Test
    void isTokenExpired_ShouldReturnFalseForNewToken() {
        String token = jwtTokenUtil.generateToken("testUser");
        assertFalse(jwtTokenUtil.isTokenExpired(token));
    }
}
