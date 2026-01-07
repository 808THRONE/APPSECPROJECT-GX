# Server Setup Requirements - Complete Checklist

## ✅ What You Already Have
- WildFly 38.0.1.Final Preview (Jakarta EE 11)
- TLS 1.3 configured
- HSTS enabled
- Domain: mortadha.me

## ❌ What You NEED to Install

### 1. PostgreSQL 16+ (Database)

**Why?** The IAM service stores users, tenants, and grants in a database.

```bash
# Install PostgreSQL 16
sudo apt update
sudo apt install -y postgresql-16 postgresql-contrib-16

# Start and enable
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Verify installation
sudo -u postgres psql --version
# Should show: psql (PostgreSQL) 16.x
```

**Configure PostgreSQL:**
```bash
# Create database and user
sudo -u postgres psql << EOF
CREATE DATABASE iam_production;
CREATE USER iam_user WITH ENCRYPTED PASSWORD 'CHANGE_THIS_PASSWORD';
GRANT ALL PRIVILEGES ON DATABASE iam_production TO iam_user;
\c iam_production
GRANT ALL ON SCHEMA public TO iam_user;
ALTER USER iam_user CREATEDB;
EOF

# Load initial schema
sudo -u postgres psql iam_production < /path/to/schema.sql
```

**Security Hardening:**
```bash
# Edit PostgreSQL config for security
sudo nano /etc/postgresql/16/main/pg_hba.conf

# Change this line:
# local   all   all   peer
# To:
local   all   all   md5

# Restart PostgreSQL
sudo systemctl restart postgresql
```

---

### 2. PostgreSQL JDBC Driver (for WildFly)

**Why?** WildFly needs this driver to connect to PostgreSQL.

```bash
# Download PostgreSQL JDBC driver
cd /tmp
wget https://jdbc.postgresql.org/download/postgresql-42.7.2.jar

# Create WildFly module directory
sudo mkdir -p /opt/wildfly/modules/system/layers/base/org/postgresql/main

# Copy driver
sudo cp postgresql-42.7.2.jar /opt/wildfly/modules/system/layers/base/org/postgresql/main/

# Create module.xml
sudo tee /opt/wildfly/modules/system/layers/base/org/postgresql/main/module.xml > /dev/null << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.9" name="org.postgresql">
    <resources>
        <resource-root path="postgresql-42.7.2.jar"/>
    </resources>
    <dependencies>
        <module name="javax.api"/>
        <module name="javax.transaction.api"/>
    </dependencies>
</module>
EOF

# Set ownership
sudo chown -R wildfly:wildfly /opt/wildfly/modules/system/layers/base/org/postgresql
```

**Configure in WildFly:**
```bash
# Connect to WildFly CLI
/opt/wildfly/bin/jboss-cli.sh --connect

# Add PostgreSQL driver
/subsystem=datasources/jdbc-driver=postgresql:add(driver-name=postgresql,driver-module-name=org.postgresql,driver-class-name=org.postgresql.Driver)

# Add datasource
data-source add \
    --name=PostgresDS \
    --jndi-name=java:jboss/datasources/PostgresDS \
    --driver-name=postgresql \
    --connection-url=jdbc:postgresql://localhost:5432/iam_production \
    --user-name=iam_user \
    --password=YOUR_SECURE_PASSWORD \
    --use-ccm=true \
    --max-pool-size=25 \
    --blocking-timeout-wait-millis=5000 \
    --enabled=true \
    --validate-on-match=true \
    --background-validation=false \
    --valid-connection-checker-class-name=org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLValidConnectionChecker \
    --exception-sorter-class-name=org.jboss.jca.adapters.jdbc.extensions.postgres.PostgreSQLExceptionSorter

# Test connection
/subsystem=datasources/data-source=PostgresDS:test-connection-in-pool

# Exit CLI
exit
```

---

### 3. Redis (Optional - for Session Management)

**Note:** The current clean implementation **doesn't use Redis yet**. You can skip this for now.

If you want to add Redis later:
```bash
sudo apt install -y redis-server
sudo systemctl enable redis-server
sudo systemctl start redis-server
```

---

## 🔧 WildFly Configuration Adjustments

### 1. Virtual Host for iam.mortadha.me

**Edit standalone.xml:**
```bash
sudo nano /opt/wildfly/standalone/configuration/standalone.xml
```

**Find the `<subsystem xmlns="urn:jboss:domain:undertow:14.0">` section and add:**
```xml
<subsystem xmlns="urn:jboss:domain:undertow:14.0">
    <server name="default-server">
        <!-- Existing configuration -->
        
        <!-- Add IAM virtual host -->
        <host name="iam-host" alias="iam.mortadha.me" default-web-module="iam-service.war">
            <location name="/" handler="welcome-content"/>
            <http-invoker security-realm="ApplicationRealm"/>
        </host>
    </server>
</subsystem>
```

**Or use CLI:**
```bash
/opt/wildfly/bin/jboss-cli.sh --connect

/subsystem=undertow/server=default-server/host=iam-host:add(alias=["iam.mortadha.me"],default-web-module="iam-service.war")

exit
```

### 2. Increase Memory Limits (Optional but Recommended)

