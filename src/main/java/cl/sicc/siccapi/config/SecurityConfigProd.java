package cl.sicc.siccapi.config;

import cl.sicc.siccapi.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Security configuration for PRODUCTION.
 *
 * Key points:
 * - CORS origins are loaded from properties (injected from environment) and MUST NOT be '*'.
 *   Browsers disallow sending credentials (cookies) when allowedOrigins is '*'.
 * - We explicitly enable allowCredentials(true) so that cookies HttpOnly are sent by browsers.
 * - JWT secret and other sensitive values are read from environment variables (see application-prod.yml)
 *   to avoid hardcoding secrets in source control.
 */
@Configuration
@Profile("prod")
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfigProd {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Accept a comma-separated list OR YAML list via property 'cors.allowed-origins'
    @Value("${cors.allowed-origins}")
    private String corsAllowedOriginsProp;

    @Bean
    public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Auth endpoints must be public
                        .requestMatchers("/api/auth/**").permitAll()
                        // Expose only minimal actuator endpoints in prod (health/info) if needed
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse allowed origins property: accept comma-separated value or YAML list mapping
        List<String> allowedOrigins = parseAllowedOrigins(corsAllowedOriginsProp);

        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            // Fail-fast: require explicit allowed origins in production
            throw new IllegalStateException("CORS allowed origins must be configured in production (cors.allowed-origins)");
        }

        // IMPORTANT: Do NOT use '*' when allowCredentials=true. Browsers will ignore cookies.
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // allow cookies (HttpOnly) to be sent

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> parseAllowedOrigins(String prop) {
        if (prop == null) return List.of();
        // If the property is a YAML list, Spring will provide comma-separated or a single entry.
        // Support both comma-separated string and single URL.
        return Arrays.stream(prop.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}

