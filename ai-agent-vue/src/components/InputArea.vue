<template>
  <div class="input-area" :class="{ collapsed: isCollapsed }">
    <!-- 已上传文件列表 -->
    <div v-if="uploadedFiles.length > 0" class="files-container">
      <div 
        v-for="file in uploadedFiles" 
        :key="file.id"
        class="file-item"
      >
        <div class="file-icon-wrapper" :class="getFileIconClass(file.name)">
          <svg class="file-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
            <polyline points="10 9 9 9 8 9"/>
          </svg>
        </div>
        <div class="file-info">
          <span class="file-name">{{ file.name }}</span>
          <span class="file-size">{{ formatFileSize(file.size) }}</span>
        </div>
        <button 
          class="file-remove-btn"
          @click="$emit('file-remove', file.id)"
          title="删除文件"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <line x1="18" y1="6" x2="6" y2="18"/>
            <line x1="6" y1="6" x2="18" y2="18"/>
          </svg>
        </button>
      </div>
      <button class="add-file-btn" @click="triggerFileInput" title="添加文件">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <path d="M12 5v14M5 12h14"/>
        </svg>
      </button>
    </div>
    
    <!-- 文本输入框 -->
    <div class="input-wrapper">
      <textarea
        ref="textareaRef"
        v-model="inputValue"
        class="input-textarea"
        placeholder="请输入您的问题，按 Enter 发送，Shift + Enter 换行..."
        rows="3"
        @keydown="handleKeydown"
        @input="handleInput"
      ></textarea>
    </div>
    
    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <!-- 深度思考按钮 - 紫色 -->
        <button class="tool-btn deep-thinking" title="深度思考模式">
          <div class="btn-icon-wrapper purple">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
            </svg>
          </div>
          <span class="btn-label">深度思考</span>
        </button>

        <!-- 模型选择按钮 - 蓝色 带下拉 -->
        <div class="model-dropdown-wrapper">
          <button ref="modelButtonRef" class="tool-btn model-select" @click="toggleModelDropdown" :title="`当前模型：${props.selectedModel}`">
            <div class="btn-icon-wrapper blue">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
                <line x1="8" y1="21" x2="16" y2="21"/>
                <line x1="12" y1="17" x2="12" y2="21"/>
              </svg>
            </div>
            <span class="btn-label">{{ props.selectedModel }}</span>
            <svg class="caret" viewBox="0 0 24 24" width="16" height="16" style="margin-left:6px;">
              <polyline points="6 9 12 15 18 9" fill="none" stroke="currentColor" stroke-width="2"/>
            </svg>
          </button>
          <div v-if="showModelDropdown" class="model-menu" :style="menuStyle">
            <div 
              v-for="m in availableModels" 
              :key="m" 
              class="model-item" 
              :class="{ active: m === props.selectedModel }"
              @click="selectModel(m)"
            >
              {{ m }}
            </div>
          </div>
        </div>

        <!-- 语音输入按钮 - 绿色 -->
        <button class="tool-btn icon-only" title="语音输入">
          <div class="btn-icon-wrapper green">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
              <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
              <line x1="12" y1="19" x2="12" y2="23"/>
              <line x1="8" y1="23" x2="16" y2="23"/>
            </svg>
          </div>
        </button>

        <!-- 上传文档按钮 - 橙色 -->
        <button 
          class="tool-btn icon-only" 
          @click="triggerFileInput"
          title="上传文档 (PDF/DOC/DOCX/TXT, 最大 5MB)"
        >
          <div class="btn-icon-wrapper orange">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="17 8 12 3 7 8"/>
              <line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
          </div>
        </button>

        <!-- 上传图片按钮 - 粉色 -->
        <button 
          class="tool-btn icon-only" 
          @click="triggerImageInput"
          title="上传图片 (JPG/PNG, 最大 2MB)"
        >
          <div class="btn-icon-wrapper pink">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
              <circle cx="8.5" cy="8.5" r="1.5"/>
              <polyline points="21 15 16 10 5 21"/>
            </svg>
          </div>
        </button>

        <!-- API模式切换按钮 - 青色/红色 -->
        <button 
          class="tool-btn icon-only api-mode"
          :class="{ 'streaming': useStreaming }"
          @click="$emit('toggle-streaming')"
          :title="useStreaming ? '当前模式：流式 (点击切换到同步)' : '当前模式：同步 (点击切换到流式)'"
        >
          <div class="btn-icon-wrapper" :class="useStreaming ? 'cyan' : 'red'">
            <svg v-if="useStreaming" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="23 6 13.5 15.5 8.5 10.5 1 18"/>
              <polyline points="17 6 23 6 23 12"/>
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="2" y="2" width="20" height="8" rx="2" ry="2"/>
              <rect x="2" y="14" width="20" height="8" rx="2" ry="2"/>
              <line x1="6" y1="6" x2="6.01" y2="6"/>
              <line x1="6" y1="18" x2="6.01" y2="18"/>
            </svg>
          </div>
        </button>
      </div>
      
      <button 
        class="send-btn"
        :disabled="!canSend || loading"
        @click="handleSend"
      >
        <svg v-if="!loading" class="send-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <line x1="22" y1="2" x2="11" y2="13"/>
          <polygon points="22 2 15 22 11 13 2 9 22 2"/>
        </svg>
        <span v-else class="loading-dots">
          <span></span>
          <span></span>
          <span></span>
        </span>
        <span class="btn-text">{{ loading ? '发送中' : '发送' }}</span>
      </button>
    </div>

    <!-- 收起/展开切换按钮 -->
    <button class="collapse-toggle" @click="toggleCollapse" :title="isCollapsed ? '展开输入区' : '收起输入区'">
      <svg v-if="!isCollapsed" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="6 9 12 15 18 9" />
      </svg>
      <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="6 15 12 9 18 15" />
      </svg>
    </button>
    <!-- 折叠后显示的浮动恢复按钮，确保用户能在页面任意位置恢复输入区 -->
    <button v-if="isCollapsed" class="restore-fab" @click="toggleCollapse" title="展开输入区">
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
        <polyline points="6 15 12 9 18 15"></polyline>
      </svg>
    </button>
    
    <!-- 隐藏的文件输入 -->
    <input
      ref="fileInputRef"
      type="file"
      style="display: none"
      accept=".pdf,.doc,.docx,.txt,.jpg,.jpeg,.png"
      multiple
      @change="handleFileChange"
    />
    <input
      ref="imageInputRef"
      type="file"
      style="display: none"
      accept=".jpg,.jpeg,.png"
      multiple
      @change="handleFileChange"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onBeforeUnmount } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  selectedModel: {
    type: String,
    default: '文心4.5T'
  },
  uploadedFiles: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  useStreaming: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['update:modelValue', 'send', 'file-select', 'file-remove', 'toggle-streaming', 'update:selected-model'])

