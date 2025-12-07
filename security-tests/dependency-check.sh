#!/bin/bash

# OWASP Dependency Check
# Scan for vulnerable dependencies (CVSS ≥ 7.0)

echo "🔍 OWASP Dependency Check"
echo "=========================="

# TODO: Install dependency-check if not present
# Download from: https://github.com/jeremylong/DependencyCheck

# Scan IAM Service
echo "Scanning IAM Service..."
# dependency-check --project "IAM Service" \
#   --scan ../iam-service/pom.xml \
#   --format HTML \
#   --out ./reports/iam-service

# Scan API Gateway
echo "Scanning API Gateway..."
# dependency-check --project "API Gateway" \
#   --scan ../api-gateway/pom.xml \
#   --format HTML \
#   --out ./reports/api-gateway

# Scan Steganography Module
echo "Scanning Steganography Module..."
# dependency-check --project "Stego Module" \
#   --scan ../stego-module/pom.xml \
#   --format HTML \
#   --out ./reports/stego-module

# Fail build if CVSS ≥ 7.0
# --failOnCVSS 7

echo ""
echo "✅ Dependency check complete"
echo "Review reports in ./reports/"
