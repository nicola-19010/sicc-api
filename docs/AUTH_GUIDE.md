# AUTH_GUIDE — Revisión real y basada en el código (SICC API)

Este documento describe exactamente (sin suposiciones) cómo funciona la autenticación en este repositorio. Todas las afirmaciones están respaldadas por ruta de archivo y fragmento de código breve. Si algo no existe en el código, se indica explícitamente.

Resumen rápido de hallazgos:
- Mecanismo de autenticación: JWT firmado con HS256 (evidencia: `JwtService`).
- Tokens entregados por cookies HttpOnly: `access_token` y `refresh_token` (evidencia: `AuthenticationService`).
- Endpoints de auth implementados en `AuthenticationController`:
  - POST `/api/auth/register`
  - POST `/api/auth/login`
  - POST `/api/auth/refresh`
  - POST `/api/auth/logout`
  (ver evidencia más abajo)
- Filtro de autenticación: `JwtAuthenticationFilter` (extrae token de cookie `access_token`, con fallback a header `Authorization`).
- Manejo global de errores: `GlobalExceptionHandler` (mapea RuntimeException y BadCredentialsException a respuestas con `ErrorResponse`).
- Security config (dev/prod/test): CORS con allowCredentials = true, CSRF deshabilitado, SessionManagement stateless, rutas `/api/auth/**` permitidas.

---

Checklist (qué hay en este documento):
- [x] Endpoints reales (método, path, request DTO, response DTO, códigos esperados)
- [x] Dónde y cómo se generan los JWT (claims, expiraciones, algoritmo)
- [x] Dónde se extraen/validan tokens en requests (cookie/header)
- [x] Configuración de Spring Security y CORS (evidencia)
- [x] Manejo de errores y formato de respuesta de error (evidencia)
- [x] Tests que validan comportamiento (evidencia)

---

1) Endpoints de autenticación (evidencia)

- Archivo: `src/main/java/cl/sicc/siccapi/auth/controller/AuthenticationController.java`

Fragmento (controller y endpoints):

```java
// src/.../AuthenticationController.java
@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request, HttpServletResponse response) { ... }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody LoginRequest request, HttpServletResponse response) { ... }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(HttpServletRequest request, HttpServletResponse response) { ... }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) { ... }
}
```

Para cada endpoint se documenta lo que hay en el código:

- POST `/api/auth/register`
  - Request DTO: `src/main/java/cl/sicc/siccapi/auth/dto/RegisterRequest.java`

    ```java
    // RegisterRequest
    public class RegisterRequest {
        private String firstname;
        private String lastname;
        private String email;
        private String password;
    }
    ```

  - Response DTO (body): `src/main/java/cl/sicc/siccapi/auth/dto/AuthenticationResponse.java`

    ```java
    // AuthenticationResponse
    public class AuthenticationResponse {
        private String email;
        private String firstname;
        private String lastname;
    }
    ```

  - Códigos observables en tests / implementación:
    - 200 OK en caso exitoso (ver `AuthenticationControllerTest#testRegisterSuccess`)
    - 4xx (Bad Request) para email duplicado (lanza `RuntimeException` y `GlobalExceptionHandler` lo traduce a 400). Evidencia: `AuthenticationService.register` lanza RuntimeException si `userRepository.existsByEmail(...)`.

  - ¿Setea cookies? Sí. Evidencia (snippet de `AuthenticationService`):

    ```java
    // setAccessTokenCookie and setRefreshTokenCookie called in register
    setAccessTokenCookie(response, accessToken);
    setRefreshTokenCookie(response, refreshToken);
    ```

    Detalles de cookies (implementación):

    ```java
    // setAccessTokenCookie
    Cookie accessCookie = new Cookie("access_token", accessToken);
    accessCookie.setHttpOnly(true);
    accessCookie.setSecure(isSecureEnvironment());
    accessCookie.setPath("/");
    accessCookie.setMaxAge((int) (jwtService.getAccessTokenExpiration() / 1000));
    accessCookie.setAttribute("SameSite", "Lax");
    response.addCookie(accessCookie);

    // setRefreshTokenCookie
    Cookie refreshCookie = new Cookie("refresh_token", refreshToken);
    refreshCookie.setHttpOnly(true);
    refreshCookie.setSecure(isSecureEnvironment());
    refreshCookie.setPath("/api/auth/refresh");
    refreshCookie.setMaxAge((int) (jwtService.getRefreshTokenExpiration() / 1000));
    refreshCookie.setAttribute("SameSite", "Lax");
    response.addCookie(refreshCookie);
    ```

    Por tanto: nombres `access_token` y `refresh_token`; `access_token` path `/`; `refresh_token` path `/api/auth/refresh`; HttpOnly=true; SameSite=Lax; Secure depende de `isSecureEnvironment()`.

