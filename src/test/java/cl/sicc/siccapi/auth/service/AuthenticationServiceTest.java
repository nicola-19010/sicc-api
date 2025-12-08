package cl.sicc.siccapi.auth.service;

import cl.sicc.siccapi.auth.dto.AuthenticationResponse;
import cl.sicc.siccapi.auth.dto.LoginRequest;
import cl.sicc.siccapi.auth.dto.RegisterRequest;
import cl.sicc.siccapi.user.domain.Role;
import cl.sicc.siccapi.user.domain.User;
import cl.sicc.siccapi.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "security.jwt.secret-key=c3lzdGVtLWNsL3NpY2Mvc2ljYS1hcGktand0LWtleS0yMDI1LWp3dC1zZWNyZXQta2V5LWZvcm1hdGVkLWluLWJhc2U2NA==")
class AuthenticationServiceTest {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        response = new MockHttpServletResponse();
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest request = RegisterRequest.builder()
                .firstname("Juan")
                .lastname("Pérez")
                .email("juan@example.com")
                .password("password123")
                .build();

        AuthenticationResponse authResponse = authenticationService.register(request, response);

        assertNotNull(authResponse);
        assertEquals("juan@example.com", authResponse.getEmail());
        assertEquals("Juan", authResponse.getFirstname());
        assertEquals("Pérez", authResponse.getLastname());
        assertTrue(userRepository.existsByEmail("juan@example.com"));
    }

    @Test
    void testRegisterDuplicateEmail() {
        User user = User.builder()
                .firstname("Juan")
                .lastname("Pérez")
                .email("juan@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .enabled(true)
                .build();
        userRepository.save(user);

        RegisterRequest request = RegisterRequest.builder()
                .firstname("Juan")
                .lastname("Pérez")
                .email("juan@example.com")
                .password("password123")
                .build();

        assertThrows(RuntimeException.class, () -> authenticationService.register(request, response));
    }

    @Test
    void testLoginSuccess() {
        // Create user
        User user = User.builder()
                .firstname("Juan")
                .lastname("Pérez")
                .email("juan@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .enabled(true)
                .build();
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .email("juan@example.com")
                .password("password123")
                .build();

        AuthenticationResponse authResponse = authenticationService.login(request, response);

        assertNotNull(authResponse);
        assertEquals("juan@example.com", authResponse.getEmail());
    }

    @Test
    void testLoginInvalidCredentials() {
        User user = User.builder()
                .firstname("Juan")
                .lastname("Pérez")
                .email("juan@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .enabled(true)
                .build();
        userRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .email("juan@example.com")
                .password("wrongpassword")
                .build();

        assertThrows(Exception.class, () -> authenticationService.login(request, response));
    }
}

