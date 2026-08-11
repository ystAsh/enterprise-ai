/*
 * =============================================================================
 * 파일명 : vite.config.js
 * =============================================================================
 * 목적
 *  - React 개발 서버의 Vite 설정을 관리한다.
 *  - /api 요청을 Spring Boot 서버로 전달한다.
 */

import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [
    react()
  ],

  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})