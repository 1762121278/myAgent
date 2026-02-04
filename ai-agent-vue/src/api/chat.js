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

// 流式聊天接口
export const streamChat = async (query, threadId, onChunk, onEnd, onError, model) => {
  try {
    const payload = { query, threadId }
    if (model) payload.model = model
    const response = await fetch(`${API_BASE_URL}/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream'
      },
      body: JSON.stringify(payload)
    })
    
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
