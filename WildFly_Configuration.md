# WildFly Security Configuration Guide

This document provides the complete WildFly configuration setup that was implemented for the Phoenix IAM security lab.

## Overview

- **WildFly Version**: 38.0.1.Final
- **Java Version**: OpenJDK 21
- **SSL/TLS**: Let's Encrypt certificates with TLS 1.3 onl### 3. Security Headers Test

```bash
# Test security headers on all domains
curl -I https://yourdomain.com
curl -I https://iam.yourdomain.com
curl -I https://api.yourdomain.com**Security**: HSTS, security headers, and proper cipher suites
- **DNS**: Subdomain configuration for multi-domain architecture

## Initial Setup

### 1. System Preparation

```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Java 21
sudo apt install openjdk-21-jdk -y

# Verify Java installation
java -version
```

### 2. WildFly Installation

```bash
# Create wildfly user
sudo useradd -r -g daemon -d /opt/wildfly -s /bin/bash wildfly

# Download and extract WildFly 38.0.1.Final
cd /tmp
wget https://github.com/wildfly/wildfly/releases/download/38.0.1.Final/wildfly-38.0.1.Final.tar.gz
sudo tar xf wildfly-38.0.1.Final.tar.gz -C /opt/
sudo mv /opt/wildfly-38.0.1.Final /opt/wildfly
sudo chown -R wildfly:daemon /opt/wildfly
```

### 3. WildFly Service Configuration

```bash
# Create systemd service file
sudo tee /etc/systemd/system/wildfly.service > /dev/null << 'EOF'
[Unit]
Description=The WildFly Application Server
After=syslog.target network.target
Before=httpd.service

[Service]
Environment=LAUNCH_JBOSS_IN_BACKGROUND=1
EnvironmentFile=-/etc/wildfly/wildfly.conf
User=wildfly
Group=daemon
LimitNOFILE=102642
PIDFile=/var/run/wildfly/wildfly.pid
ExecStart=/opt/wildfly/bin/standalone.sh -c standalone-full.xml -b 0.0.0.0
StandardOutput=null

[Install]
WantedBy=multi-user.target
EOF

# Create configuration directory
sudo mkdir -p /etc/wildfly

# Enable and start service
sudo systemctl daemon-reload
sudo systemctl enable wildfly
sudo systemctl start wildfly
```

## Security Configuration

### 1. Management User Creation

```bash
# Create management user (replace with secure credentials)
sudo -u wildfly /opt/wildfly/bin/add-user.sh

# Follow prompts:
# - Type: Management User
# - Username: [SECURE_ADMIN_USERNAME]
# - Password: [SECURE_ADMIN_PASSWORD]
# - Groups: (leave empty)
# - Remote access: yes
```

### 2. SSL Certificate Setup

```bash
# Install Certbot
sudo apt install certbot -y

# Obtain Let's Encrypt certificate for main domain and subdomains
sudo certbot certonly --standalone -d yourdomain.com -d iam.yourdomain.com -d api.yourdomain.com -d www.yourdomain.com

# Create keystore from Let's Encrypt certificates
sudo openssl pkcs12 -export \
    -in /etc/letsencrypt/live/yourdomain.com/fullchain.pem \
    -inkey /etc/letsencrypt/live/yourdomain.com/privkey.pem \
    -out /opt/wildfly/standalone/configuration/wildfly.p12 \
    -name wildfly \
    -password pass:YOUR_KEYSTORE_PASSWORD

# Convert to JKS format
sudo keytool -importkeystore \
    -deststorepass YOUR_KEYSTORE_PASSWORD \
    -destkeypass YOUR_KEYSTORE_PASSWORD \
    -destkeystore /opt/wildfly/standalone/configuration/wildfly.keystore \
    -srckeystore /opt/wildfly/standalone/configuration/wildfly.p12 \
    -srcstoretype PKCS12 \
    -srcstorepass YOUR_KEYSTORE_PASSWORD \
    -alias wildfly

# Set proper ownership
sudo chown wildfly:daemon /opt/wildfly/standalone/configuration/wildfly.*
```

### 3. WildFly CLI Configuration

Access the CLI and apply security configurations:

```bash
# Connect to CLI
sudo -u wildfly /opt/wildfly/bin/jboss-cli.sh --connect
```

#### SSL Context Configuration

```cli
# Create SSL context with TLS 1.3 only
/subsystem=elytron/key-store=wildfly-keystore:add(path=wildfly.keystore,relative-to=jboss.server.config.dir,credential-reference={clear-text="YOUR_KEYSTORE_PASSWORD"},type=JKS)

