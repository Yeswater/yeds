import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '../stores/authStore'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginRedirectView.vue')
  },
  {
    path: '/auth/sso-callback',
    name: 'sso-callback',
    component: () => import('../views/SsoCallbackView.vue')
  },
  {
    path: '/',
    component: () => import('../layouts/AdminLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        redirect: '/abac/policy'
      },
      {
        path: '/abac/policy',
        name: 'abac-policy',
        component: () => import('../views/abac/AbacPolicyView.vue'),
        meta: { menuKey: '/abac/policy' }
      },
      {
        path: '/abac/check',
        name: 'abac-check',
        component: () => import('../views/abac/AbacCheckView.vue'),
        meta: { menuKey: '/abac/check' }
      },
      {
        path: '/federation/tenants',
        name: 'federation-tenants',
        component: () => import('../views/federation/TenantFederationView.vue'),
        meta: { menuKey: '/federation/tenants' }
      },
      {
        path: '/governance/risks',
        name: 'governance-risks',
        component: () => import('../views/governance/GovernanceView.vue'),
        meta: { menuKey: '/governance/risks' }
      },
      {
        path: '/system/response',
        redirect: '/system/timeline'
      },
      {
        path: '/system/timeline',
        name: 'system-timeline',
        component: () => import('../views/system/TimelineView.vue'),
        meta: { menuKey: '/system/timeline' }
      },
      {
        path: '/platform/bids',
        name: 'platform-bids',
        component: () => import('../views/platform/BidsEntryView.vue'),
        meta: {
          menuKey: '/platform/bids',
          platform: 'BIDS',
          title: 'BIDS 工作台',
          description: 'BIDS 平台统一入口'
        }
      },
      {
        path: '/platform/edm',
        name: 'platform-edm',
        component: () => import('../views/platform/PlatformWorkspaceView.vue'),
        meta: {
          menuKey: '/platform/edm',
          platform: 'EDM',
          title: 'EDM 工作台',
          description: '这里将接入 EDM 平台注册组件与菜单。'
        }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }
  if (to.path === '/login' && isLoggedIn()) {
    const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : '/abac/policy'
    return { path: redirect.startsWith('/') && !redirect.startsWith('//') ? redirect : '/abac/policy' }
  }
  return true
})

export default router