- POST `/api/auth/login`
  - Request DTO: `LoginRequest` (`email`, `password`) — archivo: `src/main/java/cl/sicc/siccapi/auth/dto/LoginRequest.java`.
  - Response DTO: `AuthenticationResponse` (igual que register).
  - Códigos: 200 OK en success (`AuthenticationControllerTest#testLoginSuccess`); 4xx/401 en credenciales inválidas (AuthenticationManager lanzará BadCredentialsException que `GlobalExceptionHandler` mapea a 401 con mensaje "Email o contraseña incorrectos").
  - Cookies: igual que register (setAccessTokenCookie, setRefreshTokenCookie) — implementado en `AuthenticationService.login`.

- POST `/api/auth/refresh`
  - Implementación en `AuthenticationController.refresh` (ver archivo). El controller extrae cookie `refresh_token` manualmente:

    ```java
    private String extractRefreshTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
    ```

  - Si no hay `refresh_token` en cookies: el controller responde con 401 (línea: `return ResponseEntity.status(401).build();`).
  - `AuthenticationService.refresh(refreshToken, response)` valida el refresh via `JwtService` y si válido genera nuevo access token y lo setea en cookie (solo `access_token`) y devuelve `AuthenticationResponse`.
  - Si el refresh es inválido, `AuthenticationService.refresh` lanza `RuntimeException` y `AuthenticationController.refresh` captura y responde 401.

- POST `/api/auth/logout`
  - Llama `authenticationService.logout(response)` que ejecuta `clearCookie(response, "access_token", "/")` y `clearCookie(response, "refresh_token", "/api/auth/refresh")` y el controller responde `204 No Content`.

Evidencia de tests:
- `src/test/java/cl/sicc/siccapi/auth/controller/AuthenticationControllerTest.java` contiene tests que verifican existencia de `access_token` y `refresh_token` en `register` y `login`, que `refresh` responde 200 y setea `access_token`, y que `logout` responde 204.

---

2) Flujo real de JWT y configuración (evidencia)

- ¿Se usan JWT? Sí. Archivo: `src/main/java/cl/sicc/siccapi/security/service/JwtService.java`.

  - Métodos de generación:

    ```java
    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, accessTokenExpiration, "access");
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, refreshTokenExpiration, "refresh");
    }
    ```

  - Claims reales: `subject` es `userDetails.getUsername()` (email); además `buildToken` añade `extraClaims.put("tokenType", tokenType);` por lo que existe claim `tokenType` con valor `"access"` o `"refresh"`.

  - Expiraciones:
    - Propiedades usadas en `JwtService`:

      ```java
      @Value("${security.jwt.expiration-access:1800000}")
      private long accessTokenExpiration; // default 30min

      @Value("${security.jwt.expiration-refresh:2592000000}")
      private long refreshTokenExpiration; // default 30 days
      ```

    - Valores concretos en `application-dev.yml`:

      ```yaml
      security:
        jwt:
          secret-key: ZGV2LXNlY3JldC1rZXkt... (base64)
          expiration-access: 900000        # 15 minutos
          expiration-refresh: 2592000000   # 30 días
      ```

    - En `application-prod.yml` las expiraciones y el secret se leen desde variables de entorno (placeholders):

      ```yaml
      security:
        jwt:
          secret-key: ${SECURITY_JWT_SECRET_KEY}
          expiration-access: ${SECURITY_JWT_EXPIRATION_ACCESS:900000}
          expiration-refresh: ${SECURITY_JWT_EXPIRATION_REFRESH:2592000000}
      ```

  - Algoritmo y firma: `buildToken` usa `SignatureAlgorithm.HS256` y el `getSigningKey()` decodifica `secretKey` con Base64 y crea `Keys.hmacShaKeyFor(keyBytes)`.

    ```java
    return Jwts
        .builder()
        ...
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    ```

  - Conclusión: JWT HS256 firmados con el `secretKey` en Base64. En dev el secret está en `application-dev.yml` (base64), en prod se espera variable de entorno `SECURITY_JWT_SECRET_KEY`.

