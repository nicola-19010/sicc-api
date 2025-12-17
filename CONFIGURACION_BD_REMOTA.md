# Configuración BD Remota - Producción

## ✅ Cambios realizados

Se ha actualizado la configuración de conexión a base de datos para que **en producción el backend se conecte a la BD remota** (otra VPS), no a la BD del contenedor Docker local.

---

## 🔄 Flujo actual (CORRECTO)

```
┌──────────────────────────────────────────────┐
│  GitHub CI/CD                                │
│  1. Detecta push a main                      │
│  2. Build & Test (Maven)                     │
│  3. Build & Push imagen Docker               │
│  4. Despliega en VPS                         │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│  VPS (producción)                            │
│                                              │
│  .env (generado por CI/CD):                  │
│  SPRING_DATASOURCE_URL=                      │
│  jdbc:postgresql://192.168.1.100:5435/siccdb│
│                                              │
│  POSTGRES_PASSWORD=xxx                       │
│  SECURITY_JWT_SECRET_KEY=yyy                 │
│  GROQ_API_KEY=zzz                            │
│  CORS_ALLOWED_ORIGINS=https://...           │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│  docker-compose.yml                          │
│                                              │
│  backend:                                    │
│    environment:                              │
│      SPRING_DATASOURCE_URL: ${...}  ← .env  │
│      POSTGRES_PASSWORD: ${...}     ← .env  │
│      ... (otros secrets)                     │
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│  Spring Boot (dentro del contenedor)         │
│                                              │
│  application-prod.yml:                       │
│  datasource.url:                             │
│    ${SPRING_DATASOURCE_URL}  ← docker-compose│
└──────────────────────────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│  BD REMOTA (otra VPS)                        │
│                                              │
│  PostgreSQL 5435                             │
│  Host: 192.168.1.100                         │
│  DB: siccdb                                  │
│  User: sicc                                  │
└──────────────────────────────────────────────┘
```

---

## 📝 Cambios específicos

### 1. `docker-compose.yml` ✅

**Antes (INCORRECTO):**
```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/siccdb  # ← Apunta a contenedor local
```

**Después (CORRECTO):**
```yaml
environment:
  SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL}  # ← Lee del .env (CI/CD)
```

---

### 2. `.github/workflows/cicd.yml` ✅

**Antes (INCORRECTO):**
```yaml
cat > .env << EOF
POSTGRES_PASSWORD=${{ secrets.POSTGRES_PASSWORD }}
SECURITY_JWT_SECRET_KEY=${{ secrets.SECURITY_JWT_SECRET_KEY }}
...
EOF
# ← Faltaba SPRING_DATASOURCE_URL
```

**Después (CORRECTO):**
```yaml
cat > .env << EOF
SPRING_DATASOURCE_URL=jdbc:postgresql://192.168.1.100:5435/siccdb
POSTGRES_PASSWORD=${{ secrets.POSTGRES_PASSWORD }}
SECURITY_JWT_SECRET_KEY=${{ secrets.SECURITY_JWT_SECRET_KEY }}
GROQ_API_KEY=${{ secrets.GROQ_API_KEY }}
CORS_ALLOWED_ORIGINS=${{ secrets.CORS_ALLOWED_ORIGINS }}
EOF
```

---

### 3. `application-prod.yml` ✅

**Ya está CORRECTO (sin cambios):**
```yaml
datasource:
  url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://192.168.1.100:5435/siccdb}
  username: ${SPRING_DATASOURCE_USERNAME}
  password: ${SPRING_DATASOURCE_PASSWORD}
```

El default `192.168.1.100:5435` es el fallback si la variable no viene del CI/CD.

---

## 🔐 Seguridad

- ✅ La IP de la BD remota (`192.168.1.100:5435`) está en el CI/CD (`.github/workflows/cicd.yml`), no en código versionado
- ✅ Passwords y secrets vienen de GitHub Secrets
- ✅ El `.env` se genera en VPS en tiempo de despliegue (nunca se commitea)
- ✅ El `.env` tiene permisos `600` (solo lectura para el usuario que lo crea)

---

## 📊 Verificación: Logs esperados en producción

Cuando el backend inicie, busca en los logs estos mensajes:

```
HikariPool-1 - Starting...
HikariPool-1 - Added connection conn1: url=jdbc:postgresql://192.168.1.100:5435/siccdb user=sicc
```

✅ Si ves `192.168.1.100:5435` → **Conectado a BD remota correctamente**
❌ Si ves `db:5432` → **Aún conectado a BD local (error)**

---

## 🚀 Próximo despliegue

El próximo `git push` a `main` hará:

1. ✅ Build con Maven
2. ✅ Build imagen Docker
3. ✅ Push a Docker Hub
4. ✅ Deploy en VPS
5. ✅ Genera `.env` con `SPRING_DATASOURCE_URL=jdbc://192.168.1.100:5435/siccdb`
6. ✅ `docker compose up -d` lee `.env` y conecta a BD remota

---

## ⚠️ Importante

- La BD local del docker-compose (`db` service) **sigue existiendo** (no se eliminó)
- Ahora el backend la **ignora completamente** y se conecta a la BD remota
- El docker-compose la deja levantada pero **sin usarla** (puedes dejarla así o eliminarla después si no la necesitas)

---

## 📞 Checklist final

- ✅ `docker-compose.yml` → `SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL}`
- ✅ `cicd.yml` → `.env` incluye `SPRING_DATASOURCE_URL=jdbc://192.168.1.100:5435/siccdb`
- ✅ `application-prod.yml` → Variable resuelta desde docker-compose
- ✅ Cambios commiteados y pusheados a `main`
- ✅ CI/CD en progreso
- ✅ Verificar logs del backend en VPS después del despliegue


