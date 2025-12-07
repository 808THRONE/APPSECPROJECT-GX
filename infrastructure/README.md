# Infrastructure Services

## Overview
Docker Compose setup for SecureGate infrastructure.

## Services
- **PostgreSQL 16.1** - Primary database with pgcrypto
- **Redis 7.2 Cluster** - Session storage and caching (6 nodes)
- **MinIO** - S3-compatible object storage for stego cover images
- **HashiCorp Vault** - Secrets management
- **Traefik** - Reverse proxy with TLS 1.3

## Quick Start

```bash
# Set environment variables
cp .env.example .env
# Edit .env with your passwords

# Start infrastructure
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f
```

## Endpoints
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- MinIO: `localhost:9000` (API), `localhost:9001` (Console)
- Vault: `localhost:8200`
- Traefik Dashboard: `localhost:8080`

## Implementation Status
⏳ **Pending Complete Configuration** - Basic structure created
