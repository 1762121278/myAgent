<template>
  <div class="kb-manage page">
    <h2 class="page-title">知识库管理</h2>

    <!-- 上传区域 -->
    <section class="upload-panel">
      <div class="upload-box" @dragover.prevent @drop.prevent="handleDrop">
        <div class="upload-inner">
          <div class="upload-cta">拖放文件到此处或</div>
          <button class="choose-btn" @click="chooseFiles">选择文件</button>
          <div class="upload-note">支持 PDF / DOCX / TXT / JPG / PNG，最多 5MB</div>
        </div>
      </div>

        <div class="upload-controls">
          <label class="select-label">选择知识库：</label>
          <select v-model="selectedKb" class="kb-select">
            <option v-for="kb in kbList" :key="kb.id" :value="kb.id">{{ kb.title }}</option>
          </select>
          <button class="process-btn" @click="startUpload" :disabled="!stagedFiles.length">开始上传处理</button>
          <div class="staged-info" v-if="stagedFiles.length">已选 {{ stagedFiles.length }} 个文件</div>
        </div>

        <!-- 已选文件列表 -->
        <div class="staged-list" v-if="stagedFiles.length">
          <div class="staged-item" v-for="(s, idx) in stagedFiles" :key="s.file.name + s.file.size">
            <div class="staged-meta">
              <div class="file-name">{{ s.file.name }}</div>
              <div class="file-size">{{ humanSize(s.file.size) }}</div>
            </div>
            <div class="staged-actions">
              <div class="progress-bar">
                <div
                  class="progress"
                  :class="{ ready: s.status === 'ready', error: s.status === 'error', done: s.status === 'done', uploading: s.status === 'uploading' }"
                  :style="{ width: (s.status === 'ready' ? '100%' : (s.progress || 0) + '%') }"
                ></div>
              </div>
              <div class="status">{{ s.status }}</div>
              <button class="btn ghost" @click="removeStaged(idx)" v-if="s.status !== 'uploading'">移除</button>
            </div>
          </div>
        </div>
    </section>

    <!-- 知识库列表 -->
    <section class="kb-list">
      <h3 class="section-title">知识库列表</h3>
      <div class="cards">
        <div class="kb-card" v-for="kb in kbList" :key="kb.id">
          <div class="kb-card-header">
            <div class="kb-title">{{ kb.title }}</div>
            <div :class="['kb-status', kb.status]">{{ statusText(kb.status) }}</div>
          </div>
          <div class="kb-desc">{{ kb.description }}</div>
          <div class="kb-meta">
            <div class="meta-item">文档：<strong>{{ kb.docs }}</strong></div>
            <div class="meta-item">索引：<strong>{{ kb.indexed ? '已完成' : '未完成' }}</strong></div>
          </div>
          <div class="kb-actions">
            <button class="btn ghost" @click="openManage(kb.id)">管理</button>
            <button class="btn" @click="testKb(kb.id)">测试</button>
            <button class="btn danger" @click="removeKb(kb.id)">删除</button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const stagedFiles = ref([]) // 每项: { file: File, progress: Number, status: 'ready'|'uploading'|'done'|'error' }
const selectedKb = ref(null)

const kbList = ref([
  { id: 'kb-tech', title: '技术文档库', description: '包含 API 文档、接口说明、架构设计等技术资料。', docs: 124, indexed: true, status: 'active' },
  { id: 'kb-manual', title: '产品手册库', description: '用户手册、安装指南与常见问题，便于客服快速查询。', docs: 58, indexed: true, status: 'active' },
  { id: 'kb-support', title: '客户支持库', description: '客户案例、工单与解决方案，供运维与支持团队使用。', docs: 32, indexed: false, status: 'building' },
  { id: 'kb-legacy', title: '历史归档库', description: '旧版本资料与归档文档，低优先级存取。', docs: 420, indexed: true, status: 'archived' }
])

selectedKb.value = kbList.value[0].id

const chooseFiles = () => {
  const inp = document.createElement('input')
  inp.type = 'file'
  inp.multiple = true
  inp.accept = '.pdf,.doc,.docx,.txt,.jpg,.jpeg,.png'
  inp.onchange = (e) => { 
    const files = Array.from(e.target.files || [])
    stagedFiles.value = files.map(f => ({ file: f, progress: 0, status: 'ready' }))
  }
  inp.click()
}

const handleDrop = (e) => {
  const files = Array.from(e.dataTransfer.files || [])
  stagedFiles.value = files.map(f => ({ file: f, progress: 0, status: 'ready' }))
}

const uploadSingle = (s) => {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    const url = '/rag/upload'
    const fd = new FormData()
    fd.append('file', s.file)

    xhr.open('POST', url)
    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) {
        s.progress = Math.round((e.loaded / e.total) * 100)
      }
    }
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        s.status = 'done'
        try {
          const res = JSON.parse(xhr.responseText)
          resolve(res)
        } catch (e) {
          resolve({})
        }
      } else {
        s.status = 'error'
        reject(new Error('上传失败: ' + xhr.status))
      }
    }
    xhr.onerror = () => { s.status = 'error'; reject(new Error('网络错误')) }
    s.status = 'uploading'
    xhr.send(fd)
  })
}

