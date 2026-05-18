<template>
  <el-container class="yeds-admin-root admin-root">
    <YedsAdminHeader
      brand-label="BIDS"
      :platform-options="platformOptions"
      :username="displayUser"
      :avatar-text="avatarText"
      :show-theme-toggle="true"
      :dark-mode="isDark"
      @update:dark-mode="isDark = $event"
      @platform-command="onPlatformCommand"
      @user-command="onUserCommand"
    />

    <el-container class="yeds-admin-body admin-body">
      <el-aside class="yeds-admin-aside admin-aside">
        <el-menu :key="route.path" :default-active="route.path" router class="yeds-admin-side-menu side-menu">
          <el-menu-item index="/run/ds">
            <span>数据源</span>
          </el-menu-item>
          <el-menu-item index="/run/svc">
            <span>服务配置</span>
          </el-menu-item>
          <el-menu-item index="/run/run">
            <span>服务运行</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="yeds-admin-main admin-main">
        <div class="yeds-page-shell">
          <router-view />
        </div>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { YedsAdminHeader } from '@yeds/ui'
import { readSession, readThemePreference, writeThemePreference } from '../authStorage.js'
import { logoutSession } from '../iamSession.js'
import { switchToIam } from '../platformSwitch.js'

const route = useRoute()
const router = useRouter()
const displayUser = ref('')
const isDark = ref(readThemePreference() === 'dark')

const platformOptions = [
  { command: 'bids', label: 'BIDS' },
  { command: 'iam', label: 'IAM' },
  { command: 'edm', label: 'EDM' }
]

const avatarText = computed(() => {
  const u = displayUser.value
  if (!u) {
    return '?'
  }
  return u.slice(0, 1).toUpperCase()
})

function syncUser() {
  displayUser.value = readSession()?.username ?? ''
}

function applyHtmlTheme() {
  const root = document.documentElement
  if (isDark.value) {
    root.classList.add('dark')
  } else {
    root.classList.remove('dark')
  }
  writeThemePreference(isDark.value ? 'dark' : 'light')
}

watch(isDark, applyHtmlTheme)

onMounted(() => {
  syncUser()
  applyHtmlTheme()
})

router.afterEach(() => {
  syncUser()
})

function goHome() {
  router.push('/run/svc')
}

function onPlatformCommand(platform) {
  if (platform === 'bids') {
    goHome()
    return
  }
  if (platform === 'iam') {
    switchToIam('/abac/policy')
    return
  }
  const edmUrl = import.meta.env.VITE_EDM_APP_URL
  if (edmUrl && edmUrl !== '#') {
    window.location.assign(edmUrl)
  }
}

async function onUserCommand(cmd) {
  if (cmd === 'logout') {
    await logoutSession()
    router.replace('/login')
  }
}
</script>
