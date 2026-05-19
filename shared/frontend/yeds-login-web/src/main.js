import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@yeds/ui/styles/yeds-admin-tokens.css'
import '@yeds/ui/styles/yeds-login-page.css'
import App from './App.vue'

createApp(App).use(ElementPlus).mount('#app')
