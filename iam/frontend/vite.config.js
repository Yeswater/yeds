import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const apiBaseUrl = env.VITE_API_BASE_URL || '/apig'
  const apigTarget = env.VITE_APIG_PROXY_TARGET || 'http://127.0.0.1:8080'
  return {
    base: '/iam/',
    define: {
      'import.meta.env.VITE_API_BASE_URL': JSON.stringify(apiBaseUrl)
    },
    plugins: [vue()],
    resolve: {
      alias: {
        '@yeds/ui': path.resolve(__dirname, '../../shared/frontend/yeds-ui/src'),
        'element-plus': path.resolve(__dirname, 'node_modules/element-plus')
      }
    },
    server: {
      // 0.0.0.0：供 Docker nginx（host.docker.internal）反代；浏览器经 yeds.com/iam/ 访问
      host: '0.0.0.0',
      port: 5181,
      strictPort: true,
      allowedHosts: true,
      proxy: {
        '/apig': {
          target: apigTarget,
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/apig/, '')
        }
      },
      hmr: {
        protocol: 'ws',
        host: env.VITE_DEV_HOST || 'localhost',
        clientPort: 80,
        path: '/iam/'
      }
    }
  }
})
