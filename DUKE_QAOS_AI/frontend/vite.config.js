/*
 * Copyright © 2025 Devin B. Royal.
 * All Rights Reserved.
 *
 * Vite configuration for DUKE QAOS-AI frontend.
 * Production-ready: Optimized builds, CORS handling.
 */

import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3000,
    host: '0.0.0.0',
    cors: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      }
    }
  },
  build: {
    outDir: '../backend/target/static',
    emptyOutDir: true,
    sourcemap: true
  }
});
/* Copyright © 2025 Devin B. Royal. All Rights Reserved. */