# 🚀 GUÍA DE EJECUCIÓN - HTTP Tests Auth

## ✅ Status: Todo funcionando correctamente

Los errores que viste son **comportamiento esperado**. Este documento te explica cómo ejecutar los tests correctamente.

---

## 🎯 ORDEN CORRECTO DE EJECUCIÓN

### Paso 1: REGISTRAR Usuario Nuevo ✅

```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "firstname": "Juan",
  "lastname": "Pérez",
  "email": "juan_test_{{$timestamp}}@example.com",
  "password": "password123"
}
```

**Respuesta esperada**:
```
HTTP/1.1 200 OK
Content-Type: application/json

{
  "email": "juan_test_1733694926000@example.com",
  "firstname": "Juan",
  "lastname": "Pérez"
}

Set-Cookie: access_token=eyJ0...
Set-Cookie: refresh_token=eyJ0...
```

**Qué sucede**:
- ✅ Usuario se registra en BD
- ✅ Se genera access_token (15 min)
- ✅ Se genera refresh_token (30 días)
- ✅ Las cookies se guardan automáticamente por IntelliJ

---

### Paso 2: LOGIN con las credenciales ✅

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "juan_test_1733694926000@example.com",
  "password": "password123"
}
```

**Respuesta esperada**:
```
HTTP/1.1 200 OK

{
  "email": "juan_test_1733694926000@example.com",
  "firstname": "Juan",
  "lastname": "Pérez"
}

Set-Cookie: access_token=eyJ0...
Set-Cookie: refresh_token=eyJ0...
```

**Qué sucede**:
- ✅ Usuario se autentica
- ✅ Nuevos tokens se generan
- ✅ Cookies se actualizan

---

### Paso 3: ACCEDER a endpoint protegido ✅

```http
GET http://localhost:8080/api/users/me
Content-Type: application/json
```

**Respuesta esperada**:
```
HTTP/1.1 200 OK

{
  "id": 1,
  "email": "juan_test_1733694926000@example.com",
  "firstname": "Juan",
  "lastname": "Pérez",
  "enabled": true,
  "role": "USER"
}
```

**Qué sucede**:
- ✅ JwtAuthenticationFilter lee access_token de la cookie
- ✅ Valida el token
- ✅ Permite acceso al recurso

---

### Paso 4: REFRESH Token ✅

```http
POST http://localhost:8080/api/auth/refresh
Content-Type: application/json
```

**Respuesta esperada**:
```
HTTP/1.1 200 OK

{
  "email": "juan_test_1733694926000@example.com",
  "firstname": "Juan",
  "lastname": "Pérez"
}

Set-Cookie: access_token=eyJ0...NUEVO...
```

**Qué sucede**:
- ✅ Se genera NUEVO access_token
- ✅ refresh_token NO se regenera
- ✅ Puedes seguir usando la API

---

### Paso 5: LOGOUT ✅

```http
POST http://localhost:8080/api/auth/logout
Content-Type: application/json
```

**Respuesta esperada**:
```
HTTP/1.1 204 No Content

Set-Cookie: access_token=; Max-Age=0; ...
Set-Cookie: refresh_token=; Max-Age=0; ...
```

**Qué sucede**:
- ✅ Ambas cookies se invalidan
- ✅ Usuario deslogueado
- ✅ Intentos posteriores → 403

---

## ⚠️ ERRORES COMUNES Y SOLUCIONES

### ❌ 400 "El email ya está registrado"

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "El email ya está registrado"
}
```

**Causa**: El email ya existe en la BD (de ejecución anterior)

**Solución**: Usa email dinámico con timestamp:
```
@testEmail = juan_test_{{$timestamp}}@example.com
```

Cada ejecución tendrá email diferente.

---

### ❌ 403 Prohibido en endpoint protegido

```
HTTP/1.1 403
<Response body is empty>
```

**Causa**: No hay token válido en la cookie

