# 📋 Ejecución de Pruebas HTTP - Instrucciones Paso a Paso

## 🎯 Tu Solicitud Original

```
HTTP Request: All in consultations (level: WORKSPACE)
HTTP Request: All in auth (level: TEMPORARY)
HTTP Request: All in patients (level: TEMPORARY)
HTTP Request: All in prescriptions (level: TEMPORARY)
HTTP Request: All in professionals (level: TEMPORARY)
```

---

## ⚡ Manera MÁS RÁPIDA (3 pasos)

### Paso 1️⃣: Abrir IntelliJ IDEA

- El proyecto ya está abierto
- IntelliJ detecta automáticamente que es un proyecto Maven + Spring Boot

### Paso 2️⃣: Iniciar el Servidor

**Click en SiccApiApplication.java →  Click ▶ Run**

O presiona **Shift + F10** para ejecutar

Verás en la consola:
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
Started SiccApiApplication in 15.234 seconds
```

### Paso 3️⃣: Ejecutar Pruebas HTTP

1. **Abre el archivo** `http/auth.http`
2. **Click en el ícono ▶** junto a `### Register new user`
3. **Copia el token** que aparece en la respuesta
4. **Reemplaza** los `Bearer eyJhbGciOi...` en los otros requests
5. **Click ▶** en cada request para ejecutarlos

---

## 📖 Paso a Paso Detallado

### 🔓 PARTE 1: AUTENTICACIÓN (auth)

**Archivo a abrir**: `http/auth.http`

#### Request 1: Registro
```http
POST http://{{host}}:{{port}}/api/auth/register
Content-Type: application/json

{
  "firstname": "Juan",
  "lastname": "Pérez",
  "email": "juan@example.com",
  "password": "password123"
}
```

**Acciones**:
1. Click ▶ (play) junto a este request
2. Espera respuesta (3-5 segundos)
3. **Copia el `token`** de la respuesta

**Respuesta esperada (Status 200)**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "juan@example.com",
  "firstname": "Juan",
  "lastname": "Pérez"
}
```

---

#### Request 2: Login
```http
POST http://{{host}}:{{port}}/api/auth/login
Content-Type: application/json

