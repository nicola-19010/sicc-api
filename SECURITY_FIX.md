# 🔐 Configuración de Seguridad - SICC API

## ⚠️ PROBLEMA: GitHub bloqueó el push por API key expuesta

GitHub detectó una API key en el archivo `docker-compose.yml` y bloqueó el push por razones de seguridad.

## ✅ SOLUCIÓN APLICADA

### 1. **Actualización de .gitignore**
Se agregó protección para archivos sensibles:
```gitignore
### Environment Variables ###
.env
.env.local
.env.production
.env.development
*.env
```

### 2. **Creación de .env.example**
Se creó un archivo de ejemplo sin credenciales reales para documentación.

### 3. **Pasos para resolver el bloqueo de GitHub**

#### Opción A: Permitir el secreto (NO RECOMENDADO)
GitHub te da la opción de permitir el push visitando:
https://github.com/nicola-19010/sicc-api/security/secret-scanning/unblock-secret/36GQZFOxcqa2BRyrNG4fCq0I1ZO

**⚠️ NO SE RECOMIENDA** porque expone tu API key públicamente.

#### Opción B: Rotar la API key y reescribir historial (RECOMENDADO)

1. **Rotar la API key de Groq:**
   - Ve a https://console.groq.com/
   - Elimina la API key actual
   - Genera una nueva API key
   - Guárdala en tu archivo `.env` local

2. **Eliminar el commit problemático del historial:**
   ```bash
   # Ir al directorio del repositorio
   cd C:\Users\Benja\Downloads\des-software\sicc-api

   # Ver los últimos commits
   git log --oneline -5

   # Hacer un reset al commit antes del problemático
   git reset --soft HEAD~1

   # Asegurarte de que docker-compose.yml no tiene la API key
   git add docker-compose.yml .gitignore .env
   git commit -m "chore: remove exposed API key and add environment configuration"

   # Force push (CUIDADO: sobrescribe el historial remoto)
   git push origin main --force
   ```

3. **Configurar variables de entorno localmente:**
   ```bash
   # Copiar el archivo de ejemplo
   cp .env .env

   # Editar .env con tu nueva API key
   # GROQ_API_KEY=tu-nueva-api-key-aqui
   ```

### 4. **Configuración en Docker Compose**
El `docker-compose.yml` ya está configurado correctamente:
```yaml
environment:
  GROQ_API_KEY: ${GROQ_API_KEY:your-api-key-here}
```

### 5. **Uso correcto en el futuro**

**✅ CORRECTO:**
```bash
# Cargar variables de entorno desde .env
export $(cat .env | xargs)
docker-compose up -d
```

**❌ INCORRECTO:**
```yaml
# NO hardcodear API keys en docker-compose.yml
GROQ_API_KEY: sk-xxxxxxxxxxxxx  # ¡NUNCA HACER ESTO!
```

## 📋 Checklist de Seguridad

- [x] Agregar `.env` a `.gitignore`
- [x] Crear `.env.example` sin credenciales reales
- [ ] Rotar la API key expuesta en Groq
- [ ] Eliminar el commit problemático del historial
- [ ] Configurar `.env` local con la nueva API key
- [ ] Verificar que el push funcione correctamente

## 🔗 Referencias

- [GitHub Push Protection](https://docs.github.com/code-security/secret-scanning/working-with-secret-scanning-and-push-protection/working-with-push-protection-from-the-command-line)
- [Groq Console](https://console.groq.com/)
- [Git Rewrite History](https://git-scm.com/book/en/v2/Git-Tools-Rewriting-History)

