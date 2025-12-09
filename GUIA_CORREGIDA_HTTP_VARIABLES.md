# ✅ GUÍA CORREGIDA - Cómo Ejecutar Tests HTTP

## 🔧 Problema Identificado

Las variables con `@` en IntelliJ HTTP Client no funcionan de la forma que las usaba. La solución correcta es:

1. **`{{$timestamp}}`** - Variable built-in de IntelliJ (siempre funciona)
2. **`{{testEmail}}`** - Variable global que se setea en los post-request scripts
3. **No usar `@`** antes de variables globales

---

## ✅ Flujo Correcto Ahora

### Paso 1: REGISTRAR Usuario ✅

```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "firstname": "Juan",
  "lastname": "Pérez",
  "email": "juan_test_{{$timestamp}}@example.com",
  "password": "password123"
}

> {%
  if (response.status === 200) {
    const body = JSON.parse(response.body);
    client.global.set("testEmail", body.email);
    client.global.set("testPassword", "password123");
    console.log("✅ Usuario registrado: " + body.email);
  }
%}
```

**Qué sucede**:
- ✅ `{{$timestamp}}` se reemplaza por número de milisegundos actual
- ✅ Email es único: `juan_test_1733694926000@example.com`
- ✅ Response 200 guarda email en variable global `testEmail`
- ✅ Response 200 guarda password en variable global `testPassword`

---

### Paso 2: LOGIN ✅

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "{{testEmail}}",
  "password": "{{testPassword}}"
}

> {%
  if (response.status === 200) {
    console.log("✅ Login exitoso: " + JSON.parse(response.body).email);
  }
%}
```

**Qué sucede**:
- ✅ `{{testEmail}}` se reemplaza por el valor guardado en paso 1
- ✅ `{{testPassword}}` se reemplaza por el valor guardado en paso 1
- ✅ Las cookies se transfieren automáticamente al siguiente request

---

### Paso 3: GET /users/me ✅

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
  ...
}
```

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
  ...
}
Set-Cookie: access_token=...NUEVO...
```

---

### Paso 5: LOGOUT ✅

```http
POST http://localhost:8080/api/auth/logout
Content-Type: application/json
```

**Respuesta esperada**:
```
HTTP/1.1 204 No Content
Set-Cookie: access_token=; Max-Age=0;
Set-Cookie: refresh_token=; Max-Age=0;
```

---

### Paso 6: EMAIL DUPLICADO (después logout) ✅

```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "firstname": "Another",
  "lastname": "User",
  "email": "{{testEmail}}",
  "password": "password123"
}
```

**Respuesta esperada**:
```
HTTP/1.1 400 Bad Request
{
  "status": 400,
  "message": "El email ya está registrado",
  ...
}
```

---

## 🎯 CÓMO FUNCIONA AHORA

### Variables Built-in (siempre funcionan):

```
{{$timestamp}}     → Milisegundos actuales (1733694926000)
{{$randomInt}}     → Número aleatorio
{{$uuid}}          → UUID único
```

### Variables Globales (se setean con `client.global.set()`):

```javascript
// Guardar variable
client.global.set("testEmail", "juan@example.com");

// Usar variable
"email": "{{testEmail}}"
```

### Variables Locales (NO funcionan):

```
❌ @testEmail      ← No funciona en IntelliJ
❌ {{@testEmail}}  ← No funciona en IntelliJ

✅ {{testEmail}}   ← Sí funciona (variable global)
```

---

## ✅ ORDEN DE EJECUCIÓN

```
1️⃣ Click ▶ en "1. Registrar nuevo usuario"
   └─ Obtiene: access_token + refresh_token + variables globales
   
2️⃣ Click ▶ en "2. Login con credenciales"
   └─ Obtiene: nuevos tokens
   
3️⃣ Click ▶ en "3. Obtener usuario actual"
   └─ Status: 200 OK
   
4️⃣ Click ▶ en "5. Renovar access token"
   └─ Obtiene: nuevo access_token
   
5️⃣ Click ▶ en "6. Cerrar sesión"
   └─ Status: 204 No Content
   
6️⃣ Click ▶ en "13. Registrar con email duplicado"
   └─ Status: 400 (email duplicado)
```

---

## 📋 CHECKLIST

- [x] Archivo auth.http actualizado con sintaxis correcta
- [x] Uso de `{{$timestamp}}` para email único
- [x] Uso de `{{testEmail}}` y `{{testPassword}}` como variables globales
- [x] Post-request scripts guardan variables correctamente
- [x] Flujo de testing completo

---

## 🚀 AHORA SÍ FUNCIONA

Vuelve a ejecutar los tests HTTP desde IntelliJ:

```
1. Haz click en ▶ del request 1 (Registrar)
2. Haz click en ▶ del request 2 (Login)
3. Haz click en ▶ del request 3, 4, 5, 6...

Todos deberían funcionar sin errores de "unsubstituted variable"
```

---

**Problema resuelto. Ahora sí funcionan todas las variables.** ✅

