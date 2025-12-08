# 🧪 Pruebas HTTP - Guía Rápida

## Resumen de Pruebas

Basándome en tu solicitud de ejecutar pruebas HTTP en los siguientes endpoints:
- ✅ consultations (WORKSPACE)
- ✅ auth (TEMPORARY) 
- ✅ patients (TEMPORARY)
- ✅ prescriptions (TEMPORARY)
- ✅ professionals (TEMPORARY)

---

## 🔐 Paso Previo: Autenticación

Para acceder a los endpoints protegidos, primero debes autenticarte.

### 1. Registro (Crear usuario)

**Request:**
```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "firstname": "Juan",
  "lastname": "Pérez",
  "email": "juan_test@example.com",
  "password": "password123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "juan_test@example.com",
  "firstname": "Juan",
  "lastname": "Pérez"
}
```

**Guardar el token** para las pruebas siguientes.

### 2. O Login (Si ya tienes usuario)

**Request:**
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "juan_test@example.com",
  "password": "password123"
}
```

**Response:** Same as register (retorna token)

---

## 🧪 Pruebas Principales

### ✅ 1. CONSULTATIONS - Listar Consultas

**Request:**
```http
GET http://localhost:8080/api/consultations?page=0&size=10
Authorization: Bearer <AQUI_VA_TU_TOKEN>
Accept: application/json
```

**Headers necesarios:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

**Response esperada (Status 200):**
```json
{
  "content": [
    {
      "id": 1,
      "date": "2025-01-15",
      "type": "Consulta General",
      "patientId": 1,
      "professionalId": 2
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 0
}
```

---

### ✅ 2. AUTH - Autenticación

#### 2a. Registro
```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "firstname": "Ana",
  "lastname": "García",
  "email": "ana_garcia@example.com",
  "password": "secure_pass_123"
}
```

#### 2b. Login
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "ana_garcia@example.com",
  "password": "secure_pass_123"
}
```

#### 2c. Obtener Usuario Actual
```http
GET http://localhost:8080/api/users/me
Authorization: Bearer <TOKEN>
Accept: application/json
```

**Response (Status 200):**
```json
{
  "id": 1,
  "firstname": "Ana",
  "lastname": "García",
  "email": "ana_garcia@example.com",
  "role": "USER",
  "enabled": true
}
```

---

### ✅ 3. PATIENTS - Listar Pacientes

**Request:**
```http
GET http://localhost:8080/api/patients?page=0&size=10
Authorization: Bearer <TOKEN>
Accept: application/json
```

**Response esperada (Status 200):**
```json
{
  "content": [
    {
      "id": 1,
      "rut": "12345678-9",
      "name": "Carlos López",
      "birthDate": "1980-05-20",
      "sex": "M",
      "residentialSector": "Santiago Centro",
      "fonasaTier": "A"
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "currentPage": 0
}
```

---

### ✅ 4. PRESCRIPTIONS - Listar Prescripciones

**Request:**
```http
GET http://localhost:8080/api/prescriptions?page=0&size=10
Authorization: Bearer <TOKEN>
Accept: application/json
```

**Response esperada (Status 200):**
```json
{
  "content": [
    {
      "id": 1,
      "consultationId": 1,
      "date": "2025-01-15",
      "medications": [
        {
          "id": 1,
          "name": "Paracetamol",
          "dosage": "500mg",
          "instructions": "Cada 8 horas"
        }
      ]
    }
  ],
  "totalElements": 3,
  "totalPages": 1,
  "currentPage": 0
}
```

---

### ✅ 5. PROFESSIONALS - Listar Profesionales de Salud

**Request:**
```http
GET http://localhost:8080/api/healthcareprofessionals?page=0&size=10
Authorization: Bearer <TOKEN>
Accept: application/json
```

**Response esperada (Status 200):**
```json
{
  "content": [
    {
      "id": 1,
      "rut": "11111111-1",
      "name": "Dr. Roberto Martínez",
      "specialty": "Medicina General"
    },
    {
      "id": 2,
      "rut": "22222222-2",
      "name": "Dra. Patricia González",
      "specialty": "Cardiología"
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "currentPage": 0
}
```

---

## 🚀 Cómo Ejecutar las Pruebas

### Opción A: Desde IntelliJ IDEA

1. **Abrir los archivos HTTP**:
   - `http/auth.http` - Autenticación
   - `http/consultations.http` - Consultas
   - `http/patients.http` - Pacientes
   - `http/prescriptions.http` - Prescripciones
   - `http/professionals.http` - Profesionales

2. **Hacer clic en el ícono ▶** junto a cada request

3. **Ver resultados** en el panel lateral

### Opción B: Usando Postman/Insomnia

1. **Crear nueva request** para cada endpoint
2. **Seleccionar método HTTP** (GET, POST)
3. **Agregar URL**: `http://localhost:8080/api/...`
4. **Headers**: `Authorization: Bearer <TOKEN>`
5. **Click Send**

### Opción C: Usando cURL

```bash
# 1. Obtener token
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstname":"Test",
    "lastname":"User",
    "email":"test@example.com",
    "password":"password123"
  }' | jq -r '.token')

# 2. Usar token para consultas
curl -X GET http://localhost:8080/api/patients?page=0 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json"
```

### Opción D: Usando Python (Script incluido)

```bash
# Ejecutar suite completa de pruebas
python test_api.py
```

---

## 🔑 Token JWT

El token JWT se ve así:
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqdWFuQGV4YW1wbGUuY29tIiwiaWF0IjoxNzMzNjExMjAwLCJleHAiOjE3MzM2OTc2MDB9.signature
```

**Partes:**
1. Header (algoritmo): `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9`
2. Payload (datos): `eyJzdWIiOiJqdWFuQGV4YW1wbGUuY29tIn0`
3. Signature (firma): `signature`

**Duración**: 24 horas (86400 segundos)

---

## ✅ Códigos HTTP Esperados

| Request | Sin Token | Token Inválido | Con Token Válido |
|---------|-----------|---|---|
| GET /api/patients | 401 | 401 | 200 |
| GET /api/consultations | 401 | 401 | 200 |
| GET /api/prescriptions | 401 | 401 | 200 |
| GET /api/healthcareprofessionals | 401 | 401 | 200 |
| POST /api/auth/register | 200 | N/A | N/A |
| POST /api/auth/login | 200 | N/A | N/A |

---

## 🐛 Troubleshooting

### "401 Unauthorized"
**Causa**: Falta token o token inválido
**Solución**: 
- Verificar que pasaste el header `Authorization: Bearer <TOKEN>`
- Obtener nuevo token con /auth/register o /auth/login

### "403 Forbidden"
**Causa**: No tienes permisos (ej: rol ADMIN)
**Solución**:
- Usar usuario con rol ADMIN para endpoints que lo requieran

### "404 Not Found"
**Causa**: Endpoint no existe
**Solución**:
- Verificar URL correcta
- Servidor debe estar ejecutándose en localhost:8080

### "400 Bad Request"
**Causa**: Datos inválidos en request
**Solución**:
- Verificar JSON está bien formado
- Verificar tipos de datos (string, número, etc.)

### "Server Connection Refused"
**Causa**: Servidor no está corriendo
**Solución**:
- Ejecutar `run_server.bat` o `mvnw.cmd spring-boot:run`
- Esperar a que inicie completamente

---

## 📊 Variables en http-client.env.json

```json
{
  "dev": {
    "host": "localhost",
    "port": "8080"
  },
  "prod": {
    "host": "api.sicc.com",
    "port": "443"
  }
}
```

**Uso en requests:**
```http
GET http://{{host}}:{{port}}/api/patients
```

---

## ✨ Tips Pro

1. **Guardar token en variable**: 
   - IntelliJ permite guardar respuestas en variables para reutilizar

2. **Testing encadenado**:
   ```http
   > {% 
     client.global.set("auth_token", response.body.token);
   %}
   ```

3. **Validar JSON**:
   - Usar https://jsonlint.com/ para verificar formato

4. **Decodificar JWT**:
   - Usar https://jwt.io/ para ver contenido del token

---

**¡Listo! Ahora estás preparado para ejecutar todas las pruebas HTTP.**


