import { createRouter, createWebHistory } from 'vue-router'
import RouteListView from '../views/RouteListView.vue'
import RouteDetailView from '../views/RouteDetailView.vue'
import UpstreamListView from '../views/UpstreamListView.vue'
import CaddyPreviewView from '../views/CaddyPreviewView.vue'

const router = createRouter({
  history: createWebHistory('/alb/'),
  routes: [
    { path: '/', redirect: '/routes' },
    { path: '/routes', component: RouteListView },
    { path: '/routes/:id', component: RouteDetailView, props: true },
    { path: '/upstreams', component: UpstreamListView },
    { path: '/caddy', component: CaddyPreviewView }
  ]
})

export default router