---

3) Dónde se lee el token en requests (evidencia)

- Filtro: `src/main/java/cl/sicc/siccapi/security/filter/JwtAuthenticationFilter.java`.

  - `shouldNotFilter` implementado:

    ```java
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") ||
               path.startsWith("/actuator") ||
               path.startsWith("/error") ||
               path.equals("/api/auth/refresh") ||
               path.equals("/api/auth/logout");
    }
    ```

    => El filtro NO se aplica a rutas bajo `/api/auth/` (incluye `/api/auth/refresh` y `/api/auth/logout`), `/actuator`, `/error`.

  - Extracción de token (método `extractAccessToken`): primera preferencia cookie `access_token`, y si no existe hace fallback a header `Authorization: Bearer <token>`:

    ```java
    private String extractAccessToken(HttpServletRequest request) {
        String token = extractTokenFromCookie(request, "access_token");
        if (token == null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }
        return token;
    }
    ```

  - Validación y carga de Authentication:

    ```java
    String userEmail = jwtService.extractUsername(token);
    UserDetails userDetails = userService.loadUserByUsername(userEmail);
    if (jwtService.isTokenValid(token, userDetails)) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
    ```

  - Comportamiento si falta token: el filtro no lanza excepción en el try-catch (cazado y logueado) y continúa la cadena del filtro; la decisión final de rechazar se hace por Spring Security según reglas de autorización (resultando en 401/403 según el caso).

---

4) Configuración de Spring Security y CORS (evidencia)

Se encontraron tres clases de configuración según perfil: `SecurityConfigDev`, `SecurityConfigProd`, `SecurityConfigTest`.

- Archivos:
  - `src/main/java/cl/sicc/siccapi/config/SecurityConfigDev.java`
  - `src/main/java/cl/sicc/siccapi/config/SecurityConfigProd.java`
  - `src/main/java/cl/sicc/siccapi/config/SecurityConfigTest.java`

Fragmentos clave (ejemplo `SecurityConfigDev`):

```java
// SecurityConfigDev
http
  .csrf(AbstractHttpConfigurer::disable)
  .cors(cors -> cors.configurationSource(corsConfigurationSource()))
  .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
  .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**").permitAll()
        .requestMatchers("/actuator/**").permitAll()
        .anyRequest().authenticated()
  )
  .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
```

- CORS (evidencia `SecurityConfigDev.corsConfigurationSource`):

```java
CorsConfiguration configuration = new CorsConfiguration();
configuration.setAllowedOriginPatterns(List.of("http://localhost:4200", "http://localhost:3000"));
configuration.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));
configuration.setAllowedHeaders(List.of("*"));
configuration.setAllowCredentials(true);
```

- En `SecurityConfigProd` `allowedOriginPatterns` es `List.of("*")` y también `setAllowCredentials(true)`.

- CSRF está explícitamente deshabilitado en dev/prod/test.
- SessionManagement: `SessionCreationPolicy.STATELESS` en dev/prod/test.
- Rutas públicas: `.requestMatchers("/api/auth/**").permitAll()` y `.requestMatchers("/actuator/**").permitAll()` (prod niega `/actuator/**` excepto health, ver `SecurityConfigProd`).

Consecuencias:
- Spring Security no crea sesión de servidor; autenticación se gestiona con JWT en cada petición.
- CORS permite credenciales (cookies) — por ello el frontend debe usar `withCredentials: true` para enviar/recibir cookies.

