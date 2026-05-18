import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.VITE_API_PROXY_TARGET || 'http://127.0.0.1:8080'
  return {
    base: '/iam/login/',
    plugins: [vue()],
    resolve: {
      alias: {
        '@yeds/ui': path.resolve(__dirname, '../yeds-ui/src')
      }
    },
    server: {
      host: '127.0.0.1',
      port: 5190,
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true
        }
      }
    },
    build: {
      outDir: 'dist',
      emptyOutDir: true
    }
  }
})
