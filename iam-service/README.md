# IAM Service (OAuth 2.1 + PASETO)

## Overview
Identity and Access Management service implementing OAuth 2.1 with PKCE and PASETO v4 tokens.

## Technology Stack
- WildFly 38.0.1.Final Preview
- Jakarta EE 11
- PostgreSQL 16.1
- Redis 7.2.x

## Structure
```
src/main/java/com/securegate/
├── oauth/          # OAuth 2.1 authorization server
├── tokens/         # JWT/PASETO token services
├── mfa/            # TOTP 2FA enrollment
├── user/           # User management
└── session/        # Session management
```

## Implementation Status
⏳ **Pending Implementation** - Placeholder structure created

## Next Steps
1. Configure WildFly standalone.xml
2. Implement OAuth 2.1 endpoints
3. Implement PASETO v4 token service
4. Set up PostgreSQL user realm
5. Configure Redis session store
