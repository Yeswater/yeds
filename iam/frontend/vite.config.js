import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@yeds/ui': path.resolve(__dirname, '../../shared/frontend/yeds-ui/src'),
        'element-plus': path.resolve(__dirname, 'node_modules/element-plus')
      }
    },
    server: {
      host: '127.0.0.1',
      port: 5181,
      proxy: {
        '/api': {
          target: env.VITE_API_PROXY_TARGET || 'http://127.0.0.1:8091',
          changeOrigin: true
        },
        '/iam': {
          target: env.VITE_GATEWAY_PROXY_TARGET || 'http://127.0.0.1:8080',
          changeOrigin: true
        }
      }
    }
  }
})
