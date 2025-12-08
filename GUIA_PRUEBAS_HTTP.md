# Guía de Ejecución de Pruebas HTTP - SICC API

## 📋 Descripción

Este documento proporciona instrucciones para ejecutar las pruebas HTTP contra la API SICC, incluyendo pruebas de autenticación, seguridad y acceso a los endpoints principales.

---

## 🚀 Requisitos Previos

1. **Java 17+** - Instalado y configurado
2. **Maven 3.8+** - Para compilar y ejecutar el servidor
3. **Python 3.7+** - Para ejecutar el script de pruebas
4. **PostgreSQL** - BD de producción (o H2 para desarrollo)

---

## 📖 Paso 1: Compilar el Proyecto

```bash
cd C:\Users\npach\IdeaProjects\sicc\sicc-api

# Compilar sin tests
mvnw.cmd clean compile -DskipTests

# O compilar con tests
mvnw.cmd clean verify
```

---

## 🔧 Paso 2: Iniciar el Servidor

### Opción A: Usando el script batch

```bash
cd C:\Users\npach\IdeaProjects\sicc\sicc-api
run_server.bat
```

### Opción B: Usando Maven directamente

```bash
cd C:\Users\npach\IdeaProjects\sicc\sicc-api
mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Opción C: Usando el IDE (IntelliJ)

1. Click derecho en `SiccApiApplication.java`
2. Seleccionar "Run 'SiccApiApplication'"

---

## 🧪 Paso 3: Ejecutar las Pruebas HTTP

### Opción A: Script Python (Recomendado)

```bash
cd C:\Users\npach\IdeaProjects\sicc\sicc-api

# Instalar dependencias
pip install requests

# Ejecutar pruebas
python test_api.py
```

O usar el script batch:

```bash
run_tests_http.bat
```

### Opción B: Usando HTTP Client de IntelliJ

1. Abrir `http/auth.http`
2. Click en el ícono de play ▶ junto a cada request
3. Ver resultados en el panel de respuesta

---

## 📝 Pruebas Disponibles

### Pruebas de Seguridad

1. **Sin token** - Verificar que se rechaza acceso sin autenticación
2. **Token inválido** - Verificar que se rechaza un token malformado

### Pruebas de Autenticación

1. **Registro** - Crear nuevo usuario con email único
2. **Login** - Autenticarse con email y password

### Pruebas de Endpoints Protegidos

1. **GET /api/users/me** - Obtener usuario actual
2. **GET /api/patients** - Listar pacientes (requiere JWT)
3. **GET /api/consultations** - Listar consultas (requiere JWT)
4. **GET /api/prescriptions** - Listar prescripciones (requiere JWT)
5. **GET /api/healthcareprofessionals** - Listar profesionales (requiere JWT)

---

## 🎯 Flujo de Pruebas Esperado

### 1. Validar Seguridad
```
[✓] Intento sin token → Status 401/403
[✓] Token inválido → Status 401/403
```

### 2. Registro
```
POST /api/auth/register
{
  "firstname": "Juan",
  "lastname": "Pérez",
  "email": "juan@example.com",
  "password": "password123"
}
↓
Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "juan@example.com",
  "firstname": "Juan",
  "lastname": "Pérez"
}
```

### 3. Acceso con Token
```
GET /api/patients
Authorization: Bearer <TOKEN>
↓
Response: [{ id: 1, name: "...", ... }, ...]
```

---

## 📊 Ejemplo de Output de Pruebas

```
============================================================
SUITE DE PRUEBAS HTTP - SICC API
============================================================

============================================================
Verificar disponibilidad del servidor
============================================================
→ GET /actuator/health
✓ Servidor disponible en http://localhost:8080

============================================================
PRUEBAS DE SEGURIDAD
============================================================

============================================================
Prueba sin token
============================================================
→ GET /api/patients (sin token)
→ Status: 401
✓ Correctamente rechazado (status 401)

============================================================
Registro de usuario
============================================================
→ POST /api/auth/register
→ Status: 200
✓ Registro exitoso
  Email: juan_1733611234@example.com
  Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...

...más pruebas...

============================================================
PRUEBAS COMPLETADAS
============================================================
```

---

## 🔍 Troubleshooting

### Error: "Servidor no disponible"
- Verificar que el servidor está corriendo en puerto 8080
- Verificar firewall no está bloqueando el puerto
- Intentar: `curl http://localhost:8080/actuator/health`

### Error: "Conexión rechazada"
- Esperar 10-15 segundos para que el servidor inicie completamente
- Verificar logs del servidor en consola

### Error: "Email ya registrado"
- El script usa emails con timestamp, pero si reusas el mismo email fallará
- Usar diferentes emails para cada prueba
- O borrar la BD y reiniciar

### Error: "Token inválido"
- Los tokens expiran después de 24 horas
- Ejecutar un nuevo registro para obtener un nuevo token

---

## 📁 Archivos de Prueba

| Archivo | Descripción |
|---------|------------|
| `test_api.py` | Script Python con todas las pruebas |
| `run_tests_http.bat` | Batch script para ejecutar pruebas |
| `run_server.bat` | Batch script para iniciar servidor |
| `http/auth.http` | Requests HTTP manuales para auth |
| `http/patients.http` | Requests HTTP manuales para pacientes |
| `http/consultations.http` | Requests HTTP manuales para consultas |
| `http/prescriptions.http` | Requests HTTP manuales para prescripciones |
| `http/professionals.http` | Requests HTTP manuales para profesionales |

---

## ✅ Criterios de Éxito

- ✅ Servidor inicia sin errores
- ✅ Pruebas sin token retornan 401
- ✅ Registro exitoso retorna token válido
- ✅ Login exitoso retorna token válido
- ✅ Con token se accede a endpoints protegidos
- ✅ Token inválido retorna 401
- ✅ Rol ADMIN puede acceder a /api/users/{id}
- ✅ Usuario normal no puede acceder a /api/users/{id}

---

## 🎓 Referencias

- [JWT.io](https://jwt.io/) - Validador de tokens JWT
- [Spring Security Docs](https://spring.io/projects/spring-security)
- [RestClient IntelliJ](https://www.jetbrains.com/help/idea/http-client-in-product-code-editor.html)

---

## 📞 Soporte

Si tienes problemas:

1. Verifica que los logs del servidor no muestren errores
2. Comprueba la BD está disponible
3. Verifica la configuración en `application.yml`
4. Consulta la documentación de [IMPLEMENTACION_COMPLETADA.md](./IMPLEMENTACION_COMPLETADA.md)


