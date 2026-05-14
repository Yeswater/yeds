<template>
  <el-container class="layout">
    <el-header class="header">
      <el-menu mode="horizontal" router :default-active="route.path" class="menu">
        <el-menu-item index="/">查询执行</el-menu-item>
        <el-menu-item index="/admin/sql">SQL 配置</el-menu-item>
      </el-menu>
    </el-header>
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<style scoped>
.layout {
  min-height: 100vh;
}
.header {
  padding: 0;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}
.menu {
  border-bottom: none;
}
.main {
  padding: 0;
  background: #f5f7fb;
}
</style>
