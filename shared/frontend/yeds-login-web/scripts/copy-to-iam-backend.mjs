import { cpSync, existsSync, rmSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const distDir = path.resolve(__dirname, '../dist')
const targetDir = path.resolve(__dirname, '../../../../iam/backend/src/main/resources/static/yeds-login')

if (!existsSync(distDir)) {
  console.error('dist 不存在，请先执行 npm run build')
  process.exit(1)
}

rmSync(targetDir, { recursive: true, force: true })
cpSync(distDir, targetDir, { recursive: true })
console.log(`已复制登录页静态资源到 ${targetDir}`)
