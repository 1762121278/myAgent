import { createRouter, createWebHistory } from 'vue-router'
import ChatView from '../components/ChatView.vue'
import KbManage from '../components/KbManage.vue'
import DatasetTest from '../components/DatasetTest.vue'
import DialogApp from '../components/DialogApp.vue'

const routes = [
  { path: '/', name: 'Chat', component: ChatView },
  { path: '/kb/manage', name: 'KbManage', component: KbManage },
  { path: '/kb/dataset', name: 'DatasetTest', component: DatasetTest },
  { path: '/kb/dialog', name: 'DialogApp', component: DialogApp }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
