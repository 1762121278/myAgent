<template>
  <div class="chat-area" ref="chatContainer">
    <div class="messages-wrapper">
      <div
        v-for="(message, index) in messages"
        :key="index"
        class="message-row"
        :class="message.role.toLowerCase()"
      >
        <!-- 头像（非卡通简洁标识） -->
        <div v-if="message.role.toLowerCase() === 'ai'" class="avatar ai" title="AI"><span class="avatar-label">AI</span></div>

        <div class="message-bubble">
          <div class="message-content">
            <div v-if="message.text === 'loading'" class="loading-message">
              <div class="loading-spinner"></div>
              <span class="loading-text">正在思考中...</span>
            </div>
            <div v-else class="markdown-content" v-html="renderMarkdown(message.text)"></div>
          </div>

          <div class="message-footer">
            <span class="message-time">{{ formatTime(message.createdAt) }}</span>
            <button
              v-if="message.text !== 'loading'"
              class="copy-btn"
              @click="copyMessage(message.text, index)"
              title="复制此消息"
            >
              <svg v-if="copiedIndex !== index" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="9" y="9" width="13" height="13" rx="2" ry="2"/>
                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            </button>
          </div>
        </div>

        <div v-if="message.role.toLowerCase() === 'user'" class="avatar user" title="You"><span class="avatar-label">你</span></div>
      </div>

      <!-- 居中微妙的 AI 加载标识（当 AI 返回 loading 占位消息时显示） -->
      <div v-if="isAiLoading" class="ai-loading-center">
        <div class="ai-loading-badge">AI 正在思考...</div>
      </div>
    </div>

    <!-- 悬浮到最新按钮（固定在视口，始终可见） -->
    <button v-show="showScrollBtn" class="scroll-to-bottom-btn" @click.prevent="scrollToBottom()" title="跳到最新消息">
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="6 9 12 15 18 9" />
      </svg>
    </button>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, computed, onMounted, onBeforeUnmount } from 'vue'
import { renderMarkdown } from '../utils/markdown.js'
import { formatTime } from '../stores/chat.js'

const props = defineProps({
  messages: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const chatContainer = ref(null)
const copiedIndex = ref(-1)
const isAtBottom = ref(true)
const showScrollBtn = ref(false)

// 是否存在 AI 正在思考的占位消息
const isAiLoading = computed(() => {
  return props.messages.some(m => (String(m.role).toUpperCase() === 'AI' || String(m.role).toLowerCase()==='ai') && m.text === 'loading')
})

// 复制消息
const copyMessage = async (text, index) => {
  try {
    await navigator.clipboard.writeText(text)
    copiedIndex.value = index
    setTimeout(() => {
      copiedIndex.value = -1
    }, 2000)
  } catch (err) {
    console.error('复制失败:', err)
  }
}

// 平滑滚动到底部（并重置用户滚动状态）
const scrollToBottom = (smooth = true) => {
  nextTick(() => {
    if (!chatContainer.value) return
    const el = chatContainer.value
    if (smooth && 'scrollTo' in el) {
      el.scrollTo({ top: el.scrollHeight, behavior: 'smooth' })
    } else {
      el.scrollTop = el.scrollHeight
    }
    // 视为已回到底部
    isAtBottom.value = true
    showScrollBtn.value = false
  })
}

// 监听消息变化，只有当用户在底部（或接近底部）时才自动滚动，避免打断历史查看
watch(() => props.messages, () => {
  if (isAtBottom.value) scrollToBottom(true)
}, { deep: true })

// 监听滚动以判断是否在底部，控制浮动按钮显示
const onScroll = () => {
  const el = chatContainer.value
  if (!el) return
  const threshold = 120 // px
  const atBottom = (el.scrollHeight - el.scrollTop - el.clientHeight) <= threshold
  isAtBottom.value = atBottom
  showScrollBtn.value = !atBottom
}

onMounted(() => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.addEventListener('scroll', onScroll, { passive: true })
      // 初始化按钮显示
      onScroll()
    }
  })
})

