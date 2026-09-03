import { createRouter, createWebHistory } from 'vue-router';

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/',          component: () => import('./views/ThisWeek.vue'),   meta: { title: 'สัปดาห์นี้' } },
    { path: '/radar',     component: () => import('./views/Radar.vue'),      meta: { title: 'เรดาร์' } },
    { path: '/portfolio', component: () => import('./views/Portfolio.vue'),  meta: { title: 'พอร์ต' } },
    { path: '/log',       component: () => import('./views/ContentLog.vue'), meta: { title: 'คลิป' } },
    { path: '/:rest(.*)', redirect: '/' },
  ],
});

router.afterEach(to => {
  document.title = `${to.meta.title ?? ''} · Shopee Afl`.trim();
});
