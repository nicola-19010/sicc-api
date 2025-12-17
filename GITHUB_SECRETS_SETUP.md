# GitHub Secrets - Base de datos remota

## 🔐 Secrets que debes agregar en GitHub

Ve a: **Repository Settings → Secrets and variables → Actions → New repository secret**

Agrega los siguientes secrets:

### 1. **BD_HOST**
```
Nombre: BD_HOST
Valor: 200.13.5.5
```

### 2. **BD_PORT**
```
Nombre: BD_PORT
Valor: 5432
```

### 3. **SPRING_DATASOURCE_USERNAME**
```
Nombre: SPRING_DATASOURCE_USERNAME
Valor: sicc
```

### 4. **POSTGRES_PASSWORD**
```
Nombre: POSTGRES_PASSWORD
Valor: <tu_contraseña_bd_remota>
```

### 5. **SECURITY_JWT_SECRET_KEY**
```
Nombre: SECURITY_JWT_SECRET_KEY
Valor: <tu_jwt_secret_en_base64>
```

### 6. **GROQ_API_KEY**
```
Nombre: GROQ_API_KEY
Valor: <tu_groq_api_key>
```

### 7. **CORS_ALLOWED_ORIGINS**
```
Nombre: CORS_ALLOWED_ORIGINS
Valor: https://tudominio.com
```

### 8. **DOCKER_USERNAME** (ya debería existir)
```
Nombre: DOCKER_USERNAME
Valor: nicolaspa23
```

### 9. **DOCKER_PASSWORD** (ya debería existir)
```
Nombre: DOCKER_PASSWORD
Valor: <tu_docker_hub_token>
```

### 10. **VPS_HOST** (ya debería existir)
```
Nombre: VPS_HOST
Valor: <ip_vps_produccion>
```

### 11. **VPS_USER** (ya debería existir)
```
Nombre: VPS_USER
Valor: <usuario_vps>
```

### 12. **VPS_SSH_KEY** (ya debería existir)
```
Nombre: VPS_SSH_KEY
Valor: <tu_clave_ssh_privada>
```

---

## ✅ Checklist de Secrets

- [ ] BD_HOST = 200.13.5.5
- [ ] BD_PORT = 5432
- [ ] SPRING_DATASOURCE_USERNAME = sicc
- [ ] POSTGRES_PASSWORD = (contraseña BD remota)
- [ ] SECURITY_JWT_SECRET_KEY = (JWT secret en Base64)
- [ ] GROQ_API_KEY = (API key Groq)
- [ ] CORS_ALLOWED_ORIGINS = (tu dominio)
- [ ] DOCKER_USERNAME = nicolaspa23
- [ ] DOCKER_PASSWORD = (token Docker Hub)
- [ ] VPS_HOST = (IP VPS)
- [ ] VPS_USER = (usuario VPS)
- [ ] VPS_SSH_KEY = (clave SSH privada)

---

## 🔄 Cómo se usa en CI/CD

El archivo `.github/workflows/cicd.yml` ahora construye el `.env` así:

```bash
cat > .env << EOF
SPRING_DATASOURCE_URL=jdbc:postgresql://200.13.5.5:5432/siccdb
SPRING_DATASOURCE_USERNAME=sicc
POSTGRES_PASSWORD=<valor_del_secret>
SECURITY_JWT_SECRET_KEY=<valor_del_secret>
GROQ_API_KEY=<valor_del_secret>
CORS_ALLOWED_ORIGINS=<valor_del_secret>
EOF
```

---

## 🚀 Próximo despliegue

Cuando hagas `git push` a `main`:

1. ✅ GitHub Actions lee los secrets
2. ✅ Construye `.env` con valores reales
3. ✅ Sube los archivos a VPS
4. ✅ Ejecuta `docker compose up -d`
5. ✅ Backend se conecta a BD remota en `200.13.5.5:5432`

---

## ⚠️ Seguridad

- ✅ **Nunca** commitees valores sensibles en el código
- ✅ **Siempre** usa GitHub Secrets
- ✅ El `.env` se genera en VPS en tiempo de deploy (nunca se versionea)
- ✅ El `.env` tiene permisos `chmod 600` (solo lectura para el usuario)