/subsystem=elytron/key-manager=wildfly-key-manager:add(key-store=wildfly-keystore,credential-reference={clear-text="YOUR_KEYSTORE_PASSWORD"})

/subsystem=elytron/server-ssl-context=wildfly-ssl-context:add(key-manager=wildfly-key-manager,protocols=["TLSv1.3"])
```

#### HTTPS Listener Configuration

```cli
# Remove default HTTPS listener
/subsystem=undertow/server=default-server/https-listener=https:remove

# Add new HTTPS listener with SSL context
/subsystem=undertow/server=default-server/https-listener=https:add(socket-binding=https,ssl-context=wildfly-ssl-context,enable-http2=true)
```

#### TLS 1.3 Cipher Suites Configuration

```cli
# Configure TLS 1.3 cipher suites only
/subsystem=elytron/server-ssl-context=wildfly-ssl-context:write-attribute(name=cipher-suite-filter,value="TLS_AES_256_GCM_SHA384:TLS_CHACHA20_POLY1305_SHA256:TLS_AES_128_GCM_SHA256")
```

#### Security Headers Configuration

```cli
# Add security headers
/subsystem=undertow/configuration=filter/response-header=hsts-header:add(header-name="Strict-Transport-Security",header-value="max-age=63072000; includeSubDomains; preload")

/subsystem=undertow/configuration=filter/response-header=xframe-header:add(header-name="X-Frame-Options",header-value="DENY")

/subsystem=undertow/configuration=filter/response-header=xcontent-header:add(header-name="X-Content-Type-Options",header-value="nosniff")

/subsystem=undertow/configuration=filter/response-header=xss-header:add(header-name="X-XSS-Protection",header-value="1; mode=block")

/subsystem=undertow/configuration=filter/response-header=csp-header:add(header-name="Content-Security-Policy",header-value="default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none';")

# Apply headers to default host
/subsystem=undertow/server=default-server/host=default-host/filter-ref=hsts-header:add
/subsystem=undertow/server=default-server/host=default-host/filter-ref=xframe-header:add
/subsystem=undertow/server=default-server/host=default-host/filter-ref=xcontent-header:add
/subsystem=undertow/server=default-server/host=default-host/filter-ref=xss-header:add
/subsystem=undertow/server=default-server/host=default-host/filter-ref=csp-header:add
```

#### HTTP to HTTPS Redirect

```cli
# Configure HTTP to HTTPS redirect
/subsystem=undertow/server=default-server/http-listener=default:write-attribute(name=redirect-socket,value="https")
```

#### Apply Configuration

```cli
# Reload configuration
:reload
```

### 4. DNS Configuration

Configure DNS records for subdomain architecture:

```bash
# Add DNS A records for subdomains (replace with your actual IP)
# These should be configured in your DNS provider's control panel

# Main domain
yourdomain.com.          IN A    YOUR_SERVER_IP

# Subdomain for IAM service
iam.yourdomain.com.      IN A    YOUR_SERVER_IP

# Subdomain for API service  
api.yourdomain.com.      IN A    YOUR_SERVER_IP

# WWW subdomain (optional)
www.yourdomain.com.      IN A    YOUR_SERVER_IP

# CAA Records for Let's Encrypt certificate authority authorization
yourdomain.com.          IN CAA  0 issue "letsencrypt.org"
yourdomain.com.          IN CAA  0 issuewild "letsencrypt.org"
yourdomain.com.          IN CAA  0 iodef "mailto:admin@yourdomain.com"

# Verify DNS propagation
dig yourdomain.com
dig iam.yourdomain.com
dig api.yourdomain.com

# Verify CAA records
dig yourdomain.com CAA
```

### 5. Firewall Configuration

```bash
# Configure UFW firewall
sudo ufw allow 22/tcp      # SSH
sudo ufw allow 80/tcp      # HTTP (for redirect)
sudo ufw allow 443/tcp     # HTTPS
# Note: Management port 9990 is NOT exposed externally for security
sudo ufw --force enable
```

### 6. Certificate Auto-Renewal

```bash
# Create renewal script
sudo tee /opt/wildfly/scripts/renew-cert.sh > /dev/null << 'EOF'
#!/bin/bash

# Renew certificate
certbot renew --quiet

# Convert renewed certificate
openssl pkcs12 -export \
    -in /etc/letsencrypt/live/yourdomain.com/fullchain.pem \
    -inkey /etc/letsencrypt/live/yourdomain.com/privkey.pem \
    -out /opt/wildfly/standalone/configuration/wildfly.p12 \
    -name wildfly \
    -password pass:YOUR_KEYSTORE_PASSWORD

