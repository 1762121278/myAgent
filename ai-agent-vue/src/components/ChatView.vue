<template>
  <div class="chat-view">
    <ChatArea :messages="messages" :loading="loading" />
    <InputArea 
      v-model="inputText"
      :uploaded-files="uploadedFiles"
      :loading="loading"
      :use-streaming="useStreamingApi"
      :selected-model="selectedModel"
      @update:selected-model="updateSelectedModel"
      @send="handleSend"
      @file-select="handleFileSelect"
      @file-remove="handleFileRemove"
      @toggle-streaming="toggleStreaming"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import ChatArea from './ChatArea.vue'
import InputArea from './InputArea.vue'
import { 
  sessions, 
  currentSession, 
  messages, 
  uploadedFiles,
  useStreamingApi,
  loadSessions,
  createSession,
  switchSession,
  addMessage,
  updateMessage,
  addUploadedFile,
  removeUploadedFile,
  clearUploadedFiles
} from '../stores/chat.js'
import { chat, streamChat, chatWithFiles } from '../api/chat.js'

const inputText = ref('')
const loading = ref(false)
const selectedModel = ref(localStorage.getItem('selectedModel') || 'deepseek-v3.2')

const toggleStreaming = () => { useStreamingApi.value = !useStreamingApi.value }
const updateSelectedModel = (m) => { selectedModel.value = m; try{ localStorage.setItem('selectedModel', m) }catch(e){} }

const handleFileSelect = (files) => { for (const f of files) addUploadedFile(f) }
const handleFileRemove = (id) => removeUploadedFile(id)

const handleSend = async () => {
  const text = inputText.value.trim()
  const files = [...uploadedFiles.value]
  // 只保留实际可上传的 File/Blob 对象（localStorage 恢复的元数据可能没有 file 字段）
  const sendFiles = files.map(f => (f && (f.file || f.blob || f))).filter(x => x instanceof Blob)
  if (!text && files.length === 0) return
  if (!currentSession.value) {
    const s = createSession(); switchSession(s); addMessage('AI', '欢迎回来！')
  }

  let userContent = text
  if (files.length > 0) {
    if (userContent) userContent += '\n\n'
    userContent += '已上传文件：\n'
    files.forEach(f => userContent += `- ${f.name}\n`)
  }

  addMessage('USER', userContent)
  inputText.value = ''
  clearUploadedFiles()

  loading.value = true
  addMessage('AI', 'loading')
  let aiIndex = messages.value.length - 1
  const threadId = currentSession.value.id

    try {
      if (sendFiles.length > 0 && !useStreamingApi.value) {
        // 有文件且选择同步接口时，走 multipart 上传（仅实际文件）
        const resp = await chatWithFiles(text, threadId, sendFiles, selectedModel.value)
        updateMessage(aiIndex, resp)
        loading.value = false
      } else if (useStreamingApi.value) {
        let buffer = ''
        await streamChat(text, threadId, (chunk) => {
          if (buffer === '') { buffer = chunk; updateMessage(aiIndex, buffer) }
          else { buffer += chunk; updateMessage(aiIndex, buffer) }
        }, () => { loading.value = false }, (err) => { updateMessage(aiIndex, '处理出错'); loading.value = false }, selectedModel.value, sendFiles)
      } else {
        try {
          const resp = await chat(text, threadId, selectedModel.value)
          updateMessage(aiIndex, resp)
        } catch (e) { updateMessage(aiIndex, '调用接口出错：' + (e.message||e)) }
        finally { loading.value = false }
      }
  } catch (e) {
    console.error(e); addMessage('AI', '发送失败：' + (e.message||e)); loading.value = false
  }
}

// ChatAssistant 负责在入口加载会话并保证初始会话存在，ChatView 不再重复创建会话
onMounted(() => {})
</script>

<style scoped>
.chat-view { display:flex; flex-direction:column; height:100%; min-height:0 }
</style>
