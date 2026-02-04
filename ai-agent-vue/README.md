# AI Agent Vue 前端项目

## 技术栈

### 核心框架
- **Vue 3** - 渐进式JavaScript框架
- **Vite** - 下一代前端构建工具

### 主要依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| vue | ^3.4.21 | 核心框架 |
| marked | ^12.0.0 | Markdown解析 |
| marked-highlight | ^2.1.0 | Markdown代码高亮 |
| highlight.js | ^11.9.0 | 语法高亮 |

### 开发工具
- **@vitejs/plugin-vue** - Vite Vue插件
- **Vite** - 构建工具

## 项目结构

```
ai-agent-vue/
├── public/                 # 静态资源
│   └── icons/
├── src/
│   ├── api/
│   │   └── chat.js        # API接口封装
│   ├── components/        # Vue组件
│   │   ├── ChatAssistant.vue   # 主组件
│   │   ├── Sidebar.vue         # 侧边栏
│   │   ├── Header.vue          # 顶部标题栏
│   │   ├── ChatArea.vue        # 聊天消息区域
│   │   └── InputArea.vue       # 输入区域
│   ├── stores/
│   │   └── chat.js        # 状态管理
│   ├── utils/
│   │   └── markdown.js    # Markdown渲染工具
│   ├── App.vue
│   ├── main.js            # 入口文件
│   └── style.css          # 全局样式
├── index.html
├── package.json
└── vite.config.js
```

## 功能特性

- 会话管理（创建、切换、删除）
- 实时聊天（支持流式响应）
- Markdown消息渲染（代码高亮）
- 文件上传（文档/图片）
- 本地存储持久化
- 响应式布局（支持移动端）

## 开发命令

```bash
# 安装依赖
npm install

# 开发模式
npm run dev

# 生产构建
npm run build

# 预览构建结果
npm run preview
```

## 构建输出

构建后的文件位于 `dist/` 目录，包含：
- `index.html` - 入口HTML
- `assets/` - JS/CSS资源
- `icons/` - 图标资源
