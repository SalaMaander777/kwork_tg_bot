import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Сборка кладётся в ресурсы Spring Boot и попадает в jar; в dev — прокси API на запущенный бот
export default defineConfig({
  base: '/admin/',
  plugins: [react()],
  build: {
    outDir: '../src/main/resources/static/admin',
    emptyOutDir: true,
    chunkSizeWarningLimit: 1000,
  },
  server: {
    port: 5173,
    proxy: { '/admin/api': 'http://localhost:8080' },
  },
})
