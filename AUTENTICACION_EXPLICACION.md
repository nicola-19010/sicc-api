# Autenticación en SICc API — Explicación para desarrolladores y frontend

Este documento explica cómo funciona la autenticación en este proyecto Spring Boot, qué clases participan, los endpoints disponibles, el formato de las respuestas y las instrucciones concretas para implementar el frontend en Angular (login, register, refresh, logout). Está pensado para que lo entienda un desarrollador y pueda pasarlo a Copilot para generar código frontend.

NOTA: He hecho 2 supuestos razonables basados en el código y pruebas existentes: los nombres de las clases principales son `AuthenticationController`, `AuthenticationService`, `JwtAuthenticationFilter`, `JwtService` (o similar) y el DTO de respuesta es `AuthenticationResponse` (ya sin campo `token`). Si algún nombre difiere en tu código, reemplázalo por el real.

---

## Resumen del diseño

- Los JWT (access_token y refresh_token) se entregan y gestionan mediante cookies HttpOnly.
- El cuerpo de las respuestas de `register` y `login` NO contiene el token; devuelve un DTO con datos del usuario:
  - `{ "email": "...", "firstname": "...", "lastname": "..." }`
- Cookies usadas:
  - `access_token` — Path `/` — corta vida (15 minutos) — HttpOnly — Secure=true en producción — SameSite=Lax
  - `refresh_token` — Path `/api/auth/refresh` — larga vida (30 días) — HttpOnly — Secure=true en producción — SameSite=Lax
- Endpoints principales (prefijo `/api/auth`):
  - POST `/register` — crear usuario + set-cookies (access + refresh)
  - POST `/login` — autenticar + set-cookies (access + refresh)
  - POST `/refresh` — lee cookie `refresh_token`, si válida genera nuevo `access_token` (set-cookie `access_token`) — NO cambia el refresh_token
  - POST `/logout` — invalida cookies (set cookie con Max-Age=0)
- Para recursos protegidos (ej. `/api/patients`) el servidor exige `access_token` en cookie; el `JwtAuthenticationFilter` extrae y valida ese token.

---

## Clases backend (descripción por responsabilidad)

A continuación las clases/elementos clave y qué hacen (ajusta nombres si difieren):

- `AuthenticationController` (REST)
  - Rutas: `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout`
  - Recibe DTOs de entrada (`RegisterRequest`, `LoginRequest`), llama a `AuthenticationService` y crea/modifica cookies en la respuesta.
  - Devuelve `AuthenticationResponse` (email, firstname, lastname) en body.

- `AuthenticationService`
  - Métodos importantes:
    - `register(RegisterRequest)` — valida duplicados, crea `User`, guarda, genera tokens y devuelve `AuthenticationResponse`.
    - `login(LoginRequest)` — valida credenciales, genera tokens y devuelve `AuthenticationResponse`.
    - `refresh(HttpServletRequest, HttpServletResponse)` — valida refresh_token cookie, genera nuevo access_token, lo escribe como cookie y devuelve `AuthenticationResponse` o 401 si inválido.
    - `logout(HttpServletResponse)` — limpia cookies (access_token path=/, refresh_token path=/api/auth/refresh).
  - Importante: NO devolver tokens en el body; solo set-cookie.

- `JwtService` o utilitario JWT
  - Genera y valida tokens JWT.
  - Provee claims: subject (email), tokenType (`access` o `refresh`), iat, exp.
  - Secret/clave provista por variable de entorno (`SECURITY_JWT_SECRET_KEY` o similar), no en archivos versionados.

- `JwtAuthenticationFilter` (extends OncePerRequestFilter)
  - Extrae token desde la cookie `access_token` (NO desde header Authorization si el proyecto decidió cookies).
  - `shouldNotFilter` debe ignorar rutas: `/api/auth/**`, `/actuator/**`, `/error`.
  - Si token válido: carga `UsernamePasswordAuthenticationToken` en `SecurityContextHolder` con roles del `User`.
  - Debe estar diseñado para no romper pruebas con MockMvc — p.ej. soportar ausencia de token silenciosamente y dejar que Spring Security rechace cuando sea necesario.

- `GlobalExceptionHandler` (@RestControllerAdvice)
  - Atrapa `RuntimeException`, `DataIntegrityViolationException`, `MethodArgumentNotValidException`, etc.
  - Mapea caso duplicado de email -> `ResponseEntity` 400 con body `{ "error": "El email ya está registrado" }`.
  - Conviene exponer DTO de error estándar: `{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "...", "path": "/api/auth/register" }` (opcional más detallado).

