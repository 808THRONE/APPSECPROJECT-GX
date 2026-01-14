param(
    [Parameter(Mandatory = $true)]
    [string]$VpsIp,

    [Parameter(Mandatory = $true)]
    [string]$SshUser,

    [Parameter(Mandatory = $false)]
    [string]$SshKeyPath,

    [string]$RemoteDir = "/home/$SshUser/securegate-deploy"
)

$Artifacts = @(
    "iam-service\target\iam-service.war",
    "api-gateway\target\api-gateway.war",
    "stego-module\target\stego-module.war",
    "securegate-ui\target\securegate-ui.war",
    "$env:USERPROFILE\.gemini\antigravity\brain\0757cd73-57ea-4068-8d32-475974e47d51\VPS_DEPLOYMENT_GUIDE.md",
    "$env:USERPROFILE\.gemini\antigravity\brain\0757cd73-57ea-4068-8d32-475974e47d51\SECURITY_WHITEPAPER.md",
    "$env:USERPROFILE\.gemini\antigravity\brain\0757cd73-57ea-4068-8d32-475974e47d51\FILE_SECURITY_ANALYSIS.md"
)

Write-Host "🚀 Starting Deployment upload to $SshUser@$VpsIp..." -ForegroundColor Cyan

# Check if artifacts exist locally
foreach ($file in $Artifacts) {
    if (-not (Test-Path $file)) {
        Write-Error "Artifact not found: $file"
        exit 1
    }
}

# Construct SCP command base
$scpCmd = "scp"
if (-not [string]::IsNullOrEmpty($SshKeyPath)) {
    $scpCmd += " -i `"$SshKeyPath`""
}

# Create remote directory
Write-Host "Creating remote directory: $RemoteDir"
$sshCmd = "ssh"
if (-not [string]::IsNullOrEmpty($SshKeyPath)) {
    $sshCmd += " -i `"$SshKeyPath`""
}
$createDirCmd = "$sshCmd $SshUser@$VpsIp `"mkdir -p $RemoteDir`""
Invoke-Expression $createDirCmd

if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to create remote directory. Check connectivity/credentials."
    exit 1
}

# Upload files
foreach ($file in $Artifacts) {
    $fileName = Split-Path $file -Leaf
    Write-Host "Uploading $fileName..." -NoNewline
    
    $uploadCmd = "$scpCmd `"$file`" $SshUser@$VpsIp`:$RemoteDir/"
    Invoke-Expression $uploadCmd
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host " [OK]" -ForegroundColor Green
    }
    else {
        Write-Host " [FAILED]" -ForegroundColor Red
        exit 1
    }
}

Write-Host "`n✅ All files uploaded successfully to $RemoteDir" -ForegroundColor Green
Write-Host "Next Steps:"
Write-Host "1. SSH into the server: ssh $SshUser@$VpsIp"
Write-Host "2. Follow the instructions in VPS_DEPLOYMENT_GUIDE.md"