const textareaRef = ref(null)
const fileInputRef = ref(null)
const imageInputRef = ref(null)
const showModelDropdown = ref(false)
const availableModels = [
  '文心4.5T',
  'deepseek-v3.2',
  'glm-4.7',
  'qianwen-v3.2'
]
const modelButtonRef = ref(null)
const menuStyle = ref({})

// 收起/展开输入区
const isCollapsed = ref(false)
const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
  // 失焦并收起下拉菜单
  showModelDropdown.value = false
  if (!isCollapsed.value) {
    // 展开时聚焦文本框
    nextTick(() => textareaRef.value?.focus())
  }
}

const onOutsideClick = (e) => {
  const wrapper = modelButtonRef.value
  if (!wrapper) return
  const menuEl = document.querySelector('.model-menu')
  if (menuEl && (menuEl.contains(e.target) || wrapper.contains(e.target))) return
  showModelDropdown.value = false
}

// 输入值双向绑定
const inputValue = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

// 是否可以发送
const canSend = computed(() => {
  return inputValue.value.trim().length > 0 || props.uploadedFiles.length > 0
})

// 获取文件图标样式
const getFileIconClass = (filename) => {
  const ext = filename.split('.').pop().toLowerCase()
  if (['pdf'].includes(ext)) return 'pdf'
  if (['doc', 'docx'].includes(ext)) return 'doc'
  if (['jpg', 'jpeg', 'png'].includes(ext)) return 'image'
  return 'default'
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

// 处理键盘事件
const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    if (canSend.value && !props.loading) {
      handleSend()
    }
  }
}

// 处理输入
const handleInput = () => {
  const textarea = textareaRef.value
  if (textarea) {
    textarea.style.height = 'auto'
    const newHeight = Math.min(Math.max(textarea.scrollHeight, 80), 180)
    textarea.style.height = newHeight + 'px'
  }
}