- `User` entity y `Role` (ROLE_USER)
  - `User` contiene email, password (bcrypt), firstname, lastname, roles.
  - Las autoridades se toman del rol y deben mapearse a `GrantedAuthority` en el filtro.

---

## Configuración (qué debes revisar en backend)

- `application.yml` — SOLO valores comunes (puertos, formatos, logging), SIN credenciales.
- `application-dev.yml` — debe apuntar a Postgres en Docker `localhost:5435` (o `jdbc:postgresql://localhost:5435/dbname`).
- `application-prod.yml` — debe usar placeholders/env vars para credenciales y JWT; p.e. `${POSTGRES_USER}`, `${SECURITY_JWT_SECRET_KEY}`.
- El secreto JWT no debe estar en repositorio.
- Hibernate dialect: `org.hibernate.dialect.PostgreSQLDialect`.
- Activación de perfiles en docker: asegúrate de que el contenedor arranque con `SPRING_PROFILES_ACTIVE=prod`.

---

## Comportamiento HTTP y códigos esperados

- `POST /api/auth/register`
  - Entrada: `{ "firstname":"...", "lastname":"...", "email":"...", "password":"..." }`
  - Respuestas:
    - 200 OK: body `AuthenticationResponse` (email, firstname, lastname) + Set-Cookie access_token y refresh_token
    - 400 Bad Request: duplicado email -> `{ "error": "El email ya está registrado" }` (GlobalExceptionHandler)

- `POST /api/auth/login`
  - Entrada: `{ "email":"...", "password":"..." }`
  - Respuestas:
    - 200 OK: body `AuthenticationResponse` + Set-Cookie access_token y refresh_token
    - 401 Unauthorized: credenciales inválidas

- `POST /api/auth/refresh`
  - NO requiere body. Debe leer cookie `refresh_token`.
  - 200 OK: set-cookie `access_token` (nuevo) y body `AuthenticationResponse` (opcional)
  - 401 Unauthorized: refresh inválido o ausente

- `POST /api/auth/logout`
  - invalida ambas cookies: devuelve 204 No Content y Set-Cookie con Max-Age=0

- Recursos privados -> 403 cuando usuario no autenticado, 401 cuando token inválido.

---

## Cómo integrar con Angular (directrices concretas)

Principios clave:
- Las cookies `HttpOnly` no son accesibles desde JavaScript. Por eso:
  - No almacenes tokens en localStorage/sessionStorage.
  - Usa `withCredentials: true` en cada petición que debe enviar/recibir cookies.
  - Confía en el body y los códigos HTTP para actualizar el estado del cliente.

Recomendaciones prácticas para el frontend:

1) Configuración global del HttpClient

- Siempre enviar cookies:

```ts
// ejemplo de Angular service (HttpClient) - configurar por petición
this.http.post(url, body, { withCredentials: true })
```

- CORS en backend debe permitir credenciales y origen del frontend:
  - allowOrigins = `https://mi-frontend.com` (no `*`) y `allowCredentials=true`.

2) Flujo de Login

- Action: POST `/api/auth/login` con body `{ email, password }` y { withCredentials: true }
- Si 200 OK -> respuesta contiene `AuthenticationResponse` (email, firstname, lastname). Usa esto para establecer estado de usuario en el cliente (ej. store). Cookies se envían automáticamente y son HttpOnly.
- Si 401 -> mostrar mensaje de credenciales inválidas.

3) Flujo de Register

- POST `/api/auth/register` con datos de registro y { withCredentials: true }
- Si 200 OK -> usar `AuthenticationResponse` y proceder igual que login.
- Si 400 -> mostrar mensaje apropiado (p.ej. email duplicado).

4) Obtener usuario actual

- GET `/api/users/me` con { withCredentials: true }
- Si 200 -> body con datos del usuario; útil al refrescar la página.
- Si 403 -> no autenticado.

5) Refresh Token (estrategia de cliente)

Opciones:
- Opción A (recomendada): Implementar un interceptor HTTP que, al ver 401 en una petición protegida, realiza una llamada automática a `POST /api/auth/refresh` (withCredentials: true). Si `refresh` devuelve 200, reintenta la petición original una vez.
- Opción B: Llamada programada desde cliente (cada 10–12 minutos) para `POST /api/auth/refresh` y mantener sesión activa.

