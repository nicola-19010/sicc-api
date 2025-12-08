# Plan de Trabajo — Implementación de Autenticación (JWT), Usuario y Seguridad en SICC

Este documento define el plan de trabajo para que un agente de Copilot implemente el módulo de autenticación y usuarios en el backend SICC siguiendo la arquitectura modular existente.

---

## 1. Objetivo General

Implementar:
- Módulo `auth` con login y registro.
- Módulo `user` para gestión de usuarios.
- Seguridad basada en JWT.
- Integración con Spring Security 6 / Spring Boot 3.5+.
- Soporte para roles (ADMIN / USER).
- Testing básico de endpoints críticos.

---

## 2. Arquitectura del Backend (resumen)

Se agregan los módulos:

```
auth/
user/
security/
```

Los módulos actuales **no se alteran**.

---

## 3. Estructura Final Esperada

### 3.1 Módulo `user/`

```
user/
 ├── controller/
 ├── service/
 ├── repository/
 ├── domain/
 ├── dto/
 └── annotation/
```

Requisitos:
- Usuario implementa `UserDetails`
- Rol implementa `GrantedAuthority`
- Email único
- Password encriptado
- Métodos `findByEmail` en repositorio

---

### 3.2 Módulo `auth/`

Contiene:
- Login
- Registro
- DTOs de autenticación

Endpoints:
- `/api/auth/login`
- `/api/auth/register`

---

### 3.3 Módulo `security/`

Incluye:
- `SecurityConfig`
- `JwtService`
- `JwtAuthenticationFilter`

JWT protege rutas `/api/**`.

---

## 4. Tareas Prioritarias

### 4.1 Módulo USER
- [ ] Crear entidad User
- [ ] Crear enum Role
- [ ] Implementar UserDetails
- [ ] Crear repositorio
- [ ] Crear servicio
- [ ] Crear DTO y mapper
- [ ] Validación email único
- [ ] Controlador opcional

### 4.2 Módulo AUTH
- [ ] Crear DTOs (login, register, response)
- [ ] Crear AuthenticationService
- [ ] Crear AuthenticationController
- [ ] Generación de JWT

### 4.3 Módulo SECURITY
- [ ] Crear JwtService
- [ ] Crear JwtAuthenticationFilter
- [ ] Configurar SecurityConfig

---

## 5. Endpoints propuestos

| Método | Endpoint | Público | Descripción |
|--------|----------|---------|-------------|
| POST | `/api/auth/login` | Sí | Login |
| POST | `/api/auth/register` | Sí | Registro de usuario |
| GET | `/api/users/me` | JWT | Usuario autenticado |
| GET | `/api/users/{id}` | ADMIN | Buscar usuario |

---

## 6. Testing Manual Rápido

### Registro
```
POST /api/auth/register
{
  "firstname": "Juan",
  "lastname": "Pérez",
  "email": "juan@example.com",
  "password": "123456"
}
```

### Login
```
POST /api/auth/login
{
  "email": "juan@example.com",
  "password": "123456"
}
```

### Endpoint protegido
```
GET /api/consultations
Authorization: Bearer <TOKEN>
```

---

## 7. Testing Automatizado

Copilot debe generar:

### Unit tests
- AuthenticationServiceTest
- UserServiceTest
- JwtServiceTest

### Security tests
- 401 sin token
- 403 token inválido
- 200 token válido

### Integration tests
- Registro real
- Login real
- Acceso a endpoint protegido

---

## 8. Checklist final

- [ ] Módulo auth completo
- [ ] Módulo user completo
- [ ] Seguridad JWT funcional
- [ ] Roles aplicados
- [ ] Tests mínimos
- [ ] Frontend puede autenticarse

---

## 9. Criterio de Done

- El login funciona
- El registro funciona
- Los endpoints existentes se protegen
- Angular puede consumir la API segura
- Hay tests que validan el flujo principal
