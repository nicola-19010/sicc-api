# Seguridad y CORS — Configuración para producción

Resumen corto

Este documento resume los cambios realizados para garantizar una configuración segura de CORS, cookies HttpOnly (JWT en cookies) y manejo de secretos en producción, junto con instrucciones de verificación y recomendaciones operativas.

Cambios realizados (lista corta)

- Backend
  - Añadida la clase `SecurityConfigProd` (perfil `prod`) que:
    - Lee `cors.allowed-origins` desde propiedades (inyectada desde `FRONTEND_URL`).
    - Configura `CorsConfiguration.setAllowedOrigins(...)`, métodos permitidos y `allowCredentials(true)`.
    - Restringe endpoints públicos solo a `/api/auth/**` y actuator mínimo (`/actuator/health`, `/actuator/info`).
  - Añadido `application-prod.yml` con propiedades de ejemplo:
    - `cors.allowed-origins: ${FRONTEND_URL}`
    - `security.jwt.secret-key: ${SECURITY_JWT_SECRET_KEY}`
  - `AuthenticationService` actualizado:
    - Usa `Environment` para detectar perfil `prod` (en vez de `System.getProperty`).
    - Ajusta `SameSite` de cookies: `None` en prod (secure) y `Lax` en dev.
  - `SecurityConfigDev` mantiene un comportamiento más flexible para desarrollo (allowed origins `localhost:4200`, `localhost:3000`, `allowCredentials=true`).

- Frontend
  - Nuevo interceptor `credentials.interceptor.ts` que añade `withCredentials: true` a peticiones dirigidas a `'/api/'` o al `environment.apiUrl`/localhost:8080.
  - Registrado `credentialsInterceptor` en `app.config.ts` (modo standalone) junto al `authInterceptor` y `apiInterceptor`.
  - `auth.interceptor.ts` actualizado para, si `refresh` falla, limpiar usuario y redirigir a `/login`.
  - `current-user.service.ts` ajustado para actualizar el BehaviorSubject al recargar `/users/me`.
  - `environment.ts` (dev) cambiado a `apiUrl: '/api'` y añadido `proxy.conf.json` para pruebas con proxy dev.

Motivación y justificación breve

- No usar `'*'` en `allowedOrigins`: cuando `allowCredentials=true`, los navegadores ignoran `'*'` y no enviarán cookies; además es un riesgo de seguridad permitir credenciales desde cualquier origen.
- Cookies HttpOnly y dominio explícito: cookies cross-site necesitan `SameSite=None; Secure` y un `Access-Control-Allow-Origin` explícito para que el navegador las envíe en peticiones con credenciales.
- Variables de entorno para secretos/domains: el `SECRET_KEY` y la URL del frontend varían por entorno y jamás deben estar en el control de versiones.

Variables de entorno importantes (ejemplos)

- FRONTEND_URL: https://app.example.com
- SECURITY_JWT_SECRET_KEY: <BASE64_SECRET_KEY>

Estos son inyectados en `application-prod.yml` como `${FRONTEND_URL}` y `${SECURITY_JWT_SECRET_KEY}`.

Archivo `application-prod.yml` (relevante)

```yaml
# application-prod.yml (resumen)
cors:
  allowed-origins: ${FRONTEND_URL}
  allow-credentials: true

security:
  jwt:
    secret-key: ${SECURITY_JWT_SECRET_KEY}
    expiration-access: 1800000
    expiration-refresh: 2592000000

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

Pasos de verificación (dev)

1. Backend: ejecutar en perfil `dev` (o por defecto). `SecurityConfigDev` ya permite `http://localhost:4200` y `allowCredentials=true`.
2. Frontend: arrancar con proxy: `npm run start -- --proxy-config proxy.conf.json`.
3. Hacer login desde UI y verificar en DevTools → Network:
   - Response de `POST /api/auth/login` contiene `Set-Cookie: access_token=...` y `Set-Cookie: refresh_token=...`.
   - Las posteriores peticiones a `/api/users/me` deben incluir `Cookie: access_token=...` en Request Headers.