Interceptor simple (idea):
- Detecta 401 -> llama `authService.refresh()` -> si ok, reintenta la petición original -> si falla, redirige a login.

IMPORTANTE: la llamada a `/api/auth/refresh` recibirá y actualizará cookies; el front no puede leerlas, solo confía en el 200/401.

6) Logout

- Llamar `POST /api/auth/logout` con { withCredentials: true }.
- Si 204 OK -> limpiar estado de usuario en frontend (store), redirigir a login.

7) Manejo de errores y UX

- Para errores de validación (400) muestra mensajes del body (GlobalExceptionHandler debe exponer un mensaje claro).
- Para 401 -> redirigir a login o mostrar modal de re-login.
- Evitar retrys infinitos en caso de refresh fallido.

---

## Ejemplos concretos de requests/respuestas

Register (request):

```http
POST /api/auth/register
Content-Type: application/json

{
  "firstname":"Juan",
  "lastname":"Pérez",
  "email":"juan@example.com",
  "password":"password123"
}
```

Respuesta (200):

Headers incluirán:
```
Set-Cookie: access_token=<JWT>; Max-Age=900; Path=/; HttpOnly; SameSite=Lax; Secure (en prod)
Set-Cookie: refresh_token=<JWT>; Max-Age=2592000; Path=/api/auth/refresh; HttpOnly; SameSite=Lax; Secure (en prod)
```

Body:
```json
{ "email": "juan@example.com", "firstname": "Juan", "lastname": "Pérez" }
```

Login (request): similar a register pero endpoint `/api/auth/login`.

Refresh (request):
```
POST /api/auth/refresh  // body vacío
Cookie: refresh_token=<JWT>
```
Respuesta (200): Set-Cookie nuevo access_token. Body con user info opcional.

Logout (request):
```
POST /api/auth/logout
```
Respuesta: 204 No Content + Set-Cookie con Max-Age=0 para ambas cookies.

---

## Consejos y buenas prácticas

- NO intentar leer `access_token` o `refresh_token` en JavaScript; no son accesibles por diseño.
- Mantén SameSite=Lax en la mayoría de los escenarios de SPA/frontend hospedado en otro dominio; si todo está en el mismo dominio, `Strict` es más seguro.
- En producción usa `Secure=true` y `SameSite=Lax`.
- Para llamadas entre diferentes subdominios, validar política CORS y SameSite.
- Controla el tiempo de expiración: access ~15 min; refresh ~30 días. Si quieres más seguridad, guarda una lista de refresh-token invalidated server-side (opcional) para logout forzado.
- Protege la ruta `/api/auth/refresh` con `shouldNotFilter` en el filtro JWT para evitar bucles.

---

## Qué revisar en el backend para garantizar compatibilidad con frontend

1. `JwtAuthenticationFilter` debe leer `access_token` desde cookie y NO bloquear `/api/auth/**`, `/actuator/**`, `/error`.
2. `AuthenticationController` debe devolver `AuthenticationResponse` sin token en body y setear cookies correctamente en `HttpServletResponse`.
3. GlobalExceptionHandler debe devolver mensajes legibles para mostrar al usuario (p.ej. duplicado email -> 400 con mensaje).
4. CORS: `allowedOrigins` debe incluir la URL del frontend y `allowCredentials=true`.
5. `application-dev.yml` y `application-prod.yml` correctamente configurados (dev apunta a postgres en docker y prod usa variables de entorno).

---

## Checklist rápido para copiar a Copilot y generar frontend

- [ ] Usar HttpClient con { withCredentials: true }
- [ ] POST `/api/auth/login` -> si 200 guardar user state con el body
- [ ] POST `/api/auth/register` -> tratar 400 para email duplicado
- [ ] GET `/api/users/me` para sincronizar estado al cargar la app
- [ ] Implementar interceptor para refrescar token en 401 (llamar `/api/auth/refresh`)
- [ ] POST `/api/auth/logout` -> limpiar estado

---

Si quieres, puedo también:
- Generar un `AuthService` y `AuthInterceptor` completos en TypeScript/Angular basados en estas reglas.
- Revisar las clases Java reales en tu repo y ajustar este documento con nombres y fragmentos reales para que sea exacto.

Dime si quieres que proceda a crear los snippets Angular o que inspeccione los archivos Java concretos para alinear nombres y ejemplos reales en tu repo.
