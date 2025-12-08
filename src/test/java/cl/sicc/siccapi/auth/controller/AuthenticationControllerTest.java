package cl.sicc.siccapi.auth.controller;

import cl.sicc.siccapi.auth.dto.LoginRequest;
import cl.sicc.siccapi.auth.dto.RegisterRequest;
import cl.sicc.siccapi.user.domain.Role;
import cl.sicc.siccapi.user.domain.User;
import cl.sicc.siccapi.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "security.jwt.secret-key=c3lzdGVtLWNsL3NpY2Mvc2ljYS1hcGktand0LWtleS0yMDI1LWp3dC1zZWNyZXQta2V5LWZvcm1hdGVkLWluLWJhc2U2NA==")
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstname("Juan")
                .lastname("Pérez")
                .email("juan@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", equalTo("juan@example.com")))
                .andExpect(jsonPath("$.firstname", equalTo("Juan")))
                .andExpect(jsonPath("$.lastname", equalTo("Pérez")));
    }

    @Test
    void testLoginSuccess() throws Exception {
        // Create user first
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

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", equalTo("juan@example.com")));
    }

    @Test
    void testLoginInvalidCredentials() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("password123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }
}

