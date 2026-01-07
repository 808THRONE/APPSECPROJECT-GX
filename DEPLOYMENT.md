# SecureGate IAM Portal - Deployment Guide

Complete guide for deploying SecureGate to production with your domain **mortadha.me**.

---

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Local Development](#local-development)
3. [Production Deployment](#production-deployment)
4. [Domain & DNS Configuration](#domain--dns-configuration)
5. [TLS/SSL Setup](#tlsssl-setup)
6. [Environment Variables](#environment-variables)
7. [Security Checklist](#security-checklist)

---

## Prerequisites

### Required Software
- **Docker** 24.0+ and **Docker Compose** v2
- **Java JDK 17+** (for building)
- **Maven 3.9+**
- **Node.js 18+** and npm
- **Domain**: mortadha.me (or your domain)
- **VPS/Server**: Ubuntu 22.04 LTS recommended (2+ vCPU, 4GB+ RAM)

### Domain Structure
```
mortadha.me          → PWA Frontend (Port 443)
iam.mortadha.me      → OAuth 2.1 IAM Service (Port 443)
api.mortadha.me      → REST API Gateway (Port 443)
```

---

## Local Development

### 1. Build the Projects

```bash
# Build IAM Service
cd iam-service
mvn clean package -DskipTests

# Build API Gateway
cd ../api-gateway
mvn clean package -DskipTests
```

### 2. Start with Docker Compose

```bash
# Navigate to infrastructure
cd infrastructure

# Copy and configure environment (optional - defaults work for local dev)
cp .env.example .env

# Start all services
docker-compose up --build

# Or run in detached mode
docker-compose up -d --build
```

### 3. Access Services

| Service | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| IAM Service | http://localhost:8080/iam |
| API Gateway | http://localhost:8081/api |
| MinIO Console | http://localhost:9001 |

### 4. Test Login

1. Open http://localhost:5173
2. Click "Login with SSO"
3. Login with: `admin@mortadha.me` / `Admin123!`
4. You'll be redirected to the dashboard

---

## Production Deployment

### 1. Server Setup

```bash
# SSH into your server
ssh root@your-server-ip

# Update system
apt update && apt upgrade -y

# Install Docker
curl -fsSL https://get.docker.com | sh
systemctl enable docker
systemctl start docker

# Install Docker Compose
apt install docker-compose-plugin -y

# Create app directory
mkdir -p /opt/securegate
cd /opt/securegate
```

### 2. Clone Repository

```bash
git clone https://github.com/your-repo/securegate.git .
```

### 3. Configure Environment

Create `/opt/securegate/infrastructure/.env`:

```bash
# Production Environment Variables
DOMAIN=mortadha.me

# PostgreSQL (use a strong password!)
POSTGRES_PASSWORD=YourSuperSecurePassword123!

# MinIO
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=YourMinioPassword123!

# TLS/SSL
ACME_EMAIL=admin@mortadha.me
```

### 4. Build and Deploy

```bash
# Build the WARs
cd /opt/securegate/iam-service
mvn clean package -DskipTests

cd /opt/securegate/api-gateway
mvn clean package -DskipTests

# Deploy
cd /opt/securegate/infrastructure
docker-compose -f docker-compose.prod.yml up -d --build
```

---

## Domain & DNS Configuration

### DNS Records for mortadha.me

Configure these DNS records at your domain registrar:

| Type | Name | Value | TTL |
|------|------|-------|-----|
| A | @ | `YOUR_SERVER_IP` | 300 |
| A | iam | `YOUR_SERVER_IP` | 300 |
| A | api | `YOUR_SERVER_IP` | 300 |
| CNAME | www | mortadha.me | 300 |

### Verify DNS Propagation

```bash
# Check DNS resolution
dig mortadha.me +short
dig iam.mortadha.me +short
dig api.mortadha.me +short
```

---

## TLS/SSL Setup

### Production Docker Compose with Traefik

Create `infrastructure/docker-compose.prod.yml`:

```yaml
version: '3.8'

services:
  traefik:
    image: traefik:3.0
    container_name: securegate-traefik
    command:
      - "--api.dashboard=true"
      - "--providers.docker=true"
      - "--providers.docker.exposedbydefault=false"
      - "--entrypoints.web.address=:80"
      - "--entrypoints.websecure.address=:443"
      - "--entrypoints.web.http.redirections.entrypoint.to=websecure"
      - "--certificatesresolvers.letsencrypt.acme.email=${ACME_EMAIL}"
      - "--certificatesresolvers.letsencrypt.acme.storage=/letsencrypt/acme.json"
      - "--certificatesresolvers.letsencrypt.acme.httpchallenge.entrypoint=web"
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - letsencrypt:/letsencrypt
    networks:
      - securegate-network

  postgres:
    image: postgres:16.1
    container_name: securegate-postgres
    environment:
      POSTGRES_DB: securegate
      POSTGRES_USER: securegate
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ../iam-service/src/main/resources/schema.sql:/docker-entrypoint-initdb.d/init.sql
    networks:
      - securegate-network
    restart: unless-stopped

  redis:
    image: redis:7.2
    container_name: securegate-redis
    command: redis-server --requirepass ${REDIS_PASSWORD:-redis123}
    volumes:
      - redis-data:/data
    networks:
      - securegate-network
    restart: unless-stopped

  iam-service:
    build: ../iam-service
    container_name: securegate-iam
    environment:
      - POSTGRES_HOST=postgres
      - POSTGRES_PASSWORD=${POSTGRES_PASSWORD}
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.iam.rule=Host(`iam.${DOMAIN}`)"
      - "traefik.http.routers.iam.entrypoints=websecure"
      - "traefik.http.routers.iam.tls.certresolver=letsencrypt"
      - "traefik.http.services.iam.loadbalancer.server.port=8080"
    depends_on:
      - postgres
      - redis
    networks:
      - securegate-network
    restart: unless-stopped

  api-gateway:
    build: ../api-gateway
    container_name: securegate-gateway
    environment:
      - IAM_JWKS_URL=http://iam-service:8080/iam/jwk
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.api.rule=Host(`api.${DOMAIN}`)"
      - "traefik.http.routers.api.entrypoints=websecure"
      - "traefik.http.routers.api.tls.certresolver=letsencrypt"
      - "traefik.http.services.api.loadbalancer.server.port=8080"
    depends_on:
      - iam-service
    networks:
      - securegate-network
    restart: unless-stopped

  pwa-frontend:
    image: nginx:alpine
    container_name: securegate-frontend
    volumes:
      - ../dist:/usr/share/nginx/html:ro
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.frontend.rule=Host(`${DOMAIN}`) || Host(`www.${DOMAIN}`)"
      - "traefik.http.routers.frontend.entrypoints=websecure"
      - "traefik.http.routers.frontend.tls.certresolver=letsencrypt"
      - "traefik.http.services.frontend.loadbalancer.server.port=80"
    networks:
      - securegate-network
    restart: unless-stopped

volumes:
  postgres-data:
  redis-data:
  letsencrypt:

networks:
  securegate-network:
    driver: bridge
```

### Nginx Configuration for Frontend

Create `infrastructure/nginx.conf`:

```nginx
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # SPA routing
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Cache static assets
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # API proxy
    location /api/ {
        proxy_pass https://api.mortadha.me/api/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # IAM proxy
    location /iam/ {
        proxy_pass https://iam.mortadha.me/iam/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### Build Frontend for Production

```bash
# Create production environment file
cat > .env.production << EOF
VITE_IAM_URL=https://iam.mortadha.me/iam
VITE_API_URL=https://api.mortadha.me/api
VITE_OAUTH_CLIENT_ID=demo-client
VITE_OAUTH_REDIRECT_URI=https://mortadha.me/callback
VITE_WS_URL=wss://api.mortadha.me/api/audit
EOF

# Build frontend
npm install
npm run build
```

---

## Environment Variables

### Full Reference

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `DOMAIN` | Your domain name | - | Yes (prod) |
| `POSTGRES_PASSWORD` | PostgreSQL password | - | Yes |
| `MINIO_ROOT_USER` | MinIO admin username | minioadmin | No |
| `MINIO_ROOT_PASSWORD` | MinIO admin password | - | Yes (prod) |
| `REDIS_PASSWORD` | Redis password | - | Yes (prod) |
| `ACME_EMAIL` | Let's Encrypt email | - | Yes (prod) |
| `VITE_IAM_URL` | IAM service URL | /iam | No |
| `VITE_API_URL` | API Gateway URL | /api | No |
| `VITE_OAUTH_CLIENT_ID` | OAuth client ID | demo-client | No |

---

## Security Checklist

### Pre-Deployment

- [ ] Change all default passwords
- [ ] Generate new demo user with strong password
- [ ] Update OAuth client secret in database
- [ ] Configure firewall (allow only 80, 443, 22)
- [ ] Disable root SSH login
- [ ] Set up fail2ban

### Post-Deployment

- [ ] Verify HTTPS works for all subdomains
- [ ] Test OAuth login flow
- [ ] Check HSTS header is present
- [ ] Verify CSP headers
- [ ] Run OWASP ZAP scan
- [ ] Set up monitoring (Prometheus/Grafana)
- [ ] Configure log rotation
- [ ] Set up automated backups for PostgreSQL

### Firewall Setup

```bash
# Configure UFW
ufw default deny incoming
ufw default allow outgoing
ufw allow ssh
ufw allow http
ufw allow https
ufw enable
```

---

## Troubleshooting

### Common Issues

**1. WAR files not building**
```bash
# Ensure Java 17 is installed
java -version
# Should show OpenJDK 17.x
```

**2. Docker containers not starting**
```bash
# Check logs
docker-compose logs -f iam-service
docker-compose logs -f api-gateway
```

**3. SSL certificate issues**
```bash
# Check Traefik logs
docker-compose logs -f traefik
# Ensure DNS is properly configured before starting
```

**4. Database connection issues**
```bash
# Connect to PostgreSQL
docker exec -it securegate-postgres psql -U securegate -d securegate
# Check if tables exist
\dt
```

---

## Quick Commands Reference

```bash
# Start all services
docker-compose up -d

# Stop all services
docker-compose down

# View logs
docker-compose logs -f

# Rebuild a specific service
docker-compose up -d --build iam-service

# Reset database
docker-compose down -v
docker-compose up -d

# Check service health
docker-compose ps
```

---

**Last Updated**: January 2026  
**Domain**: mortadha.me  
**Subdomains**: iam.mortadha.me, api.mortadha.me
