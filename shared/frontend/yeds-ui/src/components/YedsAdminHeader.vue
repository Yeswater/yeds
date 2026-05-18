<template>
  <el-header class="yeds-admin-header admin-header">
    <el-dropdown trigger="click" @command="(command) => emit('platform-command', command)">
      <div class="yeds-admin-brand-trigger">
        <span class="yeds-admin-brand">{{ brandLabel }}</span>
        <YedsDropdownCaret />
      </div>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item
            v-for="item in platformOptions"
            :key="item.command"
            :command="item.command"
          >
            {{ item.label }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <div class="yeds-admin-header-fill" />

    <div class="yeds-admin-header-actions">
      <el-tag v-if="consoleTag" type="info" effect="plain" size="small">{{ consoleTag }}</el-tag>
      <span v-if="showClock" class="yeds-admin-clock">{{ clockText }}</span>
      <el-divider v-if="showClock && (showThemeToggle || username)" direction="vertical" />
      <template v-if="showThemeToggle">
        <span class="yeds-admin-theme-label">主题</span>
        <el-switch
          :model-value="darkMode"
          inline-prompt
          active-text="暗"
          inactive-text="亮"
          style="--el-switch-on-color: #409eff; --el-switch-off-color: #dcdfe6"
          @update:model-value="(value) => emit('update:darkMode', value)"
        />
      </template>
      <el-dropdown trigger="click" @command="(command) => emit('user-command', command)">
        <span class="yeds-admin-user-trigger">
          <el-avatar :size="32" class="yeds-admin-user-avatar">{{ avatarText }}</el-avatar>
          <span class="yeds-admin-user-name">{{ username || '未登录用户' }}</span>
          <YedsDropdownCaret />
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>
</template>

<script setup>
import YedsDropdownCaret from './YedsDropdownCaret.vue'

defineProps({
  brandLabel: {
    type: String,
    required: true
  },
  platformOptions: {
    type: Array,
    default: () => []
  },
  username: {
    type: String,
    default: ''
  },
  avatarText: {
    type: String,
    default: '?'
  },
  consoleTag: {
    type: String,
    default: ''
  },
  showClock: {
    type: Boolean,
    default: false
  },
  clockText: {
    type: String,
    default: ''
  },
  showThemeToggle: {
    type: Boolean,
    default: false
  },
  darkMode: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['platform-command', 'user-command', 'update:darkMode'])
</script>

<style scoped>
.yeds-admin-theme-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.yeds-admin-clock {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.yeds-admin-user-avatar {
  flex-shrink: 0;
  background: var(--el-color-primary-light-7);
  color: var(--el-color-primary);
  font-weight: 600;
}

.yeds-admin-header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
