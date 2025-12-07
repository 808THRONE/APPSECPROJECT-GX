# Steganography Module

## Overview
AES-256-GCM encryption combined with LSB-DCT steganography for covert data transmission.

## Technology Stack
- Bouncy Castle 1.77 (AES-256-GCM)
- OpenCV 4.8.x Java bindings (LSB-DCT)
- MinIO (Cover image storage)

## Structure
```
src/main/java/com/securegate/stego/
├── AesGcmEncryptionService.java    # AES-256-GCM with PBKDF2
├── LsbDctStegoEngine.java          # LSB-DCT implementation
├── CoverImageService.java          # MinIO integration
└── SteganographyAPI.java           # REST endpoints
```

## Implementation Status
⏳ **Pending Implementation** - Placeholder structure created

## Quality Metrics
- **PSNR**: ≥45dB (imperceptibility)
- **MSE**: <0.5
- **Robustness**: JPEG Q≥85
- **Capacity**: 12.5% of cover image size