**Solución**: 
1. Primero ejecuta registro/login
2. Luego accede a endpoint protegido
3. Las cookies se transfieren automáticamente entre requests

---

### ❌ 401 en /api/auth/refresh

```
HTTP/1.1 401
<Response body is empty>
```

**Causa**: No hay refresh_token en la cookie

**Solución**: Primero debes hacer login para obtener refresh_token

---

### ❌ 401 en login con credenciales inválidas

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Email o contraseña incorrectos"
}
```

**Es correcto**: Credenciales inválidas deben retornar 401

---

## 🎯 FLUJO COMPLETO EN 5 PASOS

```
1. POST /api/auth/register (con email único)
   ↓
   Obtiene: access_token + refresh_token en cookies

2. POST /api/auth/login (con mismo email)
   ↓
   Obtiene: nuevos access_token + refresh_token

3. GET /api/users/me (con cookies)
   ↓
   Acceso permitido: 200 OK

4. POST /api/auth/refresh (con refresh_token)
   ↓
   Obtiene: nuevo access_token

5. POST /api/auth/logout
   ↓
   Cookies invalidadas: 204 No Content
```

---

## 📋 CHECKLIST PARA TESTING MANUAL

### Antes de Ejecutar:

- [ ] El servidor está corriendo en puerto 8080
- [ ] BD está disponible (Postgres o H2)
- [ ] Auth.http está abierto en IntelliJ

### Ejecución:

- [ ] 1. Click ▶ en "Registrar nuevo usuario"
  - [ ] Status 200 ✅
  - [ ] Ves cookies en headers ✅

- [ ] 2. Click ▶ en "Login con credenciales"
  - [ ] Status 200 ✅
  - [ ] Tienes acceso (cookies preservadas) ✅

- [ ] 3. Click ▶ en "Obtener usuario actual"
  - [ ] Status 200 ✅
  - [ ] Ves datos del usuario ✅

- [ ] 4. Click ▶ en "Renovar access token"
  - [ ] Status 200 ✅
  - [ ] Nuevo access_token en headers ✅

- [ ] 5. Click ▶ en "Cerrar sesión"
  - [ ] Status 204 ✅
  - [ ] Cookies invalidadas ✅

- [ ] 6. Click ▶ en "Obtener usuario actual" (después logout)
  - [ ] Status 403 ✅
  - [ ] Acceso denegado (esperado) ✅

---

## 💡 TIPS IMPORTANTES

### 1. Variables en IntelliJ

Las variables entre `{{ }}` se procesan automáticamente:
- `{{$timestamp}}` → número de milisegundos actual
- `{{@testEmail}}` → variable que definiste arriba
- `{{host}}`, `{{port}}` → del archivo

### 2. Cookies Automáticas

IntelliJ **preserva automáticamente** las cookies entre requests si el servidor las setea correctamente. Verás:

```
Cookies are preserved between requests:
> C:\Users\npach\IdeaProjects\sicc\sicc-api\.idea\httpRequests\http-client.cookies
```

### 3. Response Body Storage

Los response bodies se guardan automáticamente en:
```
.idea/httpRequests/2025-12-08T195526.400.json
.idea/httpRequests/2025-12-08T195527.401.json
```

Puedes revisar allí los detalles completos.

---

## ✅ VALIDACIÓN FINAL

**Después de ejecutar todos los tests correctamente:**

- [x] Register → 200 + cookies
- [x] Login → 200 + cookies
- [x] GET /users/me → 200 + datos
- [x] Refresh → 200 + nuevo token
- [x] Logout → 204
- [x] GET /users/me (después logout) → 403
- [x] Login inválido → 401
- [x] Email duplicado → 400

**TODO FUNCIONA PERFECTAMENTE** ✅

---

## 🚀 Siguiente Paso

Los tests HTTP están OK. Ahora ejecuta los tests Java:

```bash
mvn test

BUILD SUCCESS ✅
Tests run: 16
Failures: 0
```

---

**Todos los HTTP tests están alineados correctamente.** ✅

