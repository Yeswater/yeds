<template>
  <el-container class="admin-root">
    <el-header class="admin-header">
      <el-dropdown trigger="click" @command="switchPlatform">
        <div class="brand platform-switch">
          {{ activePlatform }}
          <el-icon class="platform-arrow"><arrow-down /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="IAM">IAM</el-dropdown-item>
            <el-dropdown-item command="BIDS">BIDS</el-dropdown-item>
            <el-dropdown-item command="EDM">EDM</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <div class="header-fill"></div>
      <div class="header-meta">
        <el-tag type="info" effect="plain">生产治理控制台</el-tag>
        <span class="time-text">{{ nowText }}</span>
        <el-divider direction="vertical" />
        <div class="user-area" @mouseenter="hoveringUser = true" @mouseleave="hoveringUser = false">
          <span class="time-text user-text">{{ authStore.username || '未登录用户' }}</span>
          <el-button v-show="hoveringUser" link type="danger" @click="logout">退出登录</el-button>
        </div>
      </div>
    </el-header>

    <el-container class="admin-body">
      <el-aside width="220px" class="admin-aside">
        <el-menu :default-active="activeMenu" class="side-menu" @select="onSelectMenu">
          <el-menu-item
            v-for="item in currentMenus"
            :key="item.path"
            :index="item.path"
          >
            {{ item.label }}
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
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { logoutCurrentUser } from '../services/auth'
import { useAuthStore } from '../stores/authStore'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const nowText = ref('')
const timer = ref(0)
const hoveringUser = ref(false)
const activePlatform = ref(localStorage.getItem('iam_active_platform') || 'IAM')

const activeMenu = computed(() => route.meta.menuKey || route.path)

const platformMenus = {
  IAM: [
    { path: '/abac/policy', label: 'ABAC 策略管理' },
    { path: '/abac/check', label: 'ABAC 鉴权校验' },
    { path: '/federation/tenants', label: '租户联邦映射' },
    { path: '/governance/risks', label: '风险与巡检' },
    { path: '/system/timeline', label: '最近操作时间线' }
  ],
  BIDS: [
    { path: '/platform/bids', label: 'BIDS 平台入口' }
  ],
  EDM: [
    { path: '/platform/edm', label: 'EDM 工作台' }
  ]
}

const platformDefaultRoute = {
  IAM: '/abac/policy',
  BIDS: '/platform/bids',
  EDM: '/platform/edm'
}
const bidsFrontendUrl = import.meta.env.VITE_BIDS_FRONTEND_URL || 'http://127.0.0.1:5173/run/run'

const currentMenus = computed(() => platformMenus[activePlatform.value] || platformMenus.IAM)

function updateNow() {
  nowText.value = new Date().toLocaleString('zh-CN', { hour12: false })
}

function onSelectMenu(path) {
  router.push(path)
}

function switchPlatform(platformName) {
  if (platformName === 'BIDS') {
    localStorage.setItem('iam_active_platform', 'BIDS')
    window.location.href = bidsFrontendUrl
    return
  }
  activePlatform.value = platformName
  localStorage.setItem('iam_active_platform', platformName)
  router.push(platformDefaultRoute[platformName] || '/abac/policy')
}

async function logout() {
  await logoutCurrentUser()
  router.replace('/login')
}

onMounted(() => {
  updateNow()
  timer.value = window.setInterval(updateNow, 1000)
  if (route.path === '/' || route.path === '') {
    router.replace(platformDefaultRoute[activePlatform.value] || '/abac/policy')
  }
})

onBeforeUnmount(() => {
  window.clearInterval(timer.value)
})

watch(
  () => route.path,
  (path) => {
    if (path.startsWith('/platform/bids')) {
      activePlatform.value = 'BIDS'
    } else if (path.startsWith('/platform/edm')) {
      activePlatform.value = 'EDM'
    } else {
      activePlatform.value = 'IAM'
    }
    localStorage.setItem('iam_active_platform', activePlatform.value)
  },
  { immediate: true }
)
</script>
