# API Gateway & ABAC Engine

## Overview
REST API gateway with Attribute-Based Access Control policy engine.

## Technology Stack
- WildFly 38.0.1.Final Preview
- Jakarta RESTful Web Services 4.0
- Jakarta WebSocket 2.2
- Apache Artemis 2.42.0

## Structure
```
src/main/java/com/securegate/
├── api/            # REST API endpoints
├── abac/           # ABAC policy engine
└── audit/          # WebSocket audit streaming
```

## Implementation Status
⏳ **Pending Implementation** - Placeholder structure created

## Next Steps
1. Implement JWT/PASETO validation filter
2. Build ABAC policy engine
3. Create WebSocket audit endpoint
4. Configure mTLS with IAM service
