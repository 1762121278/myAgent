// AI聊天API接口
const API_BASE_URL = '/aiAgent'

// 同步聊天接口
export const chat = async (query, threadId, model) => {
  const payload = { query, threadId }
  if (model) payload.model = model
  const response = await fetch(`${API_BASE_URL}/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    },
    body: JSON.stringify(payload)
  })
  
  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }
  
  return await response.text()
}

// 支持文件上传的聊天接口（multipart/form-data）
export const chatWithFiles = async (query, threadId, files = [], model) => {
  const formData = new FormData()
  const payload = { query, threadId }
  if (model) payload.model = model
  // query 以 JSON 字符串作为一个 part 发送，后端会以 @RequestPart("query") 接收
  formData.append('query', JSON.stringify(payload))
  for (const f of files) {
    // 后端接收名为 files 的数组
    // 期望 f 为实际的 File/Blob；如果传入了封装对象则尝试取出 f.file
    let fileObj = f && (f.file || f.blob || f)
    if (!fileObj) fileObj = f
    if (fileObj instanceof Blob) {
      const filename = (fileObj.name) || (f && f.name) || 'file'
      formData.append('files', fileObj, filename)
    } else if (typeof fileObj === 'string') {
      // 如果是 URL 或文本，作为普通字段发送（不会作为二进制文件）
      formData.append('files', fileObj)
      console.warn('chatWithFiles: sending file as string part, not Blob:', fileObj)
    } else {
      console.warn('chatWithFiles: skipping non-Blob file item', f)
    }
  }

  const response = await fetch(`${API_BASE_URL}/chat/multimodal`, {
    method: 'POST',
    body: formData
  })

  if (!response.ok) {
    throw new Error(`HTTP error! status: ${response.status}`)
  }

  return await response.text()
}

// 流式聊天接口
export const streamChat = async (query, threadId, onChunk, onEnd, onError, model, files = []) => {
  try {
    const payload = { query, threadId }
    if (model) payload.model = model
    let response
    if (files && files.length > 0) {
      // 使用 multipart/form-data 发送 query(JSON) 和 files
      const formData = new FormData()
      formData.append('query', JSON.stringify(payload))
      for (const f of files) {
        let fileObj = f && (f.file || f.blob || f)
        if (!fileObj) fileObj = f
        if (fileObj instanceof Blob) {
          const filename = (fileObj.name) || (f && f.name) || 'file'
          formData.append('files', fileObj, filename)
        } else if (typeof fileObj === 'string') {
          formData.append('files', fileObj)
          console.warn('streamChat: sending file as string part, not Blob:', fileObj)
        } else {
          console.warn('streamChat: skipping non-Blob file item', f)
        }
      }
      response = await fetch(`${API_BASE_URL}/stream/multimodal`, {
        method: 'POST',
        headers: {
          'Accept': 'text/event-stream'
        },
        body: formData
      })
    } else {
      response = await fetch(`${API_BASE_URL}/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream'
        },
        body: JSON.stringify(payload)
      })
    }
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    
    while (true) {
      const { done, value } = await reader.read()
      
      if (done) {
        onEnd && onEnd()
        break
      }
      
      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''
      
      for (const line of lines) {
        if (line.startsWith('data:')) {
          try {
            const data = JSON.parse(line.substring(5).trim())
            if (data.type === 'chunk') {
              onChunk && onChunk(data.content)
            } else if (data.type === 'end') {
              onEnd && onEnd()
            }
          } catch (e) {
            console.error('解析SSE数据失败:', line, e)
          }
        }
      }
    }
  } catch (error) {
    console.error('流式请求失败:', error)
    onError && onError(error)
  }
}

// 文件上传接口
export const uploadFile = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  
  const response = await fetch(`${API_BASE_URL}/upload`, {
    method: 'POST',
    body: formData
  })
  
  if (!response.ok) {
    throw new Error(`上传失败: ${response.status}`)
  }
  
  return await response.json()
}
