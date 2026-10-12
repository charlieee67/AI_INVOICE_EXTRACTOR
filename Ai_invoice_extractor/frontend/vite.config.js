import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
      outDir: '../src/main/resources/static',
      emptyOutDir: true,
    },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})

