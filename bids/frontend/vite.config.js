import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { yedsAllowedHosts } from '../../shared/frontend/vite-allowed-hosts.js'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiBase = env.VITE_API_BASE || '/apig'
  return {
  base: '/bids/',
  define: {
    'import.meta.env.VITE_API_BASE': JSON.stringify(apiBase)
  },
  plugins: [vue()],
  resolve: {
    alias: {
      '@yeds/ui': path.resolve(__dirname, '../../shared/frontend/yeds-ui/src'),
      'element-plus': path.resolve(__dirname, 'node_modules/element-plus')
    }
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    allowedHosts: yedsAllowedHosts,
    proxy: {
      '/apig': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/apig/, '')
      }
    }
  },
  preview: {
    host: '0.0.0.0',
    port: 5173,
    allowedHosts: yedsAllowedHosts,
    proxy: {
      '/apig': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true,
        rewrite: (p) => p.replace(/^\/apig/, '')
      }
    }
  }
  }
})
