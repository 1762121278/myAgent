import { ref } from 'vue'

// 会话存储
const sessions = ref([])
const currentSession = ref(null)
const messages = ref([])
const uploadedFiles = ref([])
const useStreamingApi = ref(true)

// 从localStorage加载会话（支持从历史备选键恢复并迁移到统一键 chat-sessions）
const loadSessions = () => {
  const candidateKeys = ['chat-sessions', 'sessions', 'chat_sessions', 'chatSessions', 'chat-history']
  let saved = null
  let usedKey = null

  for (const k of candidateKeys) {
    const v = localStorage.getItem(k)
    if (v) {
      saved = v
      usedKey = k
      break
    }
  }

  if (!saved) return

  try {
    const parsed = JSON.parse(saved)
    sessions.value = parsed.map(s => ({
      ...s,
      createdAt: new Date(s.createdAt),
      updatedAt: new Date(s.updatedAt),
      messages: (s.messages || []).map(m => ({
        ...m,
        createdAt: new Date(m.createdAt)
      }))
    }))

    // 如果使用的不是目标键，执行一次迁移保存到 chat-sessions
    if (usedKey && usedKey !== 'chat-sessions') {
      try {
        localStorage.setItem('chat-sessions', JSON.stringify(sessions.value))
        console.warn(`已从 localStorage 键 "${usedKey}" 迁移会话到 "chat-sessions"`)
      } catch (e) {
        console.warn('迁移会话到 chat-sessions 失败:', e)
      }
    }
  } catch (e) {
    console.error('加载会话失败:', e)
  }
}

// 保存会话到localStorage
const saveSessions = () => {
  localStorage.setItem('chat-sessions', JSON.stringify(sessions.value))
}

// 创建新会话
const createSession = () => {
  const session = {
    id: generateId(),
    title: '新会话',
    createdAt: new Date(),
    updatedAt: new Date(),
    messages: [],
    uploadedFiles: []
  }
  sessions.value.unshift(session)
  saveSessions()
  return session
}

// 删除会话
const deleteSession = (sessionId) => {
  const index = sessions.value.findIndex(s => s.id === sessionId)
  if (index > -1) {
    sessions.value.splice(index, 1)
    saveSessions()
    
    if (currentSession.value?.id === sessionId) {
      if (sessions.value.length > 0) {
        switchSession(sessions.value[0])
      } else {
        currentSession.value = null
        messages.value = []
      }
    }
  }
}

// 切换会话
const switchSession = (session) => {
  currentSession.value = session
  messages.value = session.messages || []
  uploadedFiles.value = session.uploadedFiles || []
}

// 添加消息
const addMessage = (role, text) => {
  const message = {
    role,
    text,
    createdAt: new Date()
  }
  
  // 只添加到当前会话的消息列表
  if (currentSession.value) {
    currentSession.value.messages.push(message)
    currentSession.value.updatedAt = new Date()
    
    // 更新标题
    if (currentSession.value.title === '新会话' && role === 'USER') {
      const title = text.trim().slice(0, 30) + (text.length > 30 ? '...' : '')
      currentSession.value.title = title
    }
    
    // 同步到messages视图
    messages.value = [...currentSession.value.messages]
    saveSessions()
  }
  
  return message
}

// 更新消息
const updateMessage = (index, text) => {
  if (currentSession.value && currentSession.value.messages[index]) {
    currentSession.value.messages[index].text = text
    // 同步到messages视图
    messages.value = [...currentSession.value.messages]
    saveSessions()
  }
}

// 添加上传文件
const addUploadedFile = (file) => {
  const fileItem = {
    id: generateId(),
    name: file.name,
    size: file.size,
    type: file.type,
    file: file
  }
  uploadedFiles.value.push(fileItem)
}

// 移除上传文件
const removeUploadedFile = (fileId) => {
  const index = uploadedFiles.value.findIndex(f => f.id === fileId)
  if (index > -1) {
    uploadedFiles.value.splice(index, 1)
  }
}

// 清空上传文件
const clearUploadedFiles = () => {
  uploadedFiles.value = []
}

// 生成唯一ID
const generateId = () => {
  return Date.now().toString(36) + Math.random().toString(36).substr(2)
}

// 格式化时间
const formatTime = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

export {
  sessions,
  currentSession,
  messages,
  uploadedFiles,
  useStreamingApi,
  loadSessions,
  saveSessions,
  createSession,
  deleteSession,
  switchSession,
  addMessage,
  updateMessage,
  addUploadedFile,
  removeUploadedFile,
  clearUploadedFiles,
  formatTime
}
