import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api/config': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true
      },
      '/api/runtime': {
        target: 'http://127.0.0.1:8082',
        changeOrigin: true
      }
    }
  }
})