// 触发文件选择
const triggerFileInput = () => {
  fileInputRef.value?.click()
}

// 触发图片选择
const triggerImageInput = () => {
  imageInputRef.value?.click()
}

const toggleModelDropdown = async () => {
  showModelDropdown.value = !showModelDropdown.value
  await nextTick()
  if (showModelDropdown.value && modelButtonRef.value) {
    // compute fixed position based on button
    const rect = modelButtonRef.value.getBoundingClientRect()
    const approxMenuHeight = Math.min(availableModels.length * 40, 300)
    let top = rect.bottom + 8
    // if menu would overflow viewport bottom, open upwards
    if (top + approxMenuHeight > window.innerHeight) {
      top = rect.top - approxMenuHeight - 8
      if (top < 8) top = 8
    }
    menuStyle.value = {
      position: 'fixed',
      left: `${rect.left}px`,
      top: `${top}px`,
      minWidth: `${rect.width}px`
    }
    window.addEventListener('mousedown', onOutsideClick)
    window.addEventListener('resize', onOutsideClick)
  } else {
    window.removeEventListener('mousedown', onOutsideClick)
    window.removeEventListener('resize', onOutsideClick)
  }
}

const selectModel = (m) => {
  emit('update:selected-model', m)
  showModelDropdown.value = false
  window.removeEventListener('mousedown', onOutsideClick)
  window.removeEventListener('resize', onOutsideClick)
}

onBeforeUnmount(() => {
  window.removeEventListener('mousedown', onOutsideClick)
  window.removeEventListener('resize', onOutsideClick)
})

// 处理文件选择
const handleFileChange = (e) => {
  const files = Array.from(e.target.files)
  if (files.length > 0) {
    emit('file-select', files)
  }
  e.target.value = ''
}

// 发送消息
const handleSend = () => {
  if (canSend.value && !props.loading) {
    emit('send')
    if (textareaRef.value) {
      textareaRef.value.style.height = 'auto'
    }
  }
}

// 监听loading变化
watch(() => props.loading, (newVal) => {
  if (!newVal && textareaRef.value) {
    textareaRef.value.focus()
  }
})
</script>

<style scoped>
.input-area {
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border-top: 2px solid #e0f2fe;
  padding: 24px;
  box-shadow: 0 -4px 20px rgba(59, 130, 246, 0.08);
  position: relative;
  transition: all 300ms ease;
}

/* 折叠样式：通过限制内部区域的 max-height 实现平滑收起 */
.files-container,
.input-wrapper,
.toolbar {
  transition: max-height 300ms ease, opacity 200ms ease, transform 300ms ease;
  overflow: hidden;
}

.input-area.collapsed {
  padding: 6px 24px;
}

.input-area.collapsed .input-wrapper,
.input-area.collapsed .toolbar,
.input-area.collapsed .files-container {
  max-height: 0;
  opacity: 0;
  transform: translateY(8px);
  pointer-events: none;
}

.input-area.collapsed .input-wrapper,
.input-area.collapsed .toolbar {
  margin-bottom: 0;
}

/* 文件列表 */
.files-container {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
  padding: 4px 0;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: white;
  border-radius: 14px;
  border: 2px solid #e0f2fe;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.file-item:hover {
  border-color: #3b82f6;
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(59, 130, 246, 0.15);
}

.file-icon-wrapper {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  flex-shrink: 0;
}

.file-icon-wrapper.pdf {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #dc2626;
}

.file-icon-wrapper.doc {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #2563eb;
}

.file-icon-wrapper.image {
  background: linear-gradient(135deg, #fce7f3 0%, #fbcfe8 100%);
  color: #db2777;
}

.file-icon-wrapper.default {
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  color: #6b7280;
}

.file-icon {
  width: 24px;
  height: 24px;
}

.file-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.file-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.file-remove-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fee2e2;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  color: #dc2626;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.file-remove-btn:hover {
  background: #dc2626;
  color: white;
  transform: scale(1.1);
}

.file-remove-btn svg {
  width: 18px;
  height: 18px;
}

.add-file-btn {
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border: 2px dashed #3b82f6;
  border-radius: 14px;
  cursor: pointer;
  color: #3b82f6;
  transition: all 0.3s ease;
}

