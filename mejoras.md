

# 📘 PLAN_DE_TRABAJO_SICC_AUTH_SECURITY.md

```md
# PLAN DE TRABAJO — SICC  
## Autenticación, Usuarios y Seguridad (Access Token + Refresh Token + HttpOnly Cookies)

---

# 1. Objetivo General

Implementar un sistema de autenticación moderno y seguro basado en:

- JWT Access Tokens (corta duración)
- JWT Refresh Tokens (larga duración)
- Cookies seguras HttpOnly
- Arquitectura limpia por dominios funcionales
- Gestión segura de secretos mediante variables de entorno
- Pruebas unitarias e integración
- Compatibilidad con Angular mediante `withCredentials: true`

---

# 2. Arquitectura del Proyecto

```

src/main/java/cl/sicc/siccapi
│
├── auth
│   ├── controller
│   ├── domain
│   ├── dto
│   ├── service
│
├── user
│   ├── controller
│   ├── domain
│   ├── dto
│   ├── repository
│   ├── service
│   ├── infrastructure
│   ├── annotation
│
├── security
│   ├── config
│   ├── filter
│   └── service
│
└── config

````

---

# 3. Modelo de Tokens

## 3.1 Access Token
- Duración: 15 min  
- Uso: autorización en cada request  
- Entregado vía cookie `access_token` (HttpOnly, SameSite=None)

## 3.2 Refresh Token
- Duración: 7 días  
- Uso: regenerar access token  
- Entregado vía cookie `refresh_token` (HttpOnly, SameSite=None, Path=/api/auth/refresh)

---

# 4. Endpoints del Dominio Auth

| Endpoint | Método | Descripción |
|---------|--------|-------------|
| `/api/auth/register` | POST | Crear usuario y emitir tokens |
| `/api/auth/login` | POST | Validar credenciales y emitir tokens |
| `/api/auth/refresh` | POST | Emitir nuevo access token |
| `/api/auth/logout` | POST | Eliminar cookies y cerrar sesión |

---

# 5. Configuración de Seguridad

## 5.1 DEV
- Origen permitido: http://localhost:4200  
- Cookies permitidas con `withCredentials`  
- CSRF deshabilitado  
- Frames permitidos para H2 o Actuator si fuera necesario  

## 5.2 PROD
- Origen permitido: dominio del frontend
- Cookies con:
  - Secure = true
  - HttpOnly = true
  - SameSite = None
  - Path específico por cookie

---

# 6. Variables de Entorno y Secretos

## 6.1 application.yml
```yaml
spring:
  profiles:
    active: dev
````

## 6.2 application-dev.yml

```yaml
security:
  jwt:
    secret-key: dev-secret-key
    expiration-access: 900000        # 15 min
    expiration-refresh: 604800000    # 7 días
```

## 6.3 application-prod.yml

```yaml
security:
  jwt:
    secret-key: ${JWT_SECRET}
    expiration-access: ${JWT_ACCESS_EXPIRATION}
    expiration-refresh: ${JWT_REFRESH_EXPIRATION}
```

## 6.4 Ubicación recomendada de los secretos

* GitHub Secrets (CI/CD)
* Variables de entorno en Docker o servidor
* Nunca en GitHub
* Nunca dentro de la imagen Docker
* Nunca en application.yml

---

# 7. Cambios en Backend

## 7.1 AuthController

Debe:

* Registrar usuario
* Validar credenciales
* Emitir cookies con acces y refresh token
* Exponer endpoint de refresh
* Exponer logout seguro

## 7.2 JwtAuthenticationFilter

Debe:

* Leer token desde cookie `access_token`
* Validar token
* Manejar expiración

## 7.3 Refresh Token Flow

* Usuario obtiene nuevo access_token sin volver a loguearse
* refresh_token se valida y se renueva si es necesario

---

# 8. Integración con Angular (documentación)

## 8.1 Enviar cookies en cada request

```ts
http.get(url, { withCredentials: true });
```

## 8.2 Interceptor recomendado

* Si recibe 401 → llamar `/api/auth/refresh`
* Si refresh funciona → reintentar request original

## 8.3 Environments

```ts
export const environment = {
  apiUrl: 'http://localhost:8080',
  production: false
};
```

---

# 9. Flujo de Autenticación Completo

1. Usuario se loguea
2. Backend envía cookies (access + refresh)
3. Angular envía cookies con cada request
4. Access token expira → backend responde 401
5. Interceptor llama a `/api/auth/refresh`
6. Backend envía un nuevo access token
7. Interceptor reintenta la petición original
8. Usuario no pierde sesión

---

# 10. Pruebas

## 10.1 Pruebas Unitarias (JUnit)

### AuthServiceTests

* register_createsUser_and_emitsTokens
* login_validCredentials_returnsTokens
* refresh_validRefreshToken_returnsNewAccessToken

### JwtServiceTests

* generateAccessToken_containsCorrectClaims
* validateAccessToken_expired_false

### UserServiceTests

* loadUserByUsername_returnsCorrectUser

---

## 10.2 Pruebas de Integración (MockMvc)

### Registro

* 200 OK
* Cookies presentes

### Login

* 200 OK
* Cookies presentes

### Refresh

* 200 OK
* Nuevo access token

### Logout

* Cookies con Max-Age = 0

---

## 10.3 Pruebas Manuales

* Registrar usuario
* Login
* Consumir endpoint protegido
* Simular expiración de access token
* Confirmar que refresh funciona
* Confirmar logout elimina cookies

---

# 11. Checklist para Producción

* Secrets en variables de entorno
* Cookies Secure + HttpOnly + SameSite=None
* HTTPS obligatorio
* Desplegar con Docker sin secretos embebidos
* Revisar logs y monitoreo con Actuator
* Mantener refresh tokens con rotación segura

---

# Fin del documento