---

5) Manejo de errores (evidencia)

- `src/main/java/cl/sicc/siccapi/common/exception/GlobalExceptionHandler.java` es el manejador global.

Fragmentos y comportamiento:

- `RuntimeException` es interceptada y convertida en `ErrorResponse` con `HttpStatus.BAD_REQUEST` por defecto. Si el mensaje contiene la palabra "email" se sustituye por "El email ya está registrado".

```java
@ExceptionHandler(RuntimeException.class)
public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
    String message = ex.getMessage();
    HttpStatus status = HttpStatus.BAD_REQUEST;
    if (message != null && message.contains("email")) {
        message = "El email ya está registrado";
    }
    ErrorResponse errorResponse = ErrorResponse.builder()
            .status(status.value())
            .error(status.getReasonPhrase())
            .message(message)
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .build();
    return new ResponseEntity<>(errorResponse, status);
}
```

- `BadCredentialsException` es mapeada a 401 con mensaje "Email o contraseña incorrectos".
- `AuthenticationException` genérica -> 401 con mensaje "Autenticación fallida".
- Existe la clase DTO de error: `src/main/java/cl/sicc/siccapi/common/exception/ErrorResponse.java` con campos `status,error,message,timestamp,path`.

Ejemplo de uso práctico: si `AuthenticationService.register` lanza `RuntimeException("El email ya está registrado")`, el cliente recibirá 400 con JSON `ErrorResponse` y `message` = "El email ya está registrado".

---

6) Otros elementos relevantes (User entity, tests y properties)

- `User` entity (implementa `UserDetails`): `src/main/java/cl/sicc/siccapi/user/domain/User.java` — contiene `email`, `password`, `firstname`, `lastname`, `role` y `getAuthorities()` que devuelve `role.name()`.

- Tests importantes que confirman comportamiento:
  - `src/test/java/cl/sicc/siccapi/auth/controller/AuthenticationControllerTest.java` verifica register/login/refresh/logout y cookies.
  - `src/test/java/cl/sicc/siccapi/security/service/JwtServiceTest.java` y `AuthenticationServiceTest.java` usan `@TestPropertySource` para fijar `security.jwt.secret-key` en base64 (evidencia de la expectativa del formato base64).

- Properties y valores relevantes:
  - `src/main/resources/application-dev.yml` contiene `security.jwt.secret-key` (Base64) y expirations: `expiration-access: 900000` (15 min) y `expiration-refresh: 2592000000` (30 días).
  - `src/main/resources/application-prod.yml` usa placeholders `${SECURITY_JWT_SECRET_KEY}` etc., por lo que en producción el secreto debe suministrarse por variable de entorno.
  - `src/main/resources/application.yml` establece `hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect`.

---

7) Comportamientos/ambigüedades observadas y cómo se decide (evidencia)

- Fuente de token en request: el sistema prefiere cookie `access_token` pero acepta header `Authorization: Bearer ...` como fallback. Evidencia: `JwtAuthenticationFilter.extractAccessToken` usa cookie primero y luego header.

- Rutas excluidas del filtro: `/api/auth/**`, `/actuator`, `/error`, explícitamente listadas en `shouldNotFilter`. Esto evita que el filtro intente autenticar llamadas a `/api/auth/refresh` y `/api/auth/logout`.

- `Secure` en cookies depende de `isSecureEnvironment()` que usa `System.getProperty("spring.profiles.active", "")` y el método devuelve `true` si el perfil contiene `prod`. Evidencia: método `isSecureEnvironment()` en `AuthenticationService`.

---

8) Cosas NO encontradas en este repo (y dónde sería típico buscarlas)

- No encontrado: implementación de blacklist/invalidation server-side para refresh tokens (lista de tokens revocados). Lugar típico: `TokenRepository` o `RefreshToken` entity/service. Si se quisiera, típicamente estaría en `src/main/java/.../auth/repository` o `auth/domain/RefreshToken.java`.