Pasos de verificación (staging/prod)

1. Configura variables de entorno (ej. en server/CI):
   - FRONTEND_URL=https://app.example.com
   - SECURITY_JWT_SECRET_KEY=<tu base64 secret>
2. Arranca la app con perfil prod: `java -jar -Dspring.profiles.active=prod target/*.jar`.
3. Asegúrate que el frontend se sirve desde el dominio `FRONTEND_URL` por HTTPS.
4. En DevTools Network realizar el login y comprobar:
   - Respuesta de login incluye `Set-Cookie` con `SameSite=None; Secure`.
   - Requests a `/api/**` incluyen `Cookie: access_token=...` y las respuestas a endpoints privados devuelven 200; si no, revisar `Access-Control-Allow-Origin` y `Access-Control-Allow-Credentials`.

Notas de despliegue y CI/CD

- Al desplegar, configura `FRONTEND_URL` y `SECURITY_JWT_SECRET_KEY` vía secretos del entorno (GitHub Actions Secrets, Vault, etc.).
- Asegura que la clave JWT sea suficientemente larga y esté en base64 compatible con `JwtService`.

Mapping de requisitos → estado

1) CORS en producción: DONE
   - `SecurityConfigProd` usa `setAllowedOrigins` con lista inyectada desde `cors.allowed-origins`.
2) Configuración por propiedades: DONE
   - `application-prod.yml` emplea `${FRONTEND_URL}`.
3) SecurityConfigProd: DONE
   - lee `cors.allowed-origins`, setAllowedOrigins(...) y `allowCredentials(true)`.
4) Buenas prácticas: PARTIAL/DONE
   - JWT secret leído de `SECURITY_JWT_SECRET_KEY`.
   - Actuator limitado en prod.
   - Se recomienda integrar con Secrets Manager (sugerencia).
5) Comentarios: DONE
   - Comentarios explicativos incluidos en `SecurityConfigProd` y `AuthenticationService`.

Posibles mejoras / próximos pasos sugeridos

- Reemplazar la lectura `@Value("${cors.allowed-origins}")` por `@ConfigurationProperties("cors")` para admitir lista YAML nativa.
- Añadir tests de integración que simulen CORS/credentials para los flujos de refresh/login.
- Verificar en staging que cookies se reciben con `SameSite=None; Secure` y que el dominio coincide exactamente con `FRONTEND_URL`.
- Integrar una comprobación startup que falle si `SECURITY_JWT_SECRET_KEY` está ausente o es corta.

Archivos clave modificados / creados

- `src/main/java/cl/sicc/siccapi/config/SecurityConfigProd.java` (nuevo)
- `src/main/resources/application-prod.yml` (nuevo)
- `src/main/java/cl/sicc/siccapi/auth/service/AuthenticationService.java` (modificado)
- `src/main/java/cl/sicc/siccapi/config/SecurityConfigDev.java` (existente, revisado)
- `src/app/interceptors/credentials.interceptor.ts` (frontend)
- `src/app/interceptors/auth.interceptor.ts` (frontend, ajuste redirect)
- `src/app/pages/auth/services/current-user.service.ts` (frontend)
- `src/app/app.config.ts` (frontend, registrado interceptor)
- `proxy.conf.json` (frontend dev proxy)

Contacto / notas finales

Si quieres, puedo:
- Implementar `@ConfigurationProperties` para `cors` (cambio pequeño y recomendado).
- Limpiar warnings estáticos en frontend (imports/unused warnings).
- Añadir un script o tarea pre-deploy para validar que `FRONTEND_URL` y `SECURITY_JWT_SECRET_KEY` están definidos.

---
Documento generado automáticamente por cambios recientes en el repo para soporte de producción segura (CORS + cookies HttpOnly + secrets por env). Revisa en staging antes del deploy final.

