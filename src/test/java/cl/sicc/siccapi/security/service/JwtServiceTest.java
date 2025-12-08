package cl.sicc.siccapi.security.service;

import cl.sicc.siccapi.user.domain.Role;
import cl.sicc.siccapi.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = "security.jwt.secret-key=c3lzdGVtLWNsL3NpY2Mvc2ljYS1hcGktand0LWtleS0yMDI1LWp3dC1zZWNyZXQta2V5LWZvcm1hdGVkLWluLWJhc2U2NA==")
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void testGenerateAccessToken() {
        User user = User.builder()
                .id(1L)
                .firstname("Juan")
                .lastname("Pérez")
                .email("juan@example.com")
                .password("password123")
                .role(Role.USER)
                .enabled(true)
                .build();

        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertTrue(token.contains("."));
        assertEquals("juan@example.com", jwtService.extractUsername(token));
    }

    @Test
    void testGenerateRefreshToken() {
        User user = User.builder()
                .id(1L)
                .firstname("Juan")
                .lastname("Pérez")
                .email("juan@example.com")
                .password("password123")
                .role(Role.USER)
                .enabled(true)
                .build();

        String token = jwtService.generateRefreshToken(user);

        assertNotNull(token);
        assertTrue(token.contains("."));
        assertEquals("juan@example.com", jwtService.extractUsername(token));
    }

    @Test
    void testIsTokenValid() {
        User user = User.builder()
                .id(1L)
                .firstname("Juan")
                .lastname("Pérez")
                .email("juan@example.com")
                .password("password123")
                .role(Role.USER)
                .enabled(true)
                .build();

        String token = jwtService.generateAccessToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void testIsTokenInvalid() {
        User user = User.builder()
                .id(1L)
                .firstname("Juan")
                .lastname("Pérez")
                .email("juan@example.com")
                .password("password123")
                .role(Role.USER)
                .enabled(true)
                .build();

        User otherUser = User.builder()
                .id(2L)
                .firstname("Maria")
                .lastname("García")
                .email("maria@example.com")
                .password("password123")
                .role(Role.USER)
                .enabled(true)
                .build();

        String token = jwtService.generateAccessToken(user);

        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    @Test
    void testExtractUsername() {
        User user = User.builder()
                .id(1L)
                .firstname("Juan")
                .lastname("Pérez")
                .email("juan@example.com")
                .password("password123")
                .role(Role.USER)
                .enabled(true)
                .build();

        String token = jwtService.generateAccessToken(user);
        String username = jwtService.extractUsername(token);

        assertEquals("juan@example.com", username);
    }

    @Test
    void testExtractExpiration() {
        User user = User.builder()
                .id(1L)
                .firstname("Juan")
                .lastname("Pérez")
                .email("juan@example.com")
                .password("password123")
                .role(Role.USER)
                .enabled(true)
                .build();

        String token = jwtService.generateAccessToken(user);
        var expiration = jwtService.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.getTime() > System.currentTimeMillis());
    }

    @Test
    void testAccessTokenExpiresBeforeRefreshToken() {
        User user = User.builder()
                .id(1L)
                .firstname("Juan")
                .lastname("Pérez")
                .email("juan@example.com")
                .password("password123")
                .role(Role.USER)
                .enabled(true)
                .build();

        long accessExpiration = jwtService.getAccessTokenExpiration();
        long refreshExpiration = jwtService.getRefreshTokenExpiration();

        assertTrue(accessExpiration < refreshExpiration,
                "Access token debe expirar antes que refresh token");
    }
}