onBeforeUnmount(() => {
  if (chatContainer.value) chatContainer.value.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
.chat-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px 20px 24px 24px;
  background: linear-gradient(180deg, #f0f9ff 0%, #e0f2fe 100%);
  position: relative;
}

.messages-wrapper {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
  position: relative; /* 使内部绝对定位相对于消息区域 */
}

.message-row {
  display: flex;
  width: 100%;
  animation: fadeIn 0.3s ease;
  align-items: center;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-row.user {
  justify-content: flex-end;
  padding-left: 5px;
}

.message-row.ai {
  justify-content: flex-start;
  padding-right: 5px;
}

/* 头像样式 */
.avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(16,24,40,0.08);
}

.avatar.ai {
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 100%);
  color: #1e293b;
  margin-right: 12px;
  border: 1px solid rgba(99,102,241,0.12);
}

.avatar.user {
  background: linear-gradient(135deg, #fff7ed 0%, #fff1f2 100%);
  color: #1e293b;
  margin-left: 12px;
  border: 1px solid rgba(249,115,22,0.08);
}

.avatar-label {
  font-weight: 700;
  font-size: 13px;
  line-height: 1;
}

.message-bubble {
  max-width: 78%;
  min-width: 160px;
  padding: 18px 20px;
  border-radius: 28px;
  position: relative;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.message-row.user .message-bubble {
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
  color: white;
  border-bottom-right-radius: 12px;
}

.message-row.ai .message-bubble {
  background: white;
  color: #1e293b;
  border-bottom-left-radius: 12px;
}

.message-content {
  font-size: 26px;
  line-height: 1.8;
  word-wrap: break-word;
}

.message-row.user .message-content {
  color: white;
}

.message-row.ai .message-content {
  color: #1e293b;
}

.message-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  padding-top: 4px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  min-height: 20px;
}

.message-row.user .message-footer {
  border-top-color: rgba(255, 255, 255, 0.2);
}

.message-time {
  font-size: 11px;
  opacity: 0.75;
  font-weight: 500;
}

.copy-btn {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.copy-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.1);
}

.copy-btn svg {
  width: 12px;
  height: 12px;
  color: inherit;
}

.message-row.ai .copy-btn {
  background: #f1f5f9;
}

.message-row.ai .copy-btn:hover {
  background: #e2e8f0;
}

/* 悬浮跳转到最新消息按钮 */
.scroll-to-bottom-btn {
  /* 固定在视口中间右侧，始终可见（不随消息滚动消失） */
  position: fixed;
  right: 24px;
  top: 50%;
  transform: translateY(-50%);
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: white;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 6px 20px rgba(2,6,23,0.08);
  cursor: pointer;
  z-index: 160;
  transition: transform 160ms ease, opacity 160ms ease;
}
.scroll-to-bottom-btn:hover { transform: translateY(-4px); }

/* AI 居中加载标识，尽量不显眼 */
.ai-loading-center {
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  justify-content: center;
  pointer-events: none;
  z-index: 40;
}
.ai-loading-badge {
  background: rgba(99,102,241,0.06);
  color: #334155;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 13px;
  box-shadow: 0 4px 20px rgba(2,6,23,0.03);
  opacity: 0.95;
}

/* 加载动画 */
.loading-message {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0;
}

.loading-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid #e0f2fe;
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-text {
  font-size: 15px;
  color: #3b82f6;
  font-weight: 500;
}

/* Markdown样式 */
:deep(.markdown-content) {
  line-height: 1.8;
  font-size: 18px;
}

/* 自定义滚动条：更粗并略微内缩，保留右侧留白视觉 */
:deep(.chat-area)::-webkit-scrollbar {
  width: 12px;
}
:deep(.chat-area)::-webkit-scrollbar-track {
  background: transparent;
  border-radius: 8px;
}
:deep(.chat-area)::-webkit-scrollbar-thumb {
  background: linear-gradient(180deg, #7c3aed 0%, #4f46e5 100%);
  border-radius: 8px;
  border: 3px solid rgba(255,255,255,0.0);
  background-clip: padding-box;
}

/* Firefox */
:deep(.chat-area) {
  scrollbar-width: thin;
  scrollbar-color: #6366f1 transparent;
}

:deep(.markdown-content p) {
  margin: 0 0 14px 0;
}

:deep(.markdown-content p:last-child) {
  margin-bottom: 0;
}

:deep(.markdown-content pre) {
  /* 使用浅灰色背景以提高整体视觉舒适度 */
  background: #f3f4f6;
  border-radius: 12px;
  padding: 18px;
  overflow-x: auto;
  margin: 16px 0;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
}

:deep(.markdown-content code) {
  font-family: 'JetBrains Mono', 'Monaco', 'Menlo', monospace;
  font-size: 14px;
}

:deep(.markdown-content pre code) {
  color: #0f172a; /* 深色文本，适配浅灰背景 */
  background: transparent;
  padding: 0;
}

/* 提高深色代码块中所有语法高亮颜色的可读性：
   - 统一主色为浅灰白，保证对比
   - 对内层 token 元素采用继承并加上轻微文字阴影以增强在深色背景下的可见性
   - 如果希望保留多彩语法高亮，可调整下面的颜色映射或去掉 "color: inherit !important" 规则
*/
/* 为浅背景设置合适的 token 基准颜色（保留多彩语法高亮的可能性） */
:deep(.markdown-content pre) .hljs-keyword,
:deep(.markdown-content pre) .hljs-built_in,
:deep(.markdown-content pre) .hljs-title,
:deep(.markdown-content pre) .token.keyword,
:deep(.markdown-content pre) .token.function {
  color: #1e40af !important; /* 深蓝，用于关键词/函数，便于阅读 */
}

/* 高亮姓名：彩色渐变文字效果 */
:deep(.markdown-content .highlight-name) {
  display: inline-block;
  font-weight: 700;
  background: linear-gradient(90deg, #f97316, #ef4444, #8b5cf6);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

:deep(.markdown-content :not(pre) code) {
  background: linear-gradient(135deg, #e0f2fe 0%, #dbeafe 100%);
  color: #1e40af;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
}

:deep(.markdown-content ul),
:deep(.markdown-content ol) {
  margin: 14px 0;
  padding-left: 28px;
}

:deep(.markdown-content li) {
  margin: 6px 0;
}

:deep(.markdown-content h1),
:deep(.markdown-content h2),
:deep(.markdown-content h3),
:deep(.markdown-content h4) {
  margin: 20px 0 14px 0;
  font-weight: 700;
  color: #1e40af;
}

:deep(.markdown-content h1) {
  font-size: 26px;
}

:deep(.markdown-content h2) {
  font-size: 22px;
}

:deep(.markdown-content h3) {
  font-size: 18px;
}

:deep(.markdown-content blockquote) {
  border-left: 4px solid #3b82f6;
  padding-left: 20px;
  margin: 16px 0;
  color: #475569;
  font-style: italic;
  background: #f8fafc;
  padding: 16px 20px;
  border-radius: 0 12px 12px 0;
}

:deep(.markdown-content table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

:deep(.markdown-content th),
:deep(.markdown-content td) {
  border: 1px solid #e2e8f0;
  padding: 12px 16px;
  text-align: left;
}

:deep(.markdown-content th) {
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
  color: white;
  font-weight: 600;
}

:deep(.markdown-content tr:nth-child(even)) {
  background: #f8fafc;
}

:deep(.markdown-content a) {
  color: #3b82f6;
  text-decoration: none;
  font-weight: 600;
  border-bottom: 2px solid transparent;
  transition: all 0.3s ease;
}

:deep(.markdown-content a:hover) {
  border-bottom-color: #3b82f6;
}

:deep(.markdown-content hr) {
  border: none;
  height: 2px;
  background: linear-gradient(90deg, transparent 0%, #e0f2fe 50%, transparent 100%);
  margin: 24px 0;
}
</style>
