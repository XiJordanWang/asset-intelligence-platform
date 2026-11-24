import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// In development the Spring Boot API runs on :8080. In the Docker image the built files
// are served by Spring Boot itself, so relative /api calls work in both cases.
export default defineConfig({
  plugins: [react()],
  server: { proxy: { '/api': 'http://localhost:8080' } },
})
