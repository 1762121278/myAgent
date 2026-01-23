# 智能对话助手（JavaFX）

## 运行

本项目使用 **Java 17 + JavaFX 21**。

### Maven 运行

```bash
mvn javafx:run
```

### IntelliJ IDEA 直接运行（解决“缺少 JavaFX 运行时组件”）

你遇到的报错是因为直接用 classpath 启动时，JavaFX 需要通过 **module-path** 加载。

做法 A（推荐）：用脚本启动

```powershell
.\scripts\run-javafx.ps1
```

做法 B：IDEA Run/Debug 配置里加 VM Options

- 在运行配置的 **VM options** 填（按你截图里依赖版本 21.0.5）：

```text
--module-path %USERPROFILE%\.m2\repository\org\openjfx\javafx-controls\21.0.5;%USERPROFILE%\.m2\repository\org\openjfx\javafx-graphics\21.0.5;%USERPROFILE%\.m2\repository\org\openjfx\javafx-base\21.0.5 --add-modules javafx.controls
```

如果你本机 Maven 配置了镜像（例如 `alimaven`）且出现证书/HTTPS 错误，请修改 `~/.m2/settings.xml`：

- 将 `http://maven.aliyun.com/...` 改为 `https://maven.aliyun.com/repository/public`
- 或临时注释掉该镜像，改用 Maven Central

## 页面实现说明

- 入口：`src/main/java/com/aiagent/app/ChatAssistantApp.java`
- 页面：`src/main/java/com/aiagent/ui/ChatAssistantView.java`
- 样式：`src/main/resources/styles/chat-assistant.css`

## 交互点（对应需求）

- 文本输入：Enter 发送，Shift+Enter 换行；输入区高度随行数在 50-150px 范围内变化
- 文件上传：前端校验类型与大小（文档 5MB、图片 2MB），显示文件列表并可删除
- 对话：发送后立即出现用户气泡；AI 回复延迟 1 秒；自动滚动到底部
- 响应式：窗口宽度 < 768 时，发送按钮全宽显示

