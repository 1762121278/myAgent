<template>
  <aside class="sidebar" :class="{ 'hidden': !visible }">
    <div class="sidebar-content">
      <button class="new-session-btn" @click="$emit('new-session')">
        <svg class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <path d="M12 5v14M5 12h14"/>
        </svg>
        <span class="btn-text">新建会话</span>
      </button>
      
      <!-- 历史会话 折叠区 -->
      <div class="menu-group">
        <div class="menu-title" @click="historyOpen = !historyOpen">
          <div class="menu-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 3v6h6"/>
              <path d="M21 21v-6h-6"/>
            </svg>
          </div>
          <div class="menu-text">历史会话</div>
          <div class="menu-caret">{{ historyOpen ? '▾' : '▸' }}</div>
        </div>

        <div v-show="historyOpen" class="session-list">
          <div 
            v-for="session in sessions" 
            :key="session.id"
            class="session-item"
            :class="{ 'active': currentSession?.id === session.id }"
            @click="$emit('select-session', session)"
          >
            <div class="session-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
            </div>
            <div class="session-info">
              <div class="session-title">{{ session.title }}</div>
              <div class="session-time">{{ formatTime(session.updatedAt) }}</div>
            </div>
            <button 
              class="delete-btn"
              @click.stop="$emit('delete-session', session.id)"
              title="删除此会话"
            >
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M3 6h18M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
              </svg>
            </button>
          </div>
        </div>
      </div>

      <!-- 知识库 菜单 -->
      <div class="menu-group kb-group">
        <div class="menu-title" @click="kbOpen = !kbOpen">
          <div class="menu-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 2l7 4v6c0 5-4 9-7 9s-7-4-7-9V6l7-4z"/>
            </svg>
          </div>
          <div class="menu-text">知识库</div>
          <div class="menu-caret">{{ kbOpen ? '▾' : '▸' }}</div>
        </div>
        <div v-show="kbOpen" class="kb-submenu">
          <router-link class="kb-item" to="/kb/manage">知识库管理</router-link>
          <router-link class="kb-item" to="/kb/dataset">数据集测试</router-link>
          <router-link class="kb-item" to="/kb/dialog">对话应用</router-link>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { formatTime } from '../stores/chat.js'

import { ref } from 'vue'

defineProps({
  visible: {
    type: Boolean,
    default: true
  },
  sessions: {
    type: Array,
    default: () => []
  },
  currentSession: {
    type: Object,
    default: null
  }
})

defineEmits(['toggle', 'new-session', 'select-session', 'delete-session'])

const historyOpen = ref(true)
const kbOpen = ref(false)
</script>

<style scoped>
.sidebar {
  width: 320px;
  min-width: 280px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border-right: 2px solid #e0f2fe;
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
  box-shadow: 4px 0 20px rgba(59, 130, 246, 0.08);
}

.sidebar.hidden {
  width: 0;
  min-width: 0;
  overflow: hidden;
  border-right: none;
}

.sidebar-content {
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
  overflow: hidden;
}

.new-session-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 16px 24px;
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
  color: white;
  border: none;
  border-radius: 16px;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.35);
}

.new-session-btn:hover {
  background: linear-gradient(135deg, #2563eb 0%, #4f46e5 100%);
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.45);
}

.new-session-btn:active {
  transform: translateY(-1px);
}

.btn-icon {
  width: 24px;
  height: 24px;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: white;
  border: 2px solid transparent;
}

.session-item:hover {
  background: #f0f9ff;
  border-color: #bae6fd;
  transform: translateX(4px);
}

.session-item.active {
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
  border-color: #3b82f6;
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.2);
}

.session-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e0f2fe 0%, #dbeafe 100%);
  border-radius: 12px;
  color: #3b82f6;
  flex-shrink: 0;
}

.session-icon svg {
  width: 22px;
  height: 22px;
}

.session-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.session-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-time {
  font-size: 13px;
  color: #64748b;
  font-weight: 500;
}

.delete-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border: 2px solid #fee2e2;
  border-radius: 10px;
  cursor: pointer;
  color: #ef4444;
  opacity: 0;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.delete-btn svg {
  width: 18px;
  height: 18px;
}

.session-item:hover .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  background: #fee2e2;
  border-color: #ef4444;
  transform: scale(1.1);
}

/* 菜单组 */
.menu-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.menu-title {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  cursor: pointer;
  user-select: none;
  color: #1e293b;
}

.menu-title:hover {
  background: #f8fafc;
}

.menu-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  color: #2563eb;
  flex-shrink: 0;
}

.menu-text {
  font-weight: 700;
}

.menu-caret {
  margin-left: auto;
  color: #94a3b8;
}

.kb-submenu {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-left: 52px;
}

.kb-item {
  padding: 8px 10px;
  border-radius: 10px;
  cursor: pointer;
  color: #374151;
  font-weight: 600;
}

.kb-item:hover {
  background: #f8fafc;
}
</style>