const startUpload = async () => {
  if (!stagedFiles.value.length) return
  const kb = kbList.value.find(k => k.id === selectedKb.value)
  if (kb) {
    kb.status = 'building'
    kb.indexed = false
  }
  for (const s of stagedFiles.value) {
    try {
      await uploadSingle(s)
      if (kb) kb.docs += 1
    } catch (e) {
      console.error('上传失败', e)
    }
  }
  // 模拟后端索引完成后切换状态
  if (kb) setTimeout(() => { kb.indexed = true; kb.status = 'active' }, 800 + Math.random() * 1600)
  // 清除已完成或出错的项（可按需保留）
  stagedFiles.value = stagedFiles.value.filter(s => s.status === 'uploading')
}

const statusText = (s) => {
  if (s === 'active') return '正常'
  if (s === 'building') return '索引中'
  if (s === 'archived') return '已归档'
  return '未知'
}

const openManage = (id) => {
  alert('打开知识库管理：' + id)
}

const removeStaged = (idx) => {
  stagedFiles.value.splice(idx, 1)
}

const humanSize = (n) => {
  if (!n) return ''
  if (n < 1024) return n + ' B'
  if (n < 1024*1024) return Math.round(n/1024) + ' KB'
  return (n / (1024*1024)).toFixed(1) + ' MB'
}

const testKb = (id) => {
  alert('跳转到数据集测试 / 对话应用的集成页面（示例），知识库：' + id)
}

const removeKb = (id) => {
  if (!confirm('确定删除知识库？此操作不可恢复。')) return
  kbList.value = kbList.value.filter(k => k.id !== id)
}
</script>

<style scoped>
.page { padding: 24px; }
.page-title { font-size: 22px; font-weight: 800; color: #1e3a8a; margin-bottom: 18px }

.upload-panel { background: linear-gradient(180deg,#fff 0,#fbfdff 100%); padding: 18px; border-radius: 14px; box-shadow: 0 6px 20px rgba(16,24,40,0.04); margin-bottom: 22px }
.upload-box { border: 2px dashed #e6eefc; border-radius: 12px; padding: 28px; display:flex; align-items:center; justify-content:center }
.upload-inner { text-align:center }
.upload-cta { font-size: 16px; color: #475569; margin-bottom: 12px }
.choose-btn { background: #3b82f6; color: #fff; border: none; padding: 10px 16px; border-radius: 10px; cursor:pointer }
.upload-note { font-size: 13px; color: #94a3b8; margin-top:10px }

.upload-controls { display:flex; align-items:center; gap:12px; margin-top:12px }
.kb-select { padding:8px 12px; border-radius:8px; border:1px solid #e6eefc }
.process-btn { background:#2563eb; color:#fff; border:none; padding:8px 14px; border-radius:10px; cursor:pointer }
.process-btn:disabled { background:#cbd5e1; cursor:not-allowed }
.staged-info { color:#334155; font-weight:600 }

.staged-list { margin-top:12px; display:flex; flex-direction:column; gap:8px }
.staged-item { display:flex; align-items:center; justify-content:space-between; gap:12px; background:#fff; padding:10px; border-radius:10px; box-shadow:0 6px 18px rgba(15,23,42,0.03) }
.staged-meta { display:flex; gap:12px; align-items:center }
.file-name { font-weight:700; color:#0f172a }
.file-size { font-size:12px; color:#64748b }
.staged-actions { display:flex; align-items:center; gap:12px }
.progress-bar { width:160px; height:8px; background:#f1f5f9; border-radius:8px; overflow:hidden }
.progress { height:100%; background:linear-gradient(90deg,#60a5fa,#3b82f6); width:0 }
.progress.ready { background: linear-gradient(90deg,#34d399,#10b981) }
.progress.done { background: linear-gradient(90deg,#34d399,#10b981) }
.progress.error { background: #e5e7eb }
.progress.uploading { background: linear-gradient(90deg,#60a5fa,#3b82f6) }
.status { font-size:12px; color:#334155; min-width:48px; text-align:center }

.kb-list { margin-top: 10px }
.section-title { font-size:18px; color:#1e293b; margin-bottom:12px }
.cards { display:grid; grid-template-columns: repeat(auto-fill,minmax(260px,1fr)); gap:16px }
.kb-card { background: white; border-radius:12px; padding:16px; box-shadow: 0 6px 20px rgba(99,102,241,0.06); display:flex; flex-direction:column; gap:12px }
.kb-card-header { display:flex; align-items:center; gap:12px }
.kb-title { font-weight:800; color:#0f172a }
.kb-status { margin-left:auto; padding:6px 10px; border-radius:999px; font-weight:700; font-size:12px }
.kb-status.active { background: linear-gradient(90deg,#ecfeff, #dbeafe); color:#0f172a }
.kb-status.building { background: linear-gradient(90deg,#fff7ed,#ffe9c7); color:#92400e }
.kb-status.archived { background:#f1f5f9; color:#64748b }
.kb-desc { color:#475569; font-size:14px }
.kb-meta { display:flex; gap:10px; color:#64748b; font-size:13px }
.kb-actions { display:flex; gap:8px; margin-top:6px }
.btn { padding:8px 12px; border-radius:10px; border:none; cursor:pointer; background:#eef2ff; color:#3730a3; font-weight:700 }
.btn.ghost { background:transparent; border:1px solid #e6eefc }
.btn.danger { background:#fee2e2; color:#b91c1c }

@media (max-width:768px) {
  .cards { grid-template-columns: 1fr }
  .upload-controls { flex-direction:column; align-items:flex-start }
}
</style>
