import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { yedsAllowedHosts } from '../../shared/frontend/vite-allowed-hosts.js'

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
    host: '0.0.0.0',
    port: 5185,
    strictPort: true,
    allowedHosts: yedsAllowedHosts,
    proxy: {
      '/alb-api': {
        target: 'http://127.0.0.1:8095',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/alb-api/, '/api/alb')
      }
    }
  },
  preview: {
    host: '0.0.0.0',
    port: 5185,
    allowedHosts: yedsAllowedHosts,
    proxy: {
      '/alb-api': {
        target: 'http://127.0.0.1:8095',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/alb-api/, '/api/alb')
      }
    }
  }
})
