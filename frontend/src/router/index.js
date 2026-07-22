import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/public/Home.vue') },
  { path: '/cases', name: 'cases', component: () => import('../views/public/Cases.vue') },
  { path: '/centers', name: 'centers', component: () => import('../views/public/Centers.vue') },
  { path: '/case/:id', name: 'case-detail', component: () => import('../views/public/CaseDetail.vue') },
  { path: '/center/:id', name: 'center-profile', component: () => import('../views/public/CenterProfile.vue') },
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  { path: '/profile', name: 'profile', component: () => import('../views/Profile.vue'), meta: { requiresAuth: true } },

  {
    path: '/dashboard',
    component: () => import('../views/dashboard/DashboardLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', name: 'dashboard-home', component: () => import('../views/dashboard/Home.vue') },
      { path: 'cases', name: 'dashboard-cases', component: () => import('../views/dashboard/CenterCases.vue'), meta: { role: 'CENTER' } },
      { path: 'cases/new', name: 'dashboard-new-case', component: () => import('../views/dashboard/NewCase.vue'), meta: { role: 'CENTER' } },
      { path: 'admin/cases', name: 'admin-cases', component: () => import('../views/dashboard/AdminCases.vue'), meta: { role: 'ADMIN' } },
      { path: 'admin/centers', name: 'admin-centers', component: () => import('../views/dashboard/AdminCenters.vue'), meta: { role: 'ADMIN' } },
      { path: 'admin/categories', name: 'admin-categories', component: () => import('../views/dashboard/AdminCategories.vue'), meta: { role: 'ADMIN' } },
      { path: 'admin/locations', name: 'admin-locations', component: () => import('../views/dashboard/AdminLocations.vue'), meta: { role: 'ADMIN' } },
      { path: 'admin/notices', name: 'admin-notices', component: () => import('../views/dashboard/AdminNotices.vue'), meta: { role: 'ADMIN' } }
    ]
  },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('../views/public/NotFound.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.role && auth.role !== to.meta.role) {
    return { name: 'dashboard-home' }
  }
  return true
})

export default router
