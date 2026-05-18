import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@yeds/ui': path.resolve(__dirname, '../../shared/frontend/yeds-ui/src'),
      'element-plus': path.resolve(__dirname, 'node_modules/element-plus')
    }
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/iam': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  },
  preview: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/iam': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
