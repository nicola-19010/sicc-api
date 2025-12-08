package cl.sicc.siccapi.security.filter;

import cl.sicc.siccapi.security.service.JwtService;
import cl.sicc.siccapi.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        // No aplicar filtro a rutas públicas
        return path.startsWith("/api/auth/") ||
               path.startsWith("/actuator") ||
               path.startsWith("/error") ||
               path.equals("/api/auth/refresh") ||
               path.equals("/api/auth/logout");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String token = extractAccessToken(request);

            if (token != null) {
                processTokenAuthentication(token, request);
            }
        } catch (Exception e) {
            log.error("Error procesando autenticación JWT: {}", e.getMessage());
            // No lanzar excepción, dejar que continúe (401 será manejado por Spring Security)
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el access token desde cookies o header Authorization
     */
    private String extractAccessToken(HttpServletRequest request) {
        // 1. Intentar desde cookie HttpOnly (preferido)
        String token = extractTokenFromCookie(request, "access_token");

        // 2. Fallback a header Bearer (para compatibilidad con Postman/tests)
        if (token == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        return token;
    }

    /**
     * Procesa la autenticación si el token es válido
     */
    private void processTokenAuthentication(String token, HttpServletRequest request) {
        String userEmail = jwtService.extractUsername(token);

        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userService.loadUserByUsername(userEmail);

            if (jwtService.isTokenValid(token, userDetails)) {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                context.setAuthentication(authToken);
                SecurityContextHolder.setContext(context);

                log.debug("Autenticación JWT establecida para usuario: {}", userEmail);
            }
        }
    }

    /**
     * Extrae token de cookie HttpOnly
     */
    private String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}

