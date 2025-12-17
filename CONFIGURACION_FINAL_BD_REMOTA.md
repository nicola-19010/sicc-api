# Configuración Final - BD Remota con Secrets

## 📊 Resumen de cambios

### ✅ Cambio 1: `.github/workflows/cicd.yml`

**ANTES (incorrecto - IP hardcodeada):**
```yaml
SPRING_DATASOURCE_URL=jdbc:postgresql://192.168.1.100:5435/siccdb
```

**DESPUÉS (correcto - del secret de GitHub):**
```yaml
SPRING_DATASOURCE_URL=jdbc:postgresql://${{ secrets.BD_HOST }}:${{ secrets.BD_PORT }}/siccdb
SPRING_DATASOURCE_USERNAME=${{ secrets.SPRING_DATASOURCE_USERNAME }}
```

---

### ✅ Cambio 2: `application-prod.yml`

**ANTES (IP antigua):**
```yaml
url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://192.168.1.100:5435/siccdb}
```

**DESPUÉS (IP correcta):**
```yaml
url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://200.13.5.5:5432/siccdb}
username: ${SPRING_DATASOURCE_USERNAME}
password: ${SPRING_DATASOURCE_PASSWORD}
```

---

## 🔐 Secrets requeridos en GitHub

| Secret | Valor |
|---|---|
| `BD_HOST` | `200.13.5.5` |
| `BD_PORT` | `5432` |
| `SPRING_DATASOURCE_USERNAME` | `sicc` |
| `POSTGRES_PASSWORD` | (tu contraseña BD remota) |
| `SECURITY_JWT_SECRET_KEY` | (tu JWT secret en Base64) |
| `GROQ_API_KEY` | (tu API key Groq) |
| `CORS_ALLOWED_ORIGINS` | (tu dominio, ej: https://tudominio.com) |

---

## 🔄 Flujo completo (SEGURO)

```
┌─────────────────────────────────────────────┐
│  GitHub Secrets (MÁXIMA SEGURIDAD)          │
│  - BD_HOST: 200.13.5.5                      │
│  - BD_PORT: 5432                            │
│  - POSTGRES_PASSWORD: ***                   │
│  - SECURITY_JWT_SECRET_KEY: ***             │
│  - GROQ_API_KEY: ***                        │
└─────────────────────────────────────────────┘
              ↓ (CI/CD)
┌─────────────────────────────────────────────┐
│  .github/workflows/cicd.yml                 │
│  Lee secrets → construye .env               │
│  SPRING_DATASOURCE_URL=                     │
│  jdbc://200.13.5.5:5432/siccdb              │
└─────────────────────────────────────────────┘
              ↓ (Despliega)
┌─────────────────────────────────────────────┐
│  VPS Producción                             │
│  .env (temporal, generado en deploy)        │
│  docker-compose.yml                         │
│  ${SPRING_DATASOURCE_URL}                   │
└─────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────┐
│  Spring Boot (application-prod.yml)         │
│  ${SPRING_DATASOURCE_URL}                   │
│  → jdbc://200.13.5.5:5432/siccdb            │
└─────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────┐
│  BD REMOTA (otra VPS)                       │
│  PostgreSQL 200.13.5.5:5432                 │
│  Database: siccdb                           │
└─────────────────────────────────────────────┘
```

---

## 📋 Checklist antes de hacer push

- [ ] Todos los secrets están agregados en GitHub
- [ ] CI/CD file actualizado (`${{ secrets.BD_HOST }}` etc)
- [ ] `application-prod.yml` apunta a `200.13.5.5:5432`
- [ ] `docker-compose.yml` usa `${SPRING_DATASOURCE_URL}`
- [ ] No hay valores hardcodeados en archivos versionados

---

## 🚀 Próximo paso

1. Agrega los secrets en GitHub (ver `GITHUB_SECRETS_SETUP.md`)
2. Haz `git commit` y `git push` a `main`
3. GitHub Actions ejecutará el CI/CD
4. El backend se conectará a `200.13.5.5:5432`

---

## 📝 Verificación en producción

Después del despliegue, busca en los logs del backend:

```
HikariPool-1 - Starting...
HikariPool-1 - Added connection conn1: url=jdbc:postgresql://200.13.5.5:5432/siccdb user=sicc
```

✅ Si ves `200.13.5.5:5432` → **Conectado a BD remota correctamente**