```bash
# Edit standalone.conf
sudo nano /opt/wildfly/bin/standalone.conf

# Find and modify JAVA_OPTS:
JAVA_OPTS="-Xms512m -Xmx2048m -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m"
```

---

## 📋 Complete Installation Checklist

| Component | Required? | Install Command | Status |
|-----------|-----------|-----------------|--------|
| **WildFly 38.0.1** | ✅ Yes | Already installed | ✅ |
| **PostgreSQL 16+** | ✅ Yes | `sudo apt install postgresql-16` | ⏳ |
| **PostgreSQL JDBC** | ✅ Yes | Download + module setup | ⏳ |
| **Java 21** | ✅ Yes | (Should be installed with WildFly) | ✅ |
| **Redis** | ❌ Optional | `sudo apt install redis-server` | Skip for now |
| **DNS Record** | ✅ Yes | Add A record: iam → VPS IP | ⏳ |
| **TLS Certificate** | ✅ Yes | `certbot certonly -d iam.mortadha.me` | ⏳ |

---

## 🚀 Step-by-Step Server Setup Script

**Save this as `setup-iam-server.sh` and run it:**

```bash
#!/bin/bash
set -e

echo "======================================"
echo "IAM Service Server Setup"
echo "======================================"

# 1. Install PostgreSQL
echo "Installing PostgreSQL..."
sudo apt update
sudo apt install -y postgresql-16 postgresql-contrib-16
sudo systemctl start postgresql
sudo systemctl enable postgresql

# 2. Create database
echo "Creating IAM database..."
sudo -u postgres psql << EOF
CREATE DATABASE iam_production;
CREATE USER iam_user WITH ENCRYPTED PASSWORD 'CHANGE_THIS_PASSWORD';
GRANT ALL PRIVILEGES ON DATABASE iam_production TO iam_user;
\c iam_production
GRANT ALL ON SCHEMA public TO iam_user;
ALTER USER iam_user CREATEDB;
EOF

# 3. Download PostgreSQL JDBC driver
echo "Installing PostgreSQL JDBC driver..."
cd /tmp
wget -q https://jdbc.postgresql.org/download/postgresql-42.7.2.jar

# 4. Create WildFly PostgreSQL module
echo "Creating WildFly PostgreSQL module..."
sudo mkdir -p /opt/wildfly/modules/system/layers/base/org/postgresql/main
sudo cp postgresql-42.7.2.jar /opt/wildfly/modules/system/layers/base/org/postgresql/main/

# 5. Create module.xml
sudo tee /opt/wildfly/modules/system/layers/base/org/postgresql/main/module.xml > /dev/null << 'MODULEXML'
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.9" name="org.postgresql">
    <resources>
        <resource-root path="postgresql-42.7.2.jar"/>
    </resources>
    <dependencies>
        <module name="javax.api"/>
        <module name="javax.transaction.api"/>
    </dependencies>
</module>
MODULEXML

sudo chown -R wildfly:wildfly /opt/wildfly/modules/system/layers/base/org/postgresql

echo "======================================"
echo "✅ Server setup complete!"
echo "======================================"
echo ""
echo "Next steps:"
echo "1. Configure WildFly datasource (see DEPLOYMENT.md)"
echo "2. Load database schema"
echo "3. Deploy IAM service WAR file"
echo "4. Add DNS record for iam.mortadha.me"
echo "5. Obtain TLS certificate with certbot"
```

**Run it:**
```bash
chmod +x setup-iam-server.sh
sudo ./setup-iam-server.sh
```

---

## 🔐 Security Firewall Rules

```bash
# Allow PostgreSQL only from localhost (more secure)
sudo ufw allow from 127.0.0.1 to any port 5432

# Allow HTTPS
sudo ufw allow 443/tcp

# Allow HTTP (for certbot challenges)
sudo ufw allow 80/tcp

# Reload firewall
sudo ufw reload
```

---

## 📝 Summary

### What you MUST install:
1. **PostgreSQL 16+** - Database server
2. **PostgreSQL JDBC Driver** - WildFly module for database connectivity

### What's already configured:
1. ✅ WildFly
2. ✅ TLS 1.3
3. ✅ HSTS
4. ✅ Java (with WildFly)

### What's optional:
1. Redis (not used in clean implementation)
2. Memory tuning
3. Additional monitoring tools

---

## 🧪 Quick Test After Setup

```bash
# Test PostgreSQL
sudo -u postgres psql -c "SELECT version();"

# Test WildFly datasource
/opt/wildfly/bin/jboss-cli.sh --connect --command="/subsystem=datasources/data-source=PostgresDS:test-connection-in-pool"

# Test DNS (after adding record)
dig iam.mortadha.me

# Test WildFly deployment
curl http://localhost:8080/iam/jwk
```

---

## 💡 Pro Tips

1. **Use the setup script** - It does everything automatically
2. **Change default passwords** - Don't use 'CHANGE_THIS_PASSWORD'
3. **Backup PostgreSQL** regularly:
   ```bash
   sudo -u postgres pg_dump iam_production > iam_backup.sql
   ```
4. **Monitor logs**:
   ```bash
   tail -f /opt/wildfly/standalone/log/server.log
   tail -f /var/log/postgresql/postgresql-16-main.log
   ```

---

**Need help with any step? Let me know!** 🚀
