# SecureGate IAM Portal - Linux VPS Deployment Guide

This guide explains how to deploy the **entire project** folder to your Ubuntu VPS and run the automated installer.

## 1. Prepare & Zip

On your local machine (Windows), make sure you are in the project root:

1.  **Check Build Artifacts**: Ensure you have already built the project locally (which you have, based on the `wildfly` and `dist` folders present).
2.  **Zip the Project**: Select all files in the project folder and zip them into `securegate-project.zip`.
    *   *Tip*: You can exclude `.git`, `node_modules`, and `src` directories if you want a smaller zip, but the installer works regardless.

## 2. Upload to VPS

Open your terminal (PowerShell or Git Bash) and run:

```bash
# Replace with your actual user and IP
scp securegate-project.zip user@<VPS_IP>:~/
```

## 3. Connect & Deploy (Linux Commands)

SSH into your VPS and run the following commands:

### Step 3.1: Install Unzip & Unpack
```bash
ssh user@<VPS_IP>
sudo apt update && sudo apt install unzip -y

# Create directory and unzip
mkdir -p ~/securegate
unzip securegate-project.zip -d ~/securegate
cd ~/securegate
```

### Step 3.2: Run the Installer
I have updated `vps_installer.sh` to work directly inside this folder.

```bash
# Make script executable
chmod +x vps_installer.sh

# Run it
./vps_installer.sh
```

---

## What the Script Does (Under the Hood)

You asked for Linux commands. Here is exactly what the `vps_installer.sh` script is executing for you:

### 1. Install Dependencies
```bash
sudo apt install -y openjdk-17-jdk nginx postgresql postgresql-contrib
```

### 2. Configure Database
```bash
# Create Database and User
sudo -u postgres psql -c "CREATE DATABASE securegate_iam;"
sudo -u postgres psql -c "CREATE USER securegate WITH ENCRYPTED PASSWORD 'securegate_password';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE securegate_iam TO securegate;"
sudo -u postgres psql -d securegate_iam -c "GRANT ALL ON SCHEMA public TO securegate;"
```

### 3. Deploy WildFly (Backend)
```bash
# Move the pre-configured WildFly folder from your zip to /opt
sudo mv wildfly-38.0.0.Final /opt/wildfly

# Create Service User
sudo useradd -r -s /sbin/nologin wildfly
sudo chown -R wildfly:wildfly /opt/wildfly

# Setup Systemd Service
echo "[Unit]
Description=The WildFly Application Server
After=postgresql.service
[Service]
User=wildfly
ExecStart=/opt/wildfly/bin/standalone.sh -b=0.0.0.0
[Install]
WantedBy=multi-user.target" | sudo tee /etc/systemd/system/wildfly.service

# Start Service
sudo systemctl enable --now wildfly
```

### 4. Deploy Frontend (Nginx)
```bash
# Move the dist folder from your zip to /var/www
sudo mkdir -p /var/www/securegate
sudo mv pwa-frontend/dist/* /var/www/securegate/
sudo chown -R www-data:www-data /var/www/securegate

# Configure Nginx
echo "server {
    listen 80;
    root /var/www/securegate;
    index index.html;
    location / { try_files \$uri \$uri/ /index.html; }
    location /iam-service/ { proxy_pass http://localhost:8080/iam-service/; }
    location /api-gateway/ { proxy_pass http://localhost:8080/api-gateway/; }
    location /stego-module/ { proxy_pass http://localhost:8080/stego-module/; }
}" | sudo tee /etc/nginx/sites-available/securegate

# Enable & Restart
sudo ln -s /etc/nginx/sites-available/securegate /etc/nginx/sites-enabled/
sudo rm /etc/nginx/sites-enabled/default
sudo systemctl restart nginx
```
