# 🔍 DIAGNÓSTICO Y SOLUCIÓN - JWT Secret Base64 Invalid

## ✅ Problema Identificado

**Error**: `Illegal base64 character: '-'`

**Causa**: El `security.jwt.secret-key` en `application-dev.yml` **NO estaba en formato base64 válido**.

---

## 🔧 La Solución

### Paso 1: Entender el Problema

La librería JJWT requiere que el secret-key sea:
- ✅ Codificado en Base64
- ✅ Mínimo 256 bits (32 caracteres decodificados)
- ✅ Sin caracteres especiales que no sean válidos en base64

### Paso 2: Lo Que Hice

#### ❌ INCORRECTO (lo que tenías):
```yaml
security:
  jwt:
    secret-key: dev-secret-key-do-not-use-in-production-1234567890-min-32-chars
```

Este string contiene guiones `-` que NO son válidos en base64 cuando se intenta decodificar.

#### ✅ CORRECTO (lo que actualicé):
```yaml
security:
  jwt:
    # Base64 encoded secret
    # Original: "dev-secret-key-do-not-use-in-production-minimum-32-chars"
    secret-key: ZGV2LXNlY3JldC1rZXktZG8tbm90LXVzZS1pbi1wcm9kdWN0aW9uLW1pbmltdW0tMzItY2hhcnM=
    expiration-access: 900000
    expiration-refresh: 2592000000
```

---

## 🎯 Cambios Realizados

### 1. application-dev.yml
✅ Actualizado `secret-key` a formato base64 válido
```
ZGV2LXNlY3JldC1rZXktZG8tbm90LXVzZS1pbi1wcm9kdWN0aW9uLW1pbmltdW0tMzItY2hhcnM=
```

### 2. application-test.yml (Nuevo)
✅ Creado archivo de configuración para tests con:
- H2 database (en memoria)
- JWT secret base64 válido
- Configuración optimizada para testing

### 3. http/auth.http
✅ Actualizado comentarios explicativos

---

## 🚀 Cómo Ejecutar Ahora

### Opción 1: Ejecutar con Maven (Dev)

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

Luego ejecutar requests HTTP desde `http/auth.http`

### Opción 2: Ejecutar con Docker Compose

```bash
docker-compose up -d
```

### Opción 3: Ejecutar Tests Java

```bash
mvn test
```

---

## ✅ Validación

Después de los cambios, deberías ver:

**POST /api/auth/register** → **200 OK** (antes era 400)

```json
{
  "email": "juan_test_1765234796@example.com",
  "firstname": "Juan",
  "lastname": "Pérez"
}
```

Y luego las variables globales se asignarán correctamente:
```
✅ testEmail = juan_test_1765234796@example.com
✅ testPassword = password123
```

---

## 📝 Base64 Encoding Reference

Si necesitas crear tu propio secret en base64:

```bash
# Linux/Mac
echo -n "tu-secret-key-minimo-32-caracteres" | base64

# Windows (PowerShell)
[Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes("tu-secret-key-minimo-32-caracteres"))
```

---

## 🎓 Lección Aprendida

✅ Los valores `security.jwt.secret-key` deben estar **siempre en Base64**
✅ Usa comentarios para documentar el valor original (sin encoding)
✅ Verifica que el secret tenga mínimo 256 bits

---

**El error está resuelto. Ahora sí funcionará.** ✅