- No encontrado: `AuthenticationEntryPoint` personalizado ni `AccessDeniedHandler` personalizado (no hay clases que implementen esas interfaces). Las respuestas 401/403 dependen del manejador por defecto de Spring Security más el `GlobalExceptionHandler` para excepciones lanzadas por el código.

---

9) Recomendaciones prácticas (basadas en lo encontrado — para frontend y mantenimiento)

- Frontend SPA (Angular): debe enviar `withCredentials: true` en peticiones a endpoints protegidos y a `/api/auth/login`, `/api/auth/register`, `/api/auth/refresh`, `/api/auth/logout` porque el backend configura CORS con `allowCredentials=true` y setea cookies HttpOnly.
  - Evidencia: `SecurityConfigDev.corsConfigurationSource()` y `SecurityConfigProd.corsConfigurationSource()` usan `configuration.setAllowCredentials(true)`.

- Para el secreto JWT en producción: configurar `SECURITY_JWT_SECRET_KEY` como Base64 en las variables de entorno del deployment/CI. Evidencia: `application-prod.yml` usa `${SECURITY_JWT_SECRET_KEY}` y `JwtService.getSigningKey()` decodifica Base64.

- Si necesitas invalidar refresh tokens (logout forzado o revocación), actualmente no hay persistencia de refresh tokens ni blacklist; deberías agregar un `RefreshToken` entity y validar server-side en `AuthenticationService.refresh`.

- Tests: los tests existentes (`AuthenticationControllerTest`) están alineados con el comportamiento actual: esperan cookies `access_token` y `refresh_token`, 200/4xx/401/204 según casos. Puedes usar esos tests como referencia para cambios.

---

10) Archivos clave (lista rápida con rutas)

- Controllers / services / DTOs:
  - `src/main/java/cl/sicc/siccapi/auth/controller/AuthenticationController.java`
  - `src/main/java/cl/sicc/siccapi/auth/service/AuthenticationService.java`
  - `src/main/java/cl/sicc/siccapi/auth/dto/RegisterRequest.java`
  - `src/main/java/cl/sicc/siccapi/auth/dto/LoginRequest.java`
  - `src/main/java/cl/sicc/siccapi/auth/dto/AuthenticationResponse.java`
- JWT and filter:
  - `src/main/java/cl/sicc/siccapi/security/service/JwtService.java`
  - `src/main/java/cl/sicc/siccapi/security/filter/JwtAuthenticationFilter.java`
- Security config:
  - `src/main/java/cl/sicc/siccapi/config/SecurityConfigDev.java`
  - `src/main/java/cl/sicc/siccapi/config/SecurityConfigProd.java`
  - `src/main/java/cl/sicc/siccapi/config/SecurityConfigTest.java`
- Error handling:
  - `src/main/java/cl/sicc/siccapi/common/exception/GlobalExceptionHandler.java`
  - `src/main/java/cl/sicc/siccapi/common/exception/ErrorResponse.java`
- Entity:
  - `src/main/java/cl/sicc/siccapi/user/domain/User.java`
- Properties:
  - `src/main/resources/application-dev.yml`
  - `src/main/resources/application-prod.yml`
  - `src/main/resources/application.yml`
- Tests:
  - `src/test/java/cl/sicc/siccapi/auth/controller/AuthenticationControllerTest.java`

---

Conclusión

He documentado exactamente lo que está implementado en el repo. El sistema usa JWT firmados HS256, con `tokenType` claim, `access_token` y `refresh_token` entregados por cookies HttpOnly; el backend admite también header `Authorization` como fallback. Security está configurado para ser stateless, con CORS y allowCredentials=true.

Si quieres, hago en el siguiente paso (elige una):
- 1) Generar `docs/AUTH_GUIDE.md` extendido con ejemplos curl/Angular (con `withCredentials`) basados estrictamente en el código.
- 2) Implementar cambios mínimos (p. ej. mejorar mensajes de error, o ajustar `shouldNotFilter`) y ejecutar tests.
- 3) Generar snippets Angular `AuthService` + `AuthInterceptor` basados exactamente en la API encontrada.

Indica la opción (1/2/3) o pide otra tarea y la ejecuto.  
