# SecureGate IAM Portal - Simple VPS Deployment Guide

This guide is simplified to use the `vps_installer.sh` script, which automates 90% of the work.

> [!NOTE]
> **Why is this necessary?**
> Your application requires a Java Server (WildFly) and a Database (PostgreSQL) to run. These are not standard on a blank VPS, so they must be installed. The script below does this for you automatically.

## 1. Prepare files on your PC

Run these commands in PowerShell to zip your files for upload.

```powershell
# 1. Zip your pre-configured WildFly server
Compress-Archive -Path "wildfly-38.0.0.Final" -DestinationPath "wildfly.zip" -Force

# 2. Build and Zip the Frontend
cd pwa-frontend
npm install
npm run build
Compress-Archive -Path "dist" -DestinationPath "..\frontend.zip" -Force
cd ..
```

You should now have `wildfly.zip` and `frontend.zip` in your root folder.

## 2. Upload to VPS

Use `scp` to copy the files and the installer script to your VPS.
Replace `user` and `your-vps-ip` with your actual details.

```powershell
scp wildfly.zip frontend.zip vps_installer.sh user@your-vps-ip:~/
```

## 3. Run the Installer

Login to your VPS and run the script.

```bash
# Login
ssh user@your-vps-ip

# Run the installer
chmod +x vps_installer.sh
./vps_installer.sh
```

**That's it!** The script will:
1.  Install Java, PostgreSQL, and Nginx.
2.  Setup the Database.
3.  Deploy your WildFly server.
4.  Deploy your Frontend.
5.  Start everything.

Visit `http://your-vps-ip` to see your app.
