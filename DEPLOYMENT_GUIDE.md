# 🚀 DEPLOYMENT - GITHUB SECRETS Y VARIABLES DE ENTORNO

## 📋 GitHub Secrets Necesarios

En tu repositorio GitHub, configura estos secrets:

### Para Producción (Environment: production)

```yaml
SPRING_DATASOURCE_URL
├─ Valor: jdbc:postgresql://HOSTNAME:5432/sicc
├─ Ejemplo: jdbc:postgresql://db.example.com:5432/sicc

SPRING_DATASOURCE_USERNAME
├─ Valor: usuario_postgres
├─ Ejemplo: sicc_prod_user

SPRING_DATASOURCE_PASSWORD
├─ Valor: contraseña_super_segura
├─ Requerimiento: Mínimo 32 caracteres

SECURITY_JWT_SECRET_KEY
├─ Valor: secreto_jwt_base64_muy_largo
├─ Requerimiento: Base64 encoded, mínimo 256 bits
├─ Generación: `openssl rand -base64 32`

SECURITY_JWT_EXPIRATION_ACCESS
├─ Valor: 900000
├─ Descripción: 15 minutos en milisegundos

SECURITY_JWT_EXPIRATION_REFRESH
├─ Valor: 2592000000
├─ Descripción: 30 días en milisegundos

FRONTEND_URL
├─ Valor: https://sicc.example.com
├─ Descripción: URL del frontend para CORS

DOCKER_REGISTRY_USERNAME
├─ Valor: tu_usuario_dockerhub
├─ Descripción: Para push de imágenes

DOCKER_REGISTRY_PASSWORD
├─ Valor: tu_token_dockerhub
├─ Descripción: Token de autenticación DockerHub
```

---

## 🛠️ Cómo Crear GitHub Secrets

### Opción 1: Desde la UI de GitHub

1. Ve a tu repositorio
2. **Settings** → **Secrets and variables** → **Actions**
3. Click **New repository secret**
4. Ingresa Name y Value
5. Click **Add secret**

### Opción 2: Usando GitHub CLI

```bash
# Instalar GitHub CLI
brew install gh  # macOS
winget install GitHub.cli  # Windows
apt-get install gh  # Linux

# Autenticarse
gh auth login

# Crear secrets
gh secret set SPRING_DATASOURCE_URL --body "jdbc:postgresql://..."
gh secret set SPRING_DATASOURCE_USERNAME --body "sicc_prod_user"
gh secret set SPRING_DATASOURCE_PASSWORD --body "super_secure_password"
gh secret set SECURITY_JWT_SECRET_KEY --body "base64_encoded_secret"
gh secret set FRONTEND_URL --body "https://sicc.example.com"
```

---

## 📝 Variables de Entorno en Docker

### Dockerfile

```dockerfile
FROM openjdk:17-slim

# Copiar JAR
COPY target/sicc-api-*.jar app.jar

# Variables de entorno (pasadas en runtime)
ENV SPRING_PROFILES_ACTIVE=prod
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/sicc
ENV SPRING_DATASOURCE_USERNAME=sicc_user
ENV SPRING_DATASOURCE_PASSWORD=secret_password
ENV SECURITY_JWT_SECRET_KEY=base64_secret_key
ENV SECURITY_JWT_EXPIRATION_ACCESS=900000
ENV SECURITY_JWT_EXPIRATION_REFRESH=2592000000
ENV FRONTEND_URL=https://sicc.example.com

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### docker-compose.yml

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: sicc
      POSTGRES_USER: ${SPRING_DATASOURCE_USERNAME}
      POSTGRES_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  sicc-api:
    build: .
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/sicc
      SPRING_DATASOURCE_USERNAME: ${SPRING_DATASOURCE_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
      SECURITY_JWT_SECRET_KEY: ${SECURITY_JWT_SECRET_KEY}
      SECURITY_JWT_EXPIRATION_ACCESS: ${SECURITY_JWT_EXPIRATION_ACCESS}
      SECURITY_JWT_EXPIRATION_REFRESH: ${SECURITY_JWT_EXPIRATION_REFRESH}
      FRONTEND_URL: ${FRONTEND_URL}
    ports:
      - "8080:8080"
    depends_on:
      - postgres

volumes:
  postgres_data:
```

### .env (para desarrollo LOCAL con docker-compose)

```bash
# NO versionear este archivo en producción
SPRING_DATASOURCE_USERNAME=sicc_dev_user
SPRING_DATASOURCE_PASSWORD=dev_password_123
SECURITY_JWT_SECRET_KEY=dev-secret-key-do-not-use-in-production-1234567890-min-32-chars
SECURITY_JWT_EXPIRATION_ACCESS=900000
SECURITY_JWT_EXPIRATION_REFRESH=2592000000
FRONTEND_URL=http://localhost:4200
```

