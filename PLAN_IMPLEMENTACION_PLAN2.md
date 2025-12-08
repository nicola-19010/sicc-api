# 🚀 PLAN DE IMPLEMENTACIÓN - De Plan 1 a Plan 2 (Mejoras)

## 📊 Resumen Ejecutivo

Tienes dos opciones:

| Opción | Descripción | Tiempo | Beneficio |
|--------|---|---|---|
| A | Mantener Plan 1 (estable) | ✅ 0 horas | ✅ Sin riesgos |
| B | Agregar mejoras (Plan 2) | ⏱️ 8-10 horas | 🔐 Máxima seguridad |
| C | Implementar Plan 2 completo | ⏱️ 2-3 semanas | ✅✅ Enterprise-ready |

---

## 🎯 RECOMENDACIÓN: Opción B + C Gradual

**Mantener estable y evolucionar.**

```
AHORA (4-5 horas):
├─ Agregar Refresh Token
├─ Endpoint /auth/logout
├─ Token blacklist simple
└─ Tests

PRÓXIMAS SEMANAS:
├─ Cookies HttpOnly
├─ Angular interceptor
├─ Rate limiting
└─ Auditoría
```

---

## 🔨 FASE 1: Mejoras Selectas (4-5 horas)

### 1.1 Agregar Refresh Token a JwtService

```java
// security/service/JwtService.java

// Agregar estos métodos:

public String generateRefreshToken(UserDetails userDetails) {
    return buildToken(new HashMap<>(), userDetails, jwtRefreshExpiration);
}

public String generateAccessToken(UserDetails userDetails) {
    return buildToken(new HashMap<>(), userDetails, jwtAccessExpiration);
}

// En application.yml:
// security:
//   jwt:
//     expiration-access: 900000      # 15 minutos
//     expiration-refresh: 604800000  # 7 días
```

**Tiempo**: 45 minutos

---

### 1.2 Agregar Endpoint /auth/refresh

```java
// auth/controller/AuthenticationController.java

@PostMapping("/refresh")
public ResponseEntity<AuthenticationResponse> refresh(
    @RequestBody RefreshTokenRequest request
) {
    String refreshToken = request.getRefreshToken();
    String userEmail = jwtService.extractUsername(refreshToken);
    
    if (jwtService.isTokenValid(refreshToken, userService.loadUserByUsername(userEmail))) {
        User user = userService.findByEmail(userEmail).orElseThrow();
        String newAccessToken = jwtService.generateAccessToken(user);
        
        return ResponseEntity.ok(
            AuthenticationResponse.builder()
                .token(newAccessToken)
                .email(user.getEmail())
                .build()
        );
    }
    
    throw new InvalidTokenException("Invalid refresh token");
}

// DTO nuevo:
@Data
public class RefreshTokenRequest {
    private String refreshToken;
}
```

**Tiempo**: 1 hora

---

### 1.3 Agregar Logout Endpoint Real

```java
// auth/controller/AuthenticationController.java

@PostMapping("/logout")
public ResponseEntity<Void> logout(
    @RequestHeader("Authorization") String authHeader
) {
    // Opción 1: Solo limpiar el lado del cliente
    // (Frontend borra el token del localStorage)
    
    // Opción 2: Agregar a blacklist en servidor
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        tokenBlacklist.add(token, jwtService.extractExpiration(token));
    }
    
    return ResponseEntity.noContent().build();
}
```

**Tiempo**: 45 minutos

---

### 1.4 Tabla de Blacklist JWT

```sql
-- db/migration/V3__jwt_blacklist.sql

CREATE TABLE jwt_blacklist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token VARCHAR(1000) UNIQUE NOT NULL,
    token_type VARCHAR(20),  -- ACCESS o REFRESH
    expires_at TIMESTAMP NOT NULL,
    blacklisted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_jwt_blacklist_expires ON jwt_blacklist(expires_at);
```

**Implementar en Java:**

```java
// security/service/JwtBlacklistService.java

@Service
@RequiredArgsConstructor
public class JwtBlacklistService {
    
    private final JwtBlacklistRepository repository;
    
    public void blacklistToken(String token, Date expiresAt) {
        JwtBlacklist blacklist = new JwtBlacklist();
        blacklist.setToken(token);
        blacklist.setExpiresAt(expiresAt);
        blacklist.setBlacklistedAt(LocalDateTime.now());
        repository.save(blacklist);
    }
    
    public boolean isBlacklisted(String token) {
        return repository.existsByToken(token);
    }
    
    // Limpiar blacklist expirada
    @Scheduled(fixedDelay = 3600000)  // Cada hora
    public void cleanExpiredTokens() {
        repository.deleteByExpiresAtBefore(LocalDateTime.now());
    }
}
```

