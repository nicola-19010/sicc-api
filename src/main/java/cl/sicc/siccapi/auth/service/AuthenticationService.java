package cl.sicc.siccapi.auth.service;

import cl.sicc.siccapi.auth.dto.AuthenticationResponse;
import cl.sicc.siccapi.auth.dto.LoginRequest;
import cl.sicc.siccapi.auth.dto.RegisterRequest;
import cl.sicc.siccapi.security.service.JwtService;
import cl.sicc.siccapi.user.domain.Role;
import cl.sicc.siccapi.user.domain.User;
import cl.sicc.siccapi.user.repository.UserRepository;
import cl.sicc.siccapi.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        // Validar que el email no exista
        if (userService.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }

        // Crear nuevo usuario
        User user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        // Generar token
        String token = jwtService.generateToken(savedUser);

        return AuthenticationResponse.builder()
                .token(token)
                .email(savedUser.getEmail())
                .firstname(savedUser.getFirstname())
                .lastname(savedUser.getLastname())
                .build();
    }

    public AuthenticationResponse login(LoginRequest request) {
        // Autenticar usuario
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Obtener usuario
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Generar token
        String token = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .build();
    }
}

