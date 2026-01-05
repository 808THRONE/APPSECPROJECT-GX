# Production-Ready IAM Service

Clean implementation based on Phoenix IAM architecture.

## Features

- ✅ OAuth 2.1 Authorization Code Flow with PKCE
- ✅ EdDSA (Ed25519) JWT Signing with Key Rotation
- ✅ Argon2id Password Hashing
- ✅ ChaCha20-Poly1305 Authorization Code Encryption
- ✅ Multi-tenant Support
- ✅ User Consent Management
- ✅ Redis Session Storage
- ✅ H2 or PostgreSQL Database Support

## Technology Stack

- **Runtime**: WildFly 38.0.1.Final Preview (Jakarta EE 11)
- **Java**: OpenJDK 21
- **Database**: PostgreSQL 16+ or H2 (embedded)
- **Cache**: Redis 7.2+
- **Security**: Argon2id, EdDSA, ChaCha20-Poly1305

## Project Structure

```
src/main/java/me/mortadha/iam/
├── core/           # Base entities and DAOs
├── entities/       # Domain entities (Tenant, Identity, Grant)
├── security/       # Security utilities (Argon2, JWT, AuthCode)
├── controllers/    # Business logic (Repository, IdentityStore)
├── rest/           # JAX-RS endpoints
└── config/         # CDI producers and configuration

src/main/resources/
├── META-INF/
│   ├── persistence.xml
│   └── microprofile-config.properties
└── schema.sql      # Database initialization
```

## Quick Start

### Development (H2 Database)

```bash
# Build
mvn clean package

# Deploy to WildFly
cp target/iam-service.war $WILDFLY_HOME/standalone/deployments/

# Access at
http://localhost:8080/iam/
```

### Production (PostgreSQL)

See `DEPLOYMENT.md` for full instructions.

## Endpoints

- `GET  /iam/authorize` - OAuth authorization endpoint
- `POST /iam/token` - Token exchange endpoint
- `GET  /iam/jwk` - Public key endpoint (JWKS)
- `POST /iam/login` - User authentication
- `PATCH /iam/consent` - User consent approval

## Configuration

Edit `src/main/resources/META-INF/microprofile-config.properties`:

```properties
# JWT Settings
jwt.issuer=urn:mortadha.me:iam
jwt.audiences=urn:mortadha.me:api
jwt.lifetime.duration=1020

# Key Rotation
key.pair.lifetime.duration=10800
key.pair.cache.size=3

# Argon2 Parameters
argon2.iterations=23
argon2.memory=97579
argon2.threads=2
```

## Security Features

1. **Password Security**: Argon2id with high-security parameters
2. **JWT Security**: Ed25519 signing with automatic key rotation
3. **OAuth Security**: PKCE mandatory, authorization code encryption
4. **Session Security**: Redis-backed with configurable TTL
5. **Transport Security**: TLS 1.3 only in production

## License

Educational/Internal Use
