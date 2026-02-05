import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'

// 配置marked
marked.setOptions({
  breaks: true,
  gfm: true,
  headerIds: false,
  mangle: false
})

// 自定义渲染器
const renderer = new marked.Renderer()

// 代码块渲染
renderer.code = (code, language) => {
  const validLanguage = language && hljs.getLanguage(language) ? language : 'plaintext'
  const highlighted = hljs.highlight(code, { language: validLanguage }).value
  return `<pre class="code-block"><code class="hljs language-${validLanguage}">${highlighted}</code></pre>`
}

// 行内代码渲染
renderer.codespan = (code) => {
  return `<code class="inline-code">${code}</code>`
}

// 段落渲染
renderer.paragraph = (text) => {
  return `<p class="markdown-paragraph">${text}</p>`
}

// 列表渲染
renderer.list = (body, ordered) => {
  const type = ordered ? 'ol' : 'ul'
  return `<${type} class="markdown-list">${body}</${type}>`
}

// 列表项渲染
renderer.listitem = (text) => {
  return `<li class="markdown-list-item">${text}</li>`
}

// 链接渲染
renderer.link = (href, title, text) => {
  return `<a href="${href}" title="${title || ''}" target="_blank" rel="noopener noreferrer" class="markdown-link">${text}</a>`
}

// 强调渲染
renderer.strong = (text) => {
  return `<strong class="markdown-strong">${text}</strong>`
}

// 斜体渲染
renderer.em = (text) => {
  return `<em class="markdown-em">${text}</em>`
}

// 标题渲染
renderer.heading = (text, level) => {
  return `<h${level} class="markdown-heading markdown-h${level}">${text}</h${level}>`
}

// 引用块渲染
renderer.blockquote = (quote) => {
  return `<blockquote class="markdown-blockquote">${quote}</blockquote>`
}

// 表格渲染
renderer.table = (header, body) => {
  return `<table class="markdown-table"><thead>${header}</thead><tbody>${body}</tbody></table>`
}

// 表格行渲染
renderer.tablerow = (content) => {
  return `<tr class="markdown-table-row">${content}</tr>`
}

// 表格单元格渲染
renderer.tablecell = (content, flags) => {
  const tag = flags.header ? 'th' : 'td'
  return `<${tag} class="markdown-table-cell">${content}</${tag}>`
}

// 分割线渲染
renderer.hr = () => {
  return `<hr class="markdown-hr">`
}

marked.use({ renderer })

// 渲染Markdown文本
export const renderMarkdown = (text) => {
  if (!text) return ''
  try {
    // 将特定姓名用 span 包裹，便于前端用样式高亮（例如：李敏）
    const safeText = String(text).replace(/李敏/g, '<span class="highlight-name">李敏</span>')
    return marked.parse(safeText)
  } catch (e) {
    console.error('Markdown渲染失败:', e)
    return text
  }
}

// 转义HTML特殊字符
export const escapeHtml = (text) => {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}
