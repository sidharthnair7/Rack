import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  build: {
    // Build straight into Spring's static resources so `mvn package` produces ONE jar that
    // serves the UI, the API, the storefront and the uploaded images from a single origin.
    // Deploying two services and reconciling CORS between them is the kind of task that eats
    // a day you do not have; this makes deployment "copy one jar and run it".
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      // Proxied rather than calling http://localhost:8080 directly, so the app uses the same
      // relative paths in dev as in production (where Spring serves both the bundle and the API
      // from one origin). It also means the image paths the API returns — relative, like
      // /uploads/items/1/on_model.jpg — resolve as-is with no rewriting on the client.
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/uploads': { target: 'http://localhost:8080', changeOrigin: true },
      '/shop': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
