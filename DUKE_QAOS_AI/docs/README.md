# DUKE QAOS-AI - Unified Post-Quantum AI Operating System

## Overview

**DUKE QAOS-AI** represents the convergence of two cutting-edge systems:
- **DUKEAi**: The cognitive core of DUKEªٱ, featuring DTK Kernel, BOSS Blockchain, NIL Neuro Interface, COE Engine, and XR Shell
- **QAOS-AI**: Quantum-AI Operating System for Autonomous Security and Identity

This unified platform delivers:
- Post-quantum cryptography (NIST FIPS-203/204/205 compliant via ML-KEM/ML-DSA)
- Self-aware Bash pipeline for native/web packaging
- Adaptive cognition loops with neuro-feedback simulation
- Real-time threat monitoring and decentralized identity management
- XR-native productivity tools and blockchain monetization

## Architecture
DUKE_QAOS_AI/
├── backend/          # Java backend with Maven
│   ├── src/main/java/com/devinroyal/dukeai/
│   │   ├── DUKEAi.java           # Cognitive core orchestrator
│   │   ├── QAOSAI.java           # HTTP server with embedded frontend
│   │   ├── core/                 # Core OS subsystems
│   │   ├── components/           # QAOS-AI components
│   │   ├── enhancements/         # Advanced AI features
│   │   ├── security/             # PQ cryptography
│   │   └── utils/                # Utilities
│   ├── target/                   # Compiled classes & static assets
│   └── pom.xml                   # Maven configuration
├── frontend/         # Vue.js frontend (embedded in backend)
├── docs/            # Documentation
└── target/scripts/  # Generated packaging scripts


## Security Features

- **Post-Quantum Cryptography**: ML-KEM (key encapsulation), ML-DSA (signatures) via Bouncy Castle 1.79
- **Threat Model**: Resists quantum Grover/Shor attacks; hybrid classical-PQ modes available
- **Key Management**: Ephemeral keys, secure random, no hard-coded secrets
- **Input Validation**: Defensive programming throughout all components
- **Concurrency**: Thread-safe data structures (ConcurrentHashMap, locks)
- **Supply Chain**: PQ-signed Bash scripts for reproducible builds

## Quick Start

### Prerequisites
- Java 11+ with Maven
- Node.js 16+ (for frontend development)

### Backend (Primary)
```bash
cd DUKE_QAOS_AI
mvn clean package
java -jar target/DUKEAi-1.0.0.jar

Access: http://localhost:8080

cd frontend
npm install
npm run dev

Native Packaging
bash# Generate platform-specific packaging scripts
java -jar target/DUKEAi-1.0.0.jar

# Package for Linux (requires GraalVM)
bash target/scripts/package_linux.sh

# Package for Web (PWA/WASM)
bash target/scripts/package_web.sh