keytool -importkeystore \
    -deststorepass YOUR_KEYSTORE_PASSWORD \
    -destkeypass YOUR_KEYSTORE_PASSWORD \
    -destkeystore /opt/wildfly/standalone/configuration/wildfly.keystore \
    -srckeystore /opt/wildfly/standalone/configuration/wildfly.p12 \
    -srcstoretype PKCS12 \
    -srcstorepass YOUR_KEYSTORE_PASSWORD \
    -alias wildfly \
    -noprompt

# Set ownership
chown wildfly:daemon /opt/wildfly/standalone/configuration/wildfly.*

# Reload WildFly
systemctl reload wildfly
EOF

# Make executable
sudo chmod +x /opt/wildfly/scripts/renew-cert.sh

# Add to crontab for automatic renewal
sudo crontab -e
# Add: 0 2 1 * * /opt/wildfly/scripts/renew-cert.sh
```

## Verification

### 1. DNS Configuration Test

```bash
# Verify DNS records are properly configured
nslookup yourdomain.com
nslookup iam.yourdomain.com
nslookup api.yourdomain.com

# Test DNS propagation
dig +short yourdomain.com
dig +short iam.yourdomain.com
dig +short api.yourdomain.com

# Verify CAA records are properly configured
dig yourdomain.com CAA

# Check all records point to your server IP
# All should return: YOUR_SERVER_IP
```

### 2. Service Status

```bash
# Check WildFly status
sudo systemctl status wildfly

# Check listening ports
sudo netstat -tlnp | grep java

# Check SSL certificate
openssl s_client -connect localhost:443 -servername yourdomain.com
```

### 3. Security Headers Test

```bash
# Test security headers
curl -I https://yourdomain.com

# Expected headers:
# Strict-Transport-Security: max-age=63072000; includeSubDomains; preload
# X-Frame-Options: DENY
# X-Content-Type-Options: nosniff
# X-XSS-Protection: 1; mode=block
# Content-Security-Policy: default-src 'self'; ...
```

### 4. TLS 1.3 Configuration Test

```bash
# Test TLS 1.3 support on all domains (only protocol supported)
openssl s_client -connect yourdomain.com:443 -tls1_3
openssl s_client -connect iam.yourdomain.com:443 -tls1_3  
openssl s_client -connect api.yourdomain.com:443 -tls1_3

# Verify TLS 1.2 is rejected on all domains
openssl s_client -connect yourdomain.com:443 -tls1_2
openssl s_client -connect iam.yourdomain.com:443 -tls1_2
openssl s_client -connect api.yourdomain.com:443 -tls1_2

# Test specific cipher suites
nmap --script ssl-enum-ciphers -p 443 yourdomain.com

# Expected cipher suites (TLS 1.3 only):
# TLS_AES_256_GCM_SHA384
# TLS_CHACHA20_POLY1305_SHA256  
# TLS_AES_128_GCM_SHA256
```

## Access Points

- **Main Application**: https://yourdomain.com
- **IAM Service**: https://iam.yourdomain.com
- **API Service**: https://api.yourdomain.com
- **Management Console**: http://localhost:9990 (local access only)
- **HTTP Redirect**: All HTTP traffic → HTTPS

## Security Notes

### Credential Management
- All passwords mentioned as placeholders should be replaced with secure, randomly generated credentials
- Store credentials securely using a password manager
- Never commit credentials to version control

### Regular Maintenance
- Update WildFly regularly for security patches
- Monitor certificate expiration dates
- Review security logs regularly
- Update Java runtime for security patches

### Hardening Recommendations
- Management console is only accessible locally (not exposed to internet)
- Use strong authentication for database connections
- Implement proper logging and monitoring
- Regular security audits and penetration testing

## Troubleshooting

### Common Issues

1. **Certificate Issues**
   ```bash
   # Check certificate validity
   keytool -list -keystore /opt/wildfly/standalone/configuration/wildfly.keystore
   ```

2. **Port Conflicts**
   ```bash
   # Check what's using ports
   sudo lsof -i :443
   sudo lsof -i :8080
   ```

3. **Permission Issues**
   ```bash
   # Fix WildFly permissions
   sudo chown -R wildfly:daemon /opt/wildfly
   ```

4. **Configuration Backup**
   ```bash
   # Backup current configuration
   sudo cp /opt/wildfly/standalone/configuration/standalone-full.xml /opt/wildfly/standalone/configuration/standalone-full.xml.backup
   ```

## References

- [WildFly Documentation](https://docs.wildfly.org/)
- [Let's Encrypt Documentation](https://letsencrypt.org/docs/)
- [OWASP Security Headers](https://owasp.org/www-project-secure-headers/)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)
