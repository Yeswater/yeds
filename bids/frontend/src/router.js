import { createRouter, createWebHistory } from 'vue-router'
import { isSessionExpired, readSession } from './authStorage.js'
import { refreshSessionIfNeeded } from './iamSession.js'
import AdminLayout from './layouts/AdminLayout.vue'
import RunQuery from './views/RunQuery.vue'
import IamRedirect from './views/IamRedirect.vue'
import IamCallback from './views/IamCallback.vue'

export const router = createRouter({
  history: createWebHistory('/bids/'),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: IamRedirect,
      meta: { public: true }
    },
    {
      path: '/auth/iam/callback',
      name: 'iam-callback',
      component: IamCallback,
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

router.beforeEach(async (to) => {
  const session = readSession()
  const available = session && !isSessionExpired(session)
  if (to.meta.public) {
    if (available && to.name === 'login') {
      return { path: '/run/svc' }
    }
    return true
  }
  if (!session) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  try {
    await refreshSessionIfNeeded(session, !available)
  } catch {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})
