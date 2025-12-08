package cl.sicc.siccapi.security.service;

import cl.sicc.siccapi.user.domain.Role;
import cl.sicc.siccapi.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = "security.jwt.secret-key=c3lzdGVtLWNsL3NpY2Mvc2ljYS1hcGktand0LWtleS0yMDI1LWp3dC1zZWNyZXQta2V5LWZvcm1hdGVkLWluLWJhc2U2NA==")
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        userDetails = User.builder()
                .id(1L)
                .firstname("Juan")
                .lastname("Pérez")
                .email("juan@example.com")
                .password("encoded_password")
                .role(Role.USER)
                .enabled(true)
                .build();
    }

    @Test
    void testGenerateTokenSuccess() {
        String token = jwtService.generateToken(userDetails);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testExtractUsername() {
        String token = jwtService.generateToken(userDetails);
        String username = jwtService.extractUsername(token);
        assertEquals("juan@example.com", username);
    }

    @Test
    void testIsTokenValidSuccess() {
        String token = jwtService.generateToken(userDetails);
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void testIsTokenValidInvalidUser() {
        String token = jwtService.generateToken(userDetails);
        UserDetails differentUser = User.builder()
                .id(2L)
                .firstname("Pedro")
                .lastname("García")
                .email("pedro@example.com")
                .password("encoded_password")
                .role(Role.USER)
                .enabled(true)
                .build();
        assertFalse(jwtService.isTokenValid(token, differentUser));
    }
}

