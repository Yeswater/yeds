<template>
  <el-container class="admin-root">
    <el-header class="admin-header">
      <el-dropdown trigger="click" @command="onPlatformCommand">
        <span class="brand-switcher">
          <span class="brand">BIDS</span>
          <span class="caret">▼</span>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="bids">BIDS</el-dropdown-item>
            <el-dropdown-item command="iam">IAM</el-dropdown-item>
            <el-dropdown-item command="edm">EDM</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <div class="header-fill" />
      <div class="header-actions">
        <span class="theme-label">主题</span>
        <el-switch
          v-model="isDark"
          inline-prompt
          active-text="暗"
          inactive-text="亮"
          style="--el-switch-on-color: #409eff; --el-switch-off-color: #dcdfe6"
        />
        <el-dropdown trigger="click" @command="onUserCommand">
          <span class="user-trigger">
            <el-avatar :size="32" class="user-avatar">{{ avatarText }}</el-avatar>
            <span class="user-name">{{ displayUser }}</span>
            <span class="caret">▼</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-container class="admin-body">
      <el-aside width="200px" class="admin-aside">
        <el-menu :key="route.path" :default-active="route.path" router class="side-menu">
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
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { readSession, readThemePreference, writeThemePreference } from '../authStorage.js'
import { logoutSession } from '../iamSession.js'
import { switchToIam } from '../platformSwitch.js'

const route = useRoute()
const router = useRouter()
const displayUser = ref('')
const isDark = ref(readThemePreference() === 'dark')

const avatarText = computed(() => {
  const u = displayUser.value
  if (!u) return '?'
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

<style scoped>
.admin-root {
  height: 100vh;
  flex-direction: column;
}
.admin-header {
  display: flex;
  align-items: center;
  height: 52px !important;
  padding: 0 16px;
  border-bottom: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
}
.brand {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--el-color-primary);
  cursor: default;
  user-select: none;
}
.brand-switcher {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}
.header-fill {
  flex: 1;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
.theme-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
}
.user-trigger:hover {
  background: var(--el-fill-color-light);
}
.user-avatar {
  flex-shrink: 0;
}
.user-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}
.caret {
  font-size: 10px;
  color: var(--el-text-color-secondary);
}
.admin-body {
  flex: 1;
  min-height: 0;
}
.admin-aside {
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
}
.side-menu {
  flex: 1;
  border-right: none;
}
.admin-main {
  padding: 0;
  background: var(--el-bg-color-page);
  overflow: auto;
}
</style>
