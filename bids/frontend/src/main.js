import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import 'vxe-table/lib/style.css'
import VXETable from 'vxe-table'
import App from './App.vue'
import { router } from './router'
import { readThemePreference } from './authStorage.js'
import './style.css'

if (readThemePreference() === 'dark') {
  document.documentElement.classList.add('dark')
} else {
  document.documentElement.classList.remove('dark')
}

const app = createApp(App)
app.use(ElementPlus)
app.use(VXETable)
app.use(router)
app.mount('#app')