---

## 🔐 GitHub Actions Workflow

### .github/workflows/deploy.yml

```yaml
name: Deploy to Production

on:
  push:
    branches:
      - main

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    environment: production

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Build with Maven
        run: mvn clean package -DskipTests

      - name: Build Docker image
        run: |
          docker build -t sicc-api:${{ github.sha }} .
          docker tag sicc-api:${{ github.sha }} sicc-api:latest

      - name: Push to Docker Registry
        run: |
          echo ${{ secrets.DOCKER_REGISTRY_PASSWORD }} | docker login -u ${{ secrets.DOCKER_REGISTRY_USERNAME }} --password-stdin
          docker push sicc-api:${{ github.sha }}
          docker push sicc-api:latest

      - name: Deploy to Production
        run: |
          # Aquí iría tu comando de deployment
          # Ejemplo: kubectl apply -f k8s/deployment.yml
          echo "Deploying SICC API..."
```

---

## 🐳 Kubernetes Deployment

### k8s/deployment.yml

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: sicc-api
  namespace: production

spec:
  replicas: 2
  selector:
    matchLabels:
      app: sicc-api

  template:
    metadata:
      labels:
        app: sicc-api
    spec:
      containers:
        - name: sicc-api
          image: sicc-api:latest
          ports:
            - containerPort: 8080

          # Variables de entorno desde Secrets
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            
            - name: SPRING_DATASOURCE_URL
              valueFrom:
                secretKeyRef:
                  name: sicc-secrets
                  key: database-url
            
            - name: SPRING_DATASOURCE_USERNAME
              valueFrom:
                secretKeyRef:
                  name: sicc-secrets
                  key: database-username
            
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: sicc-secrets
                  key: database-password
            
            - name: SECURITY_JWT_SECRET_KEY
              valueFrom:
                secretKeyRef:
                  name: sicc-secrets
                  key: jwt-secret-key
            
            - name: SECURITY_JWT_EXPIRATION_ACCESS
              value: "900000"
            
            - name: SECURITY_JWT_EXPIRATION_REFRESH
              value: "2592000000"
            
            - name: FRONTEND_URL
              value: "https://sicc.example.com"

          # Health checks
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10

          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5

          # Recursos
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "500m"
```

### k8s/secrets.yml

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: sicc-secrets
  namespace: production

type: Opaque

data:
  database-url: base64_encoded_jdbc_url
  database-username: base64_encoded_username
  database-password: base64_encoded_password
  jwt-secret-key: base64_encoded_jwt_secret
```

**Para crear los valores en base64:**

```bash
echo -n "jdbc:postgresql://postgres:5432/sicc" | base64
echo -n "sicc_user" | base64
echo -n "your_secure_password" | base64
echo -n "your_jwt_secret_key" | base64
```

---

## ✅ Checklist de Deployment

- [ ] Todos los secrets están en GitHub Secrets
- [ ] application-prod.yml NO tiene valores hardcodeados
- [ ] Dockerfile copia secretos desde variables de entorno
- [ ] docker-compose.yml usa .env
- [ ] JWT_SECRET_KEY es base64 encoded y tiene 256+ bits
- [ ] FRONTEND_URL apunta al dominio correcto
- [ ] Database URL apunta a servidor de producción
- [ ] Logs se escriben a /var/log/sicc-api/
- [ ] Actuator está expuesto solo a endpoints de salud
- [ ] CORS solo permite frontend domain

---

## 🔐 Seguridad en Producción

### ❌ NO HAGAS

```yaml
# ❌ MAL - Secret en código
spring:
  datasource:
    password: my_secret_password

# ❌ MAL - Hardcoded en Docker
ENV JWT_SECRET = "my_secret_key"

# ❌ MAL - Versionado en Git
# .env con secretos reales
```

### ✅ SÍ HACES

```yaml
# ✅ BIEN - Variable de entorno
spring:
  datasource:
    password: ${SPRING_DATASOURCE_PASSWORD}

# ✅ BIEN - Desde Docker secrets
docker secret create jwt_secret -

# ✅ BIEN - .env en .gitignore
echo ".env" >> .gitignore
```

---

## 📞 Referencias

- [GitHub Secrets Documentation](https://docs.github.com/en/actions/security-guides/encrypted-secrets)
- [Docker Secrets](https://docs.docker.com/engine/swarm/secrets/)
- [Kubernetes Secrets](https://kubernetes.io/docs/concepts/configuration/secret/)
- [Spring Cloud Config](https://spring.io/projects/spring-cloud-config)

---

*Guía completa para deployment seguro en producción.* 🚀

