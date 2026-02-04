<template>
  <div class="chat-assistant" :class="{ 'mobile': isMobile }">
    <!-- 侧边栏 -->
    <Sidebar 
      :visible="sidebarVisible"
      :sessions="sessions"
      :current-session="currentSession"
      @toggle="toggleSidebar"
      @new-session="handleNewSession"
      @select-session="handleSelectSession"
      @delete-session="handleDeleteSession"
    />
    
    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 顶部标题栏 -->
      <Header 
        :sidebar-visible="sidebarVisible"
        @toggle-sidebar="toggleSidebar"
      />
      
      <!-- 路由视图：ChatView / KbManage / DatasetTest / DialogApp -->
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  sessions,
  currentSession,
  loadSessions,
  createSession,
  switchSession,
  deleteSession,
  formatTime
} from '../stores/chat.js'
import Sidebar from './Sidebar.vue'
import Header from './Header.vue'

// 响应式状态
const sidebarVisible = ref(true)
const windowWidth = ref(window.innerWidth)

// 计算属性
const isMobile = computed(() => windowWidth.value < 768)

const handleResize = () => {
  windowWidth.value = window.innerWidth
  if (isMobile.value) sidebarVisible.value = false
}

const toggleSidebar = () => { sidebarVisible.value = !sidebarVisible.value }

onMounted(() => {
  window.addEventListener('resize', handleResize)
  if (isMobile.value) sidebarVisible.value = false
})

// 加载会话并保证初始会话存在，Sidebar 需要这些数据
const router = useRouter()

onMounted(() => {
  try {
    loadSessions()
    if (sessions.value.length > 0) {
      switchSession(sessions.value[0])
    } else {
      const s = createSession()
      switchSession(s)
    }
  } catch (e) {
    console.error('会话加载失败:', e)
  }
})

const handleNewSession = () => {
  const s = createSession()
  switchSession(s)
}

const handleSelectSession = (s) => {
  switchSession(s)
  try { router.push('/') } catch (e) { console.warn('导航到聊天页失败', e) }
}

const handleDeleteSession = (id) => {
  deleteSession(id)
}
</script>

<style scoped>
.chat-assistant {
  display: flex;
  height: 100vh;
  background-color: #f8fafc;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}
</style>
