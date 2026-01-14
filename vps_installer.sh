#!/bin/bash

# SecureGate VPS Installer
# Automates dependency installation and setup for Ubuntu

set -e

echo "=== SecureGate VPS Installer ==="
echo "This script will install Java, Nginx, PostgreSQL, and setup WildFly."

# 1. Update & Install Dependencies
echo "[1/5] Installing dependencies..."
sudo apt update
sudo apt install -y openjdk-17-jdk nginx postgresql postgresql-contrib unzip

# 2. Setup Database
echo "[2/5] Setting up PostgreSQL..."
# Check if DB exists to avoid errors
if ! sudo -u postgres psql -lqt | cut -d \| -f 1 | grep -qw securegate_iam; then
    sudo -u postgres psql -c "CREATE DATABASE securegate_iam;"
    sudo -u postgres psql -c "CREATE USER securegate WITH ENCRYPTED PASSWORD 'securegate_password';"
    sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE securegate_iam TO securegate;"
    # Grant schema privileges
    sudo -u postgres psql -d securegate_iam -c "GRANT ALL ON SCHEMA public TO securegate;"
    echo "Files created."
else
    echo "Database already exists, skipping..."
fi

# 3. Setup WildFly
echo "[3/5] Setting up WildFly..."

if [ -d "/opt/wildfly" ]; then
    echo "Cleaning up old installation..."
    sudo systemctl stop wildfly || true
    sudo rm -rf /opt/wildfly
fi

# Check for the folder (since we are inside the unzipped project)
if [ -d "wildfly-38.0.0.Final" ]; then
    echo "Found WildFly folder, moving to /opt..."
    sudo cp -r wildfly-38.0.0.Final /opt/wildfly
elif [ -f "wildfly.zip" ]; then
    echo "Found wildfly.zip, unzipping..."
    sudo unzip -q wildfly.zip -d /opt/
    sudo mv /opt/wildfly-38.0.0.Final /opt/wildfly
else
    echo "Error: Could not find 'wildfly-38.0.0.Final' folder or 'wildfly.zip'."
    exit 1
fi

# Create wildfly user if not exists
if ! id "wildfly" &>/dev/null; then
    sudo groupadd -r wildfly
    sudo useradd -r -g wildfly -d /opt/wildfly -s /sbin/nologin wildfly
fi

sudo chown -R wildfly:wildfly /opt/wildfly

# Service file
cat <<EOF | sudo tee /etc/systemd/system/wildfly.service
[Unit]
Description=The WildFly Application Server
After=syslog.target network.target postgresql.service

[Service]
User=wildfly
Group=wildfly
ExecStart=/opt/wildfly/bin/standalone.sh -b=0.0.0.0 -bmanagement=0.0.0.0
StandardOutput=null

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable wildfly
sudo systemctl start wildfly

# 4. Setup Frontend
echo "[4/5] Setting up Frontend..."
sudo mkdir -p /var/www/securegate

if [ -d "pwa-frontend/dist" ]; then
    echo "Found Frontend build in 'pwa-frontend/dist'..."
    sudo cp -r pwa-frontend/dist/* /var/www/securegate/
elif [ -d "dist" ]; then
    echo "Found 'dist' folder..."
    sudo cp -r dist/* /var/www/securegate/
elif [ -f "frontend.zip" ]; then
    echo "Found frontend.zip, unzipping..."
    sudo unzip -q -o frontend.zip -d /var/www/securegate
    # Handle if it unzips into a 'dist' subdir
    if [ -d "/var/www/securegate/dist" ]; then
        sudo mv /var/www/securegate/dist/* /var/www/securegate/
        sudo rmdir /var/www/securegate/dist
    fi
else
    echo "Error: Could not find 'pwa-frontend/dist' or 'frontend.zip'."
    echo "Please ensure you have built the frontend locally: npm run build"
    exit 1
fi

sudo chown -R www-data:www-data /var/www/securegate

# Nginx Config
echo "[5/5] Configuring Nginx..."
cat <<EOF | sudo tee /etc/nginx/sites-available/securegate
server {
    listen 80;
    server_name _;

    root /var/www/securegate;
    index index.html;

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /iam-service/ {
        proxy_pass http://localhost:8080/iam-service/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }
    
    location /api-gateway/ {
        proxy_pass http://localhost:8080/api-gateway/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }

    location /stego-module/ {
        proxy_pass http://localhost:8080/stego-module/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
    }
}
EOF

# Enable site
sudo ln -sf /etc/nginx/sites-available/securegate /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl restart nginx

echo "=== Deployment Complete ==="
echo "Visit http://$(curl -s ifconfig.me) to verify."