**Tiempo**: 1.5 horas

---

### 1.5 Tests para Refresh Flow

```java
// auth/controller/AuthenticationControllerTest.java

@Test
void testRefresh_validToken_returnsNewAccessToken() {
    // 1. Login
    AuthenticationResponse loginResponse = authenticationService.login(
        new LoginRequest("juan@example.com", "password123")
    );
    
    // 2. Esperar (simular expiración de access token)
    // 3. Refresh
    AuthenticationResponse refreshResponse = authenticationController.refresh(
        new RefreshTokenRequest(loginResponse.getRefreshToken())
    );
    
    // 4. Validar
    assertNotNull(refreshResponse.getToken());
    assertNotEquals(loginResponse.getToken(), refreshResponse.getToken());
}

@Test
void testLogout_validToken_addsToBlacklist() {
    // Login
    AuthenticationResponse response = authenticationService.login(...);
    
    // Logout
    authenticationController.logout("Bearer " + response.getToken());
    
    // Verificar que está en blacklist
    assertTrue(jwtBlacklistService.isBlacklisted(response.getToken()));
}
```

**Tiempo**: 1 hora

---

## ⏱️ TOTAL FASE 1: 5-6 horas

---

## 🔐 FASE 2: Cookies HttpOnly (2-3 horas)

### 2.1 Actualizar AuthenticationResponse

```java
// auth/dto/AuthenticationResponse.java

@Data
@Builder
public class AuthenticationResponse {
    private String email;
    private String firstname;
    private String lastname;
    // ❌ NO incluir tokens (van en cookies)
}
```

---

### 2.2 Setear Cookies en AuthController

```java
// auth/controller/AuthenticationController.java

@PostMapping("/login")
public ResponseEntity<AuthenticationResponse> login(
    @RequestBody LoginRequest request,
    HttpServletResponse response
) {
    AuthenticationResponse authResponse = authenticationService.login(request);
    User user = userService.findByEmail(request.getEmail()).orElseThrow();
    
    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);
    
    // Setear cookies
    addCookie(response, "access_token", accessToken, 15 * 60);  // 15 min
    addCookie(response, "refresh_token", refreshToken, 7 * 24 * 60 * 60);  // 7 días
    
    return ResponseEntity.ok(authResponse);
}

private void addCookie(
    HttpServletResponse response,
    String name,
    String value,
    int maxAge
) {
    Cookie cookie = new Cookie(name, value);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);  // HTTPS only
    cookie.setPath("/");
    cookie.setMaxAge(maxAge);
    cookie.setAttribute("SameSite", "None");  // Para CORS
    response.addCookie(cookie);
}
```

---

### 2.3 Actualizar JwtAuthenticationFilter

```java
// security/filter/JwtAuthenticationFilter.java

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        // ✅ NUEVO: Leer token de cookies en lugar de headers
        String token = extractTokenFromCookie(request, "access_token");
        
        if (token == null) {
            // ✅ Fallback a header Bearer
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }
        
        if (token != null) {
            String userEmail = jwtService.extractUsername(token);
            if (jwtService.isTokenValid(token, userService.loadUserByUsername(userEmail))) {
                // ... establecer autenticación
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String extractTokenFromCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
```

**Tiempo**: 1.5 horas

---

### 2.4 Actualizar SecurityConfig para Cookies

```java
// config/SecurityConfigProd.java

@Configuration
@Profile("prod")
public class SecurityConfigProd {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("https://yourdomain.com"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);  // ✅ IMPORTANTE para cookies
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

**Tiempo**: 1 hora

---

## ⏱️ TOTAL FASE 2: 3-4 horas

---

## 📱 FASE 3: Angular Interceptor Avanzado (2 horas)

```typescript
// src/app/core/interceptors/auth.interceptor.ts

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}
  
  intercept(
    req: HttpRequest<any>,
    next: HttpHandler
  ): Observable<HttpEvent<any>> {
    
    // ✅ Cookies se envían automáticamente con withCredentials
    const modifiedReq = req.clone({
      withCredentials: true
    });
    
    return next.handle(modifiedReq).pipe(
      catchError((error: HttpErrorResponse) => {
        
        if (error.status === 401) {
          // Token expiró - intentar refresh
          return this.handleTokenExpiration(req);
        }
        
        return throwError(() => error);
      })
    );
  }
  
  private handleTokenExpiration(req: HttpRequest<any>): Observable<HttpEvent<any>> {
    return this.authService.refresh().pipe(
      switchMap(() => {
        // Reintentar request original
        return next.handle(req.clone({
          withCredentials: true
        }));
      }),
      catchError(() => {
        // Refresh falló - logout
        this.router.navigate(['/login']);
        return throwError(() => new Error('Session expired'));
      })
    );
  }
}

