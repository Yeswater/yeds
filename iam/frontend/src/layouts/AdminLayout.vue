<template>
  <el-container class="yeds-admin-root admin-root">
    <YedsAdminHeader
      :brand-label="activePlatform"
      :platform-options="platformOptions"
      :username="authStore.username || ''"
      :avatar-text="avatarText"
      @platform-command="switchPlatform"
      @user-command="onUserCommand"
    />

    <el-container class="yeds-admin-body admin-body">
      <el-aside class="yeds-admin-aside admin-aside">
        <el-menu
          :default-active="activeMenu"
          class="yeds-admin-side-menu side-menu"
          @select="onSelectMenu"
        >
          <el-menu-item v-for="item in currentMenus" :key="item.path" :index="item.path">
            {{ item.label }}
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
import { logoutCurrentUser } from '../services/auth'
import { switchToBids } from '../platformSwitch'
import { useAuthStore } from '../stores/authStore'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const activePlatform = ref(localStorage.getItem('iam_active_platform') || 'IAM')

const activeMenu = computed(() => route.meta.menuKey || route.path)

const platformOptions = [
  { command: 'IAM', label: 'IAM' },
  { command: 'BIDS', label: 'BIDS' },
  { command: 'EDM', label: 'EDM' }
]

const avatarText = computed(() => {
  const name = authStore.username || ''
  if (!name) {
    return '?'
  }
  return name.slice(0, 1).toUpperCase()
})

const platformMenus = {
  IAM: [
    { path: '/abac/policy', label: 'ABAC 策略管理' },
    { path: '/abac/check', label: 'ABAC 鉴权校验' },
    { path: '/federation/tenants', label: '租户联邦映射' },
    { path: '/governance/risks', label: '风险与巡检' },
    { path: '/system/timeline', label: '最近操作时间线' }
  ],
  BIDS: [{ path: '/platform/bids', label: 'BIDS 平台入口' }],
  EDM: [{ path: '/platform/edm', label: 'EDM 工作台' }]
}

const platformDefaultRoute = {
  IAM: '/abac/policy',
  BIDS: '/platform/bids',
  EDM: '/platform/edm'
}
const BIDS_ENTRY_PATH = '/run/svc'

const currentMenus = computed(() => platformMenus[activePlatform.value] || platformMenus.IAM)

function onSelectMenu(path) {
  router.push(path)
}

function switchPlatform(platformName) {
  if (platformName === 'BIDS') {
    localStorage.setItem('iam_active_platform', 'BIDS')
    switchToBids(BIDS_ENTRY_PATH)
    return
  }
  activePlatform.value = platformName
  localStorage.setItem('iam_active_platform', platformName)
  router.push(platformDefaultRoute[platformName] || '/abac/policy')
}

async function onUserCommand(command) {
  if (command === 'logout') {
    await logoutCurrentUser()
    router.replace('/login')
  }
}

onMounted(() => {
  if (route.path === '/' || route.path === '') {
    router.replace(platformDefaultRoute[activePlatform.value] || '/abac/policy')
  }
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
