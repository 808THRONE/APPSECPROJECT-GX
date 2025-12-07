# Security Testing

## Overview
Comprehensive security testing suite for SecureGate IAM Portal.

## Tools Used
- **OWASP ZAP** - Automated vulnerability scanning
- **Burp Suite** - Manual penetration testing
- **sqlmap** - SQL injection testing
- **testssl.sh** - TLS configuration testing
- **Trivy** - Container CVE scanning
- **OWASP Dependency-Check** - Java library vulnerability scanning

## Test Coverage

### 1. Algorithm Confusion Attack
```bash
./security-tests.sh algorithm-confusion
```
**Expected**: JWT with `alg: none` rejected

### 2. Token Replay Attack
```bash
./security-tests.sh token-replay
```
**Expected**: Duplicate jti rejected (Redis tracking)

### 3. OAuth CSRF Test
```bash
./security-tests.sh oauth-csrf
```
**Expected**: Invalid state parameter fails validation

### 4. SQL Injection
```bash
sqlmap -u "https://api.yourdomain.me/endpoint" --batch
```
**Expected**: Zero vulnerabilities found

### 5. XSS Testing
```bash
zap-cli quick-scan https://www.yourdomain.me
```
**Expected**: Zero stored/reflected XSS

### 6. TLS Configuration
```bash
testssl.sh https://iam.yourdomain.me
```
**Expected**: TLS 1.3 only, HSTS preload active

### 7. Container Scanning
```bash
trivy image securegate-iam:latest
```
**Expected**: Zero HIGH/CRITICAL CVEs

## Implementation Status
⏳ **Pending Implementation** - Test scripts created, awaiting backend deployment
