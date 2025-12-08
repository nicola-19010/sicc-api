# 🚀 GUÍA DE USO - AUTENTICACIÓN, SEGURIDAD Y DEPLOYMENT

## 📋 TABLA DE CONTENIDOS

1. [Desarrollo Local](#desarrollo-local)
2. [Testing](#testing)
3. [Deployment a Producción](#deployment-a-producción)
4. [GitHub Actions](#github-actions)
5. [Troubleshooting](#troubleshooting)

---

## 🔧 Desarrollo Local

### Opción 1: Con docker-compose (Recomendado)

```bash
# Iniciar Postgres + API
docker-compose up -d

# Ver logs
docker-compose logs -f sicc-api

# Acceder a la API
curl http://localhost:8080/actuator/health
# {"status":"UP"}

# Ver base de datos
psql -h localhost -p 5435 -U sicc_user -d sicc
```

### Opción 2: Postgres en Docker + IDE

```bash
# Iniciar solo Postgres
docker-compose up postgres -d

# En IntelliJ/Editor:
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Opción 3: PostgreSQL local

```bash
# Instalar Postgres 15
brew install postgresql@15  # macOS
# O usar Windows installer

# Iniciar servidor
pg_ctl -D /usr/local/var/postgres start

# Crear BD y usuario
createuser -d sicc_user
psql -U sicc_user -d postgres -c "ALTER USER sicc_user WITH PASSWORD 'sicc_password';"
createdb -U sicc_user sicc

# En tu IDE: mvn spring-boot:run
```

---

## 🧪 Testing

### Ejecutar Todos los Tests

```bash
# Con Maven
mvn test

# Tests específicos
mvn test -Dtest=AuthenticationControllerTest
mvn test -Dtest=JwtServiceTest

# Con cobertura
mvn test jacoco:report
```

### Tests HTTP Manuales

```bash
# 1. Registrar usuario
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstname": "Juan",
    "lastname": "Pérez",
    "email": "juan@example.com",
    "password": "password123"
  }'

# 2. Ver cookies
curl -i -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@example.com",
    "password": "password123"
  }'
# Respuesta incluye: Set-Cookie: access_token=...
# Respuesta incluye: Set-Cookie: refresh_token=...

# 3. Usar token en cookie
curl -b "access_token=TOKEN_VALUE" http://localhost:8080/api/users/me

# 4. Refresh token
curl -b "refresh_token=TOKEN_VALUE" \
  -X POST http://localhost:8080/api/auth/refresh

# 5. Logout
curl -b "access_token=TOKEN_VALUE" \
  -X POST http://localhost:8080/api/auth/logout
```

### Tests en IntelliJ HTTP Client

Abrir `http/auth.http` y usar:
```
POST /api/auth/register
POST /api/auth/login
GET /api/users/me
POST /api/auth/refresh
POST /api/auth/logout
```

---

## 🌍 Deployment a Producción

### Paso 1: Configurar GitHub Secrets

```bash
# Usando GitHub CLI
gh secret set SPRING_DATASOURCE_URL --body "jdbc:postgresql://db.example.com:5432/sicc"
gh secret set SPRING_DATASOURCE_USERNAME --body "prod_user"
gh secret set SPRING_DATASOURCE_PASSWORD --body "super_secure_password_256bits"
gh secret set SECURITY_JWT_SECRET_KEY --body "base64_encoded_secret_min_256bits"
gh secret set SECURITY_JWT_EXPIRATION_ACCESS --body "900000"
gh secret set SECURITY_JWT_EXPIRATION_REFRESH --body "2592000000"
gh secret set FRONTEND_URL --body "https://sicc.example.com"
gh secret set DOCKER_REGISTRY_USERNAME --body "your_docker_username"
gh secret set DOCKER_REGISTRY_PASSWORD --body "your_docker_token"
```

### Paso 2: Generar JWT Secret

```bash
# Generar un secreto seguro de 256 bits en base64
openssl rand -base64 32

# Ejemplo de salida:
# 9f8d7c6e5a4b3c2d1e0f9g8h7i6j5k4l3m2n1o0p==
```

### Paso 3: Build y Push Docker

```bash
# Build local
docker build -t sicc-api:1.0.0 .

# Login a Docker Registry
docker login -u your_username

# Tag y push
docker tag sicc-api:1.0.0 your_username/sicc-api:1.0.0
docker tag sicc-api:1.0.0 your_username/sicc-api:latest
docker push your_username/sicc-api:1.0.0
docker push your_username/sicc-api:latest
```

### Paso 4: Desplegar en Producción

#### Opción A: Docker Compose en Servidor

```bash
# En servidor producción:
git clone https://github.com/your-org/sicc.git
cd sicc-api

# Crear .env.prod
cat > .env.prod << EOF
SPRING_DATASOURCE_USERNAME=prod_user
SPRING_DATASOURCE_PASSWORD=$(echo $DB_PASSWORD)
SECURITY_JWT_SECRET_KEY=$(echo $JWT_SECRET)
SECURITY_JWT_EXPIRATION_ACCESS=900000
SECURITY_JWT_EXPIRATION_REFRESH=2592000000
FRONTEND_URL=https://sicc.example.com
EOF

# Desplegar
docker-compose -f docker-compose.prod.yml up -d
```

#### Opción B: Kubernetes

```bash
# Crear namespace
kubectl create namespace sicc-prod

# Crear secrets
kubectl create secret generic sicc-secrets \
  --from-literal=database-url='jdbc:postgresql://...' \
  --from-literal=database-username='prod_user' \
  --from-literal=database-password='...' \
  --from-literal=jwt-secret-key='...' \
  -n sicc-prod

# Desplegar
kubectl apply -f k8s/deployment.yml -n sicc-prod
kubectl apply -f k8s/service.yml -n sicc-prod
kubectl apply -f k8s/ingress.yml -n sicc-prod

# Verificar
kubectl get pods -n sicc-prod
kubectl logs -f deployment/sicc-api -n sicc-prod
```

#### Opción C: Heroku

```bash
# Login
heroku login

# Crear app
heroku create sicc-api

# Configurar secrets
heroku config:set SPRING_DATASOURCE_URL="jdbc:postgresql://..." --app sicc-api
heroku config:set SPRING_DATASOURCE_USERNAME="prod_user" --app sicc-api
heroku config:set SPRING_DATASOURCE_PASSWORD="..." --app sicc-api
heroku config:set SECURITY_JWT_SECRET_KEY="..." --app sicc-api
heroku config:set FRONTEND_URL="https://sicc.example.com" --app sicc-api

# Deploy
git push heroku main
```

---

## 🔄 GitHub Actions Workflow

Crear `.github/workflows/deploy.yml`:

```yaml
name: Build & Deploy SICC API

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest
    
    services:
      postgres:
        image: postgres:15
        env:
          POSTGRES_DB: sicc_test
          POSTGRES_USER: test_user
          POSTGRES_PASSWORD: test_password
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: 17
          distribution: temurin
          cache: maven

      - name: Run tests
        run: mvn clean test

      - name: Upload coverage
        uses: codecov/codecov-action@v3

  build:
    needs: test
    runs-on: ubuntu-latest
    environment: production

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: 17
          distribution: temurin
          cache: maven

      - name: Build with Maven
        run: mvn clean package -DskipTests

      - name: Login to Docker Hub
        uses: docker/login-action@v2
        with:
          username: ${{ secrets.DOCKER_REGISTRY_USERNAME }}
          password: ${{ secrets.DOCKER_REGISTRY_PASSWORD }}

      - name: Build and push Docker image
        uses: docker/build-push-action@v4
        with:
          context: .
          push: true
          tags: |
            ${{ secrets.DOCKER_REGISTRY_USERNAME }}/sicc-api:${{ github.sha }}
            ${{ secrets.DOCKER_REGISTRY_USERNAME }}/sicc-api:latest

      - name: Deploy to production
        if: github.ref == 'refs/heads/main'
        run: |
          # Aquí iría tu comando de deployment
          # kubectl rollout restart deployment/sicc-api -n sicc-prod
          echo "Deployment script here"
```

---

## 🔍 Troubleshooting

### Error: "No suitable driver found for jdbc:postgresql"

```bash
# Solución: Agregar dependencia PostgreSQL
mvn dependency:tree | grep postgresql

# Si no está, agregar a pom.xml y recompilar
```

### Error: "Connection refused" en Postgres

```bash
# Verificar que Postgres está corriendo
docker ps | grep postgres

# Iniciar si está parado
docker-compose up postgres -d

# O en local
pg_ctl start
```

### Error: "401 Unauthorized" en requests

```bash
# Opción 1: Incluir cookies
curl -b "access_token=TOKEN" http://localhost:8080/api/users/me

# Opción 2: Incluir header Authorization
curl -H "Authorization: Bearer TOKEN" http://localhost:8080/api/users/me
```

### Error: "CSRF token missing"

```
# Solución: CSRF está deshabilitado en API
# Si ves este error, verificar SecurityConfig
```

### Logs no aparecen en /var/log/sicc-api/

```bash
# Verificar permisos en Docker
docker exec sicc-api ls -la /var/log/sicc-api/

# Verificar que el volumen está montado
docker inspect sicc-api | grep -A 5 Mounts
```

---

## ✅ Checklist Pre-Production

- [ ] Todos los tests pasan (`mvn test`)
- [ ] Build exitoso (`mvn clean package`)
- [ ] Docker image builds sin errores
- [ ] GitHub Secrets configurados
- [ ] JWT Secret generado y almacenado de forma segura
- [ ] Database URL apunta a servidor de producción
- [ ] FRONTEND_URL configurada correctamente
- [ ] CORS solo permite dominio frontend
- [ ] Logging configurado para producción
- [ ] Healthcheck funcionando
- [ ] Base de datos creada en servidor
- [ ] Migraciones Flyway ejecutadas
- [ ] Certificado HTTPS configurado
- [ ] Firewall permite puerto 443
- [ ] Backups de BD configurados

---

## 📞 Recursos

- [Spring Security Docs](https://spring.io/projects/spring-security)
- [JWT.io](https://jwt.io/)
- [Docker Docs](https://docs.docker.com/)
- [Kubernetes Docs](https://kubernetes.io/docs/)
- [GitHub Actions](https://docs.github.com/en/actions)

---

**Deployment listo para producción.** 🚀