.add-file-btn:hover {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  transform: scale(1.05);
}

.add-file-btn svg {
  width: 24px;
  height: 24px;
}

/* 输入框 */
.input-wrapper {
  position: relative;
  margin-bottom: 16px;
}

.input-textarea {
  width: 100%;
  min-height: 80px;
  max-height: 180px;
  padding: 20px 24px;
  border: 2px solid #e0f2fe;
  border-radius: 20px;
  font-size: 17px;
  line-height: 1.6;
  resize: none;
  outline: none;
  transition: all 0.3s ease;
  font-family: inherit;
  background: white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.03);
}

.input-textarea:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15), 0 4px 20px rgba(59, 130, 246, 0.1);
}

/* 收起/展开按钮 */
.collapse-toggle {
  position: absolute;
  right: 18px;
  top: 12px;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: white;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  cursor: pointer;
  transition: transform 180ms ease;
  z-index: 40;
}

.collapse-toggle:hover { transform: translateY(-2px); }

.input-area.collapsed .collapse-toggle {
  background: linear-gradient(135deg,#f8fafc,#ffffff);
}

/* 折叠后在页面右下角显示的恢复按钮 (Floating Action Button) */
.restore-fab {
  position: fixed;
  right: 28px;
  bottom: 28px;
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
  color: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 30px rgba(99,102,241,0.25);
  cursor: pointer;
  z-index: 60;
  transition: transform 160ms ease;
}

.restore-fab:hover { transform: translateY(-4px); }

.input-textarea::placeholder {
  color: #94a3b8;
  font-size: 16px;
}

/* 工具栏 */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.tool-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: white;
  border: 2px solid #e2e8f0;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tool-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.08);
}

.tool-btn.icon-only {
  padding: 10px;
}

.model-dropdown-wrapper {
  position: relative;
}

.model-menu {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 6px 20px rgba(0,0,0,0.08);
  min-width: 160px;
  z-index: 30;
  overflow: hidden;
}

.model-item {
  padding: 8px 12px;
  cursor: pointer;
  font-weight: 600;
  color: #374151;
}

.model-item:hover {
  background: #f8fafc;
}

.model-item.active {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1e40af;
}

.btn-icon-wrapper {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  transition: all 0.3s ease;
}

.btn-icon-wrapper svg {
  width: 20px;
  height: 20px;
}

/* 彩色按钮样式 */
.btn-icon-wrapper.purple {
  background: linear-gradient(135deg, #e9d5ff 0%, #d8b4fe 100%);
  color: #7c3aed;
}

.btn-icon-wrapper.blue {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #2563eb;
}

.btn-icon-wrapper.green {
  background: linear-gradient(135deg, #dcfce7 0%, #bbf7d0 100%);
  color: #16a34a;
}

.btn-icon-wrapper.orange {
  background: linear-gradient(135deg, #ffedd5 0%, #fed7aa 100%);
  color: #ea580c;
}

.btn-icon-wrapper.pink {
  background: linear-gradient(135deg, #fce7f3 0%, #fbcfe8 100%);
  color: #db2777;
}

.btn-icon-wrapper.cyan {
  background: linear-gradient(135deg, #cffafe 0%, #a5f3fc 100%);
  color: #0891b2;
}

.btn-icon-wrapper.red {
  background: linear-gradient(135deg, #fee2e2 0%, #fecaca 100%);
  color: #dc2626;
}

.tool-btn:hover .btn-icon-wrapper {
  transform: scale(1.1);
}

.btn-label {
  font-weight: 600;
}

/* 发送按钮 */
.send-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 28px;
  background: linear-gradient(135deg, #3b82f6 0%, #6366f1 100%);
  color: white;
  border: none;
  border-radius: 16px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.35);
}

.send-btn:hover:not(:disabled) {
  background: linear-gradient(135deg, #2563eb 0%, #4f46e5 100%);
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(59, 130, 246, 0.45);
}

.send-btn:active:not(:disabled) {
  transform: translateY(-1px);
}

.send-btn:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.send-icon {
  width: 20px;
  height: 20px;
}

.btn-text {
  font-weight: 700;
}

/* 加载动画 */
.loading-dots {
  display: flex;
  gap: 4px;
}

.loading-dots span {
  width: 6px;
  height: 6px;
  background: white;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  
  .toolbar-left {
    justify-content: center;
  }
  
  .send-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