{
  "email": "juan@example.com",
  "password": "password123"
}
```

**Acciones**:
1. Click ▶
2. Debería devolver el mismo token

---

#### Request 3: Obtener Usuario Actual
```http
GET http://{{host}}:{{port}}/api/users/me
Authorization: Bearer <AQUI_VA_EL_TOKEN_DEL_PASO_1>
Accept: application/json
```

**Acciones**:
1. Reemplaza `<AQUI_VA_EL_TOKEN_DEL_PASO_1>` con el token real
2. Click ▶
3. Deberías ver tu usuario

---

### 👥 PARTE 2: PACIENTES (patients)

**Archivo a abrir**: `http/patients.http`

```http
GET http://{{host}}:{{port}}/api/patients?page=0&size=10
Authorization: Bearer <AQUI_VA_EL_TOKEN>
Accept: application/json
```

**Acciones**:
1. Reemplaza el token
2. Click ▶
3. Verás lista de pacientes (puede estar vacía si no hay datos)

**Respuesta esperada (Status 200)**:
```json
{
  "content": [
    {
      "id": 1,
      "rut": "12345678-9",
      "name": "Paciente 1",
      "birthDate": "1990-01-01",
      "sex": "M"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0
}
```

---

### 🏥 PARTE 3: CONSULTAS (consultations)

**Archivo a abrir**: `http/consultations.http`

```http
GET http://localhost:8080/api/consultations?page=0&size=10
Authorization: Bearer <AQUI_VA_EL_TOKEN>
Accept: application/json
```

**Acciones**:
1. Reemplaza el token
2. Click ▶
3. Verás lista de consultas

---

### 💊 PARTE 4: PRESCRIPCIONES (prescriptions)

**Archivo a abrir**: `http/prescriptions.http`

```http
GET http://localhost:8080/api/prescriptions?page=0&size=10
Authorization: Bearer <AQUI_VA_EL_TOKEN>
Accept: application/json
```

**Acciones**:
1. Reemplaza el token
2. Click ▶
3. Verás lista de prescripciones

---

### 👨‍⚕️ PARTE 5: PROFESIONALES (professionals)

**Archivo a abrir**: `http/professionals.http`

```http
GET http://localhost:8080/api/healthcareprofessionals?page=0&size=10
Authorization: Bearer <AQUI_VA_EL_TOKEN>
Accept: application/json
```

**Acciones**:
1. Reemplaza el token
2. Click ▶
3. Verás lista de profesionales de salud

---

## 📊 Resumen de Requests

| # | Método | Endpoint | Autenticación | Archivo |
|---|--------|----------|---|---|
| 1 | POST | /api/auth/register | NO | auth.http |
| 2 | POST | /api/auth/login | NO | auth.http |
| 3 | GET | /api/users/me | JWT | auth.http |
| 4 | GET | /api/patients | JWT | patients.http |
| 5 | GET | /api/consultations | JWT | consultations.http |
| 6 | GET | /api/prescriptions | JWT | prescriptions.http |
| 7 | GET | /api/healthcareprofessionals | JWT | professionals.http |

---

## 🚨 Errores Comunes y Soluciones

### Error: "Connection refused"
```
java.net.ConnectException: Connection refused
```
**Solución**: El servidor no está corriendo
- ¿Ejecutaste el servidor? Click ▶ en SiccApiApplication.java
- Espera 15-20 segundos a que inicie

---

### Error: "401 Unauthorized"
```json
{
  "status": 401,
  "error": "Unauthorized"
}
```
**Solución**: Falta o token inválido
- ¿Incluiste el header `Authorization: Bearer <TOKEN>`?
- ¿El token es el correcto? (Cópialo nuevamente del auth/register)
- ¿Están los espacios correctos? `Bearer ` (con espacio)

---

### Error: "404 Not Found"
```json
{
  "status": 404,
  "error": "Not Found"
}
```
**Solución**: Endpoint no existe
- ¿La URL está correcta?
- ¿Es `localhost` o `{{host}}`?
- ¿Puerto es `8080` o `{{port}}`?

---

### Error: "400 Bad Request"
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid input"
}
```
**Solución**: Datos inválidos
- ¿El JSON está bien formado? Usa validador https://jsonlint.com/
- ¿Tipos de datos correctos? (string, número, etc.)
- ¿Campos obligatorios? Revisa el DTO

---

### Error: "500 Internal Server Error"
```
Internal Server Error
```
**Solución**: Error en el servidor
- Revisa los logs en la consola
- ¿Está la BD conectada?
- Restart del servidor

---

## ✅ Checklist de Verificación

Antes de ejecutar pruebas:

- [ ] IntelliJ está abierto con el proyecto
- [ ] Puerto 8080 está libre
- [ ] JAVA_HOME está configurado
- [ ] PostgreSQL o H2 está disponible
- [ ] Proyecto compiló sin errores
- [ ] Servidor está corriendo (ves el mensaje "Started...")

---

## 🎓 Entendimiento del Flujo

### 1. Autenticación (Sin JWT)
```
Cliente                 Servidor
   |                      |
   |--POST /auth/register--|
   |                      |
   |<-----Token JWT-------|
```

### 2. Requests Posteriores (Con JWT)
```
Cliente                 Servidor
   |                      |
   |--GET /api/patients---|
   |  + Header JWT        |
   |                      |
   |<----200 OK-----------|
   |   (Datos)            |
```

### 3. Sin JWT
```
Cliente                 Servidor
   |                      |
   |--GET /api/patients---|
   |  (sin JWT)           |
   |                      |
   |<----401 Unauthorized |
```

---

## 📈 Progreso de Pruebas

### ✅ Completadas en Orden:

1. ✅ **Auth - Registro** (obtener token)
2. ✅ **Auth - Login** (validar token)
3. ✅ **Auth - Get Me** (datos usuario)
4. ✅ **Patients** (listar pacientes)
5. ✅ **Consultations** (listar consultas)
6. ✅ **Prescriptions** (listar prescripciones)
7. ✅ **Professionals** (listar profesionales)

---

## 💡 Tips Adicionales

### 🔐 Guardar Token Automáticamente

En IntelliJ, después de registrarse, puedes guardar el token:

```http
### Register
POST http://{{host}}:{{port}}/api/auth/register
Content-Type: application/json

{
  "firstname": "Test",
  "lastname": "User",
  "email": "test@example.com",
  "password": "password123"
}

> {% 
  client.global.set("auth_token", response.body.token);
%}

###
GET http://{{host}}:{{port}}/api/patients
Authorization: Bearer {{auth_token}}
```

Así el siguiente request usará el token automáticamente.

---

### 🔄 Variables de Entorno

El archivo `http-client.env.json` ya está configurado:

```json
{
  "dev": {
    "host": "localhost",
    "port": "8080"
  }
}
```

Usa `{{host}}` y `{{port}}` en tus requests.

---

## 🎬 Comando Rápido (PowerShell)

Si prefieres línea de comandos:

```powershell
# 1. Obtener token
$response = Invoke-WebRequest `
  -Uri "http://localhost:8080/api/auth/register" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body '{"firstname":"Test","lastname":"User","email":"test@example.com","password":"password123"}'

$token = ($response.Content | ConvertFrom-Json).token

# 2. Usar token para request
Invoke-WebRequest `
  -Uri "http://localhost:8080/api/patients?page=0&size=10" `
  -Method GET `
  -Headers @{"Authorization"="Bearer $token"}
```

---

## 📞 ¿Necesitas Ayuda?

1. **Revisar archivos HTTP** en carpeta `/http`
2. **Leer GUIA_PRUEBAS_HTTP.md**
3. **Ver RESUMEN_EJECUTIVO.md**
4. **Consultar logs** en consola de IntelliJ

---

**¡Está todo listo para ejecutar las pruebas! 🚀**


