package cl.sicc.siccapi.auth.service;

import cl.sicc.siccapi.auth.dto.AuthenticationResponse;
import cl.sicc.siccapi.auth.dto.LoginRequest;
import cl.sicc.siccapi.auth.dto.RegisterRequest;
import cl.sicc.siccapi.security.service.JwtService;
import cl.sicc.siccapi.user.domain.Role;
import cl.sicc.siccapi.user.domain.User;
import cl.sicc.siccapi.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final Environment environment;

    /**
     * Registrar nuevo usuario
     */
    public AuthenticationResponse register(RegisterRequest request, HttpServletResponse response) {
        // Validar que el email no exista
        if (userRepository.existsByEmail(request.getEmail())) {
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
        log.info("Usuario registrado: {}", savedUser.getEmail());

        // Generar tokens
        String accessToken = jwtService.generateAccessToken(savedUser);
        String refreshToken = jwtService.generateRefreshToken(savedUser);

        // Setear cookies HttpOnly
        setAccessTokenCookie(response, accessToken);
        setRefreshTokenCookie(response, refreshToken);

        return AuthenticationResponse.builder()
                .email(savedUser.getEmail())
                .firstname(savedUser.getFirstname())
                .lastname(savedUser.getLastname())
                .build();
    }

    /**
     * Login de usuario
     */
    public AuthenticationResponse login(LoginRequest request, HttpServletResponse response) {
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

        log.info("Login exitoso: {}", user.getEmail());

        // Generar tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Setear cookies HttpOnly
        setAccessTokenCookie(response, accessToken);
        setRefreshTokenCookie(response, refreshToken);

        return AuthenticationResponse.builder()
                .email(user.getEmail())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .build();
    }

    /**
     * Refresh del access token
     * - Genera NUEVO access token
     * - NO regenera refresh token
     */
    public AuthenticationResponse refresh(String refreshToken, HttpServletResponse response) {
        String userEmail = jwtService.extractUsername(refreshToken);

        if (jwtService.isTokenValid(refreshToken,
                new org.springframework.security.core.userdetails.User(userEmail, "", java.util.List.of()))) {

            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            String newAccessToken = jwtService.generateAccessToken(user);

            // Setear SOLO nuevo access token
            setAccessTokenCookie(response, newAccessToken);

            log.info("Token refrescado para usuario: {}", userEmail);

            return AuthenticationResponse.builder()
                    .email(user.getEmail())
                    .firstname(user.getFirstname())
                    .lastname(user.getLastname())
                    .build();
        }

        throw new RuntimeException("Refresh token inválido o expirado");
    }

    /**
     * Logout de usuario
     * - Limpiar cookies invalidándolas
     */
    public void logout(HttpServletResponse response) {
        clearCookie(response, "access_token", "/");
        clearCookie(response, "refresh_token", "/api/auth/refresh");
        log.info("Logout ejecutado");
    }

    // ============ PRIVADOS ============

    /**
     * Setea la cookie de access token
     * - HttpOnly: true (no accesible a JavaScript)
     * - Secure: true (HTTPS solo)
     * - SameSite: Lax (mejor balance para APIs + frontend)
     * - MaxAge: 15 minutos
     */
    private void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        Cookie accessCookie = new Cookie("access_token", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(isSecureEnvironment());
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) (jwtService.getAccessTokenExpiration() / 1000));
        // Use SameSite=None only in secure environments (production) where cookies are served over HTTPS.
        // In development (non-secure) keep Lax to avoid browsers rejecting cookies without Secure flag.
        accessCookie.setAttribute("SameSite", isSecureEnvironment() ? "None" : "Lax");
        response.addCookie(accessCookie);
    }

    /**
     * Setea la cookie de refresh token
     * - HttpOnly: true (no accesible a JavaScript)
     * - Secure: true (HTTPS solo)
     * - SameSite: Lax
     * - MaxAge: 30 días
     * - Path: /api/auth/refresh (para máxima seguridad)
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(isSecureEnvironment());
        refreshCookie.setPath("/api/auth/refresh");
        refreshCookie.setMaxAge((int) (jwtService.getRefreshTokenExpiration() / 1000));
        // Use SameSite=None only in secure environments (production) where cookies are served over HTTPS.
        refreshCookie.setAttribute("SameSite", isSecureEnvironment() ? "None" : "Lax");
        response.addCookie(refreshCookie);
    }

    /**
     * Limpia una cookie invalidándola (MaxAge = 0)
     */
    private void clearCookie(HttpServletResponse response, String name, String path) {
        Cookie cookie = new Cookie(name, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecureEnvironment());
        cookie.setPath(path);
        cookie.setMaxAge(0);  // Expirar inmediatamente
        response.addCookie(cookie);
    }

    /**
     * Determina si estamos en ambiente seguro (producción)
     */
    private boolean isSecureEnvironment() {
        // Use Spring Environment active profiles to detect running profile reliably
        String[] active = environment.getActiveProfiles();
        if (active == null) return false;
        for (String p : active) {
            if ("prod".equalsIgnoreCase(p)) return true;
        }
        return false;
    }
}
