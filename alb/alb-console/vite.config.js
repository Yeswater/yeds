import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export default defineConfig({
  base: '/alb/',
  plugins: [vue()],
  resolve: {
    alias: {
      '@yeds/ui': path.resolve(__dirname, '../../shared/frontend/yeds-ui/src'),
      'element-plus': path.resolve(__dirname, 'node_modules/element-plus')
    }
  },
  server: {
    host: '127.0.0.1',
    port: 5185,
    strictPort: true,
    allowedHosts: ['yeds.com', 'localhost', '127.0.0.1'],
    proxy: {
      '/alb-api': {
        target: 'http://127.0.0.1:8095',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/alb-api/, '/api/alb')
      }
    }
  }
})
