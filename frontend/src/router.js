import { createRouter, createWebHistory } from 'vue-router'
import RunQuery from './views/RunQuery.vue'
import SqlAdmin from './views/SqlAdmin.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'run', component: RunQuery },
    { path: '/admin/sql', name: 'sql-admin', component: SqlAdmin }
  ]
})
