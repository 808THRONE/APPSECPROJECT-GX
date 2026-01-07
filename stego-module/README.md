# Steganography Module

## Overview
**XChaCha20-Poly1305** encryption combined with **STC (Syndrome Trellis Codes)** steganography for secure covert data transmission.

## Algorithm: STC (Filler-Fridrich)
The module implements Syndrome Trellis Codes, a near-optimal steganographic embedding algorithm that:
- Minimizes embedding distortion using trellis-based optimization
- Uses HILL-inspired cost calculation for adaptive embedding
- Achieves PSNR ≥ 45dB for high imperceptibility

## Technology Stack
- **Java 21** with Jakarta EE 10
- **Bouncy Castle 1.77** - XChaCha20-Poly1305 encryption
- **STC Engine** - Pure Java implementation (no native dependencies)

## Components
```
src/main/java/com/securegate/stego/
├── StegoApplication.java           # JAX-RS application entry
├── SteganographyAPI.java           # REST API endpoints
├── AdaptiveSteganographyService.java # Main orchestration service
├── StcStegoEngine.java             # STC embedding/extraction
└── XChaCha20Poly1305EncryptionService.java # Encryption service
```

## API Endpoints
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/stego/embed` | Embed secret into cover image |
| POST | `/api/stego/extract` | Extract secret from stego image |
| GET | `/api/stego/capacity?size=N` | Calculate capacity for image size |
| GET | `/api/stego/health` | Health check |

## Security Parameters
- **Encryption**: XChaCha20-Poly1305 with 192-bit nonce, 128-bit auth tag
- **Key Derivation**: PBKDF2-SHA512 with 100,000 iterations
- **Embedding**: STC with h=10 constraint height

## Quality Metrics
- **PSNR**: ≥45dB (imperceptibility target)
- **Capacity**: ~100% of cover image size (minus 76-byte overhead)

## Build
```bash
cd stego-module
mvn clean package
```
