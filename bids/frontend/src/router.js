import { createRouter, createWebHistory } from 'vue-router'
import { readSession } from './authStorage.js'
import AdminLayout from './layouts/AdminLayout.vue'
import RunQuery from './views/RunQuery.vue'
import Login from './views/Login.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: Login,
      meta: { public: true }
    },
    { path: '/admin/sql', redirect: '/run/svc' },
    { path: '/query', redirect: '/run/svc' },
    { path: '/sql', redirect: '/run/svc' },
    {
      path: '/',
      component: AdminLayout,
      redirect: '/run/svc',
      children: [
        { path: 'run/ds', name: 'console-ds', component: RunQuery, meta: { menu: 'ds' } },
        { path: 'run/svc', name: 'console-svc', component: RunQuery, meta: { menu: 'svc' } },
        { path: 'run/run', name: 'console-run', component: RunQuery, meta: { menu: 'run' } }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/run/svc' }
  ]
})

router.beforeEach((to) => {
  const session = readSession()
  if (to.meta.public) {
    if (session && to.name === 'login') {
      return { path: '/run/svc' }
    }
    return true
  }
  if (!session) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})