// auth.service.ts
refresh(): Observable<void> {
  return this.http.post<void>(
    `${this.apiUrl}/auth/refresh`,
    {},
    { withCredentials: true }
  );
}
```

**Tiempo**: 2 horas

---

## ⏱️ TOTAL TODAS LAS FASES: 10-12 horas

---

## 📋 Orden de Implementación Recomendado

```
DÍA 1 (4-5 horas):
├─ 1.1 Refresh Token en JwtService
├─ 1.2 Endpoint /auth/refresh
├─ 1.3 Endpoint /auth/logout
└─ Tests básicos

DÍA 2 (3-4 horas):
├─ 2.1-2.4 Cookies HttpOnly
├─ Actualizar SecurityConfig
└─ Tests de cookies

DÍA 3 (2 horas):
├─ Angular interceptor
├─ E2E testing
└─ Documentación

TOTAL: 2-3 días de trabajo
```

---

## ✅ Checklist de Implementación

- [ ] Agregar método `generateRefreshToken` en JwtService
- [ ] Agregar propiedades en application.yml
- [ ] Endpoint POST /api/auth/refresh
- [ ] Endpoint POST /api/auth/logout
- [ ] Tabla jwt_blacklist en BD
- [ ] JwtBlacklistService
- [ ] Validar token contra blacklist en JwtService
- [ ] Setear cookies HttpOnly en AuthController
- [ ] Limpiar cookies en logout
- [ ] Actualizar JwtAuthenticationFilter (leer de cookies)
- [ ] Configurar CORS para credentials
- [ ] Actualizar SecurityConfig
- [ ] Tests para refresh
- [ ] Tests para logout
- [ ] Tests para blacklist
- [ ] Angular interceptor
- [ ] Documentación de cambios

---

## 🚀 Comando para Empezar

Cuando estés listo:

```bash
# Crear rama de features
git checkout -b feature/jwt-refresh-token

# Implementar fases 1-2 (8 horas)
# ...

# Crear PR
git push origin feature/jwt-refresh-token
```

---

## 📊 Impacto de Cambios

| Aspecto | Antes (Plan 1) | Después (Plan 2) |
|--------|---|---|
| Seguridad XSS | ⚠️ Token en localStorage | ✅ Token en HttpOnly |
| Duración sesión | 24 horas | 7 días (con refresh) |
| Logout | ❌ No real | ✅ Real |
| Experiencia usuario | ⚠️ Relogin cada 24h | ✅ Seamless |
| Complejidad | ✅ Simple | ⚠️ Media |

---

## 💡 Tips de Implementación

1. **Mantener compatibilidad hacia atrás**
   ```java
   // Aceptar token tanto en header como en cookie
   String token = extractTokenFromCookie() ?? extractTokenFromHeader();
   ```

2. **Testing en desarrollo**
   ```yaml
   # application-dev.yml
   security:
     cookies:
       secure: false  # Permite HTTP en desarrollo
   ```

3. **No romper requests existentes**
   - Frontend angular sigue funcionando
   - Postman sigue funcionando (con Bearer header)
   - Solo agregar soporte para cookies

---

## ⚠️ Cuidados Importantes

### NO OLVIDES:
- [ ] Limpiar cookies expiradas
- [ ] Validar refresh token
- [ ] Agregar tests antes de implementar
- [ ] Documentar cambios
- [ ] Revisar CORS en prod

### EVITA:
- ❌ Guardar secreto en código
- ❌ Usar secure=true en dev
- ❌ Cookies sin HttpOnly
- ❌ SameSite=None sin Secure
- ❌ Olvidar migración BD

---

**¿Deseas que implemente estas mejoras? ¿O prefieres que consolide primero el Plan 1?**


