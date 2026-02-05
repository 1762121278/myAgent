<template>
  <header class="header">
    <button class="toggle-btn" @click="$emit('toggle-sidebar')" title="显示/隐藏历史会话">
      <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M4 6h16M4 12h16M4 18h16"/>
      </svg>
    </button>
    
    <div class="brand">
      <div class="logo-wrapper">
        <img src="/icons/airobot.jpg" alt="江澜" class="brand-icon">
        <div class="status-dot"></div>
      </div>
      <div class="brand-text">
        <h1 class="brand-title">江澜</h1>
        <span class="brand-subtitle">智能助手</span>
      </div>
    </div>

    <div class="header-actions">
      <button class="action-btn" title="设置">
        <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="3"/>
          <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"/>
        </svg>
      </button>
    </div>
  </header>
</template>

<script setup>
defineProps({
  sidebarVisible: {
    type: Boolean,
    default: true
  }
})

defineEmits(['toggle-sidebar'])
</script>

<style scoped>
.header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 16px; /* 减小高度 */
  background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%);
  border-bottom: 2px solid #e0f2fe;
  box-shadow: 0 2px 12px rgba(59, 130, 246, 0.08);
}

.toggle-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 3px 12px rgba(59, 130, 246, 0.25);
}

.toggle-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.4);
}

.toggle-btn .icon {
  width: 20px;
  height: 20px;
  color: white;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.logo-wrapper {
  position: relative;
}

.brand-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  object-fit: cover;
  border: 2px solid #3b82f6;
  box-shadow: 0 3px 12px rgba(59, 130, 246, 0.25);
}

.status-dot {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 12px;
  height: 12px;
  background: #10b981;
  border-radius: 50%;
  border: 2px solid white;
}

.brand-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-title {
  font-size: 18px;
  font-weight: 700;
  color: #1e40af;
  margin: 0;
  background: linear-gradient(135deg, #1e40af 0%, #3b82f6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-subtitle {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
  letter-spacing: 0.6px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.action-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border: 2px solid #e0f2fe;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.action-btn:hover {
  background: #f0f9ff;
  border-color: #3b82f6;
  transform: translateY(-2px);
}

.action-btn .icon {
  width: 18px;
  height: 18px;
  color: #3b82f6;
}
</style>
