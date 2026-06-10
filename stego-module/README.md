# Steganography Module

## Overview
XChaCha20-Poly1305 encryption combined with DWT (Discrete Wavelet Transform) steganography for robust, covert data transmission.

## Technology Stack
- Bouncy Castle 1.77 (XChaCha20-Poly1305)
- OpenCV 4.8.x Java bindings (DWT)
- MinIO (Cover image storage)

## Structure
```
src/main/java/com/securegate/stego/
├── XChaCha20Poly1305Service.java   # XChaCha20-Poly1305 with Argon2id KDF
├── DwtStegoEngine.java             # DWT steganography implementation
├── CoverImageService.java          # MinIO integration
└── SteganographyAPI.java           # REST endpoints
```

## Implementation Status
⏳ **Pending Implementation** - Placeholder structure created

## Quality Metrics
- **PSNR**: ≥45dB (imperceptibility)
- **MSE**: <0.5
- **Robustness**: JPEG Q≥85, resistant to filtering
- **Capacity**: Balanced against DWT sub-bands
