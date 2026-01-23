package com.aiagent.ui;

import com.aiagent.model.ChatMessage;
import com.aiagent.model.ChatSession;
import com.aiagent.model.UploadedFileItem;
import com.alibaba.fastjson.JSON;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.io.File;

/**
 * 聊天助手视图类，负责显示聊天界面和处理用户交互
 * @author jiangtao.shu
 */
public final class ChatAssistantView extends BorderPane {
    /** 桌面版宽度 */
    private static final double DESKTOP_WIDTH = 1400;
    /** 移动版断点宽度 */
    private static final double MOBILE_BREAKPOINT = 768;

    /** 文档最大字节数 (5MB) */
    private static final long DOC_MAX_BYTES = 5L * 1024 * 1024;
    /** 图片最大字节数 (2MB) */
    private static final long IMG_MAX_BYTES = 2L * 1024 * 1024;

    /** 支持的文档扩展名 */
    private static final Set<String> DOC_EXT = Set.of("pdf", "doc", "docx", "txt");
    /** 支持的图片扩展名 */
    private static final Set<String> IMG_EXT = Set.of("jpg", "jpeg", "png");

    /** 聊天消息列表 */
    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();
    /** 上传文件列表 */
    private final ObservableList<UploadedFileItem> uploadedFiles = FXCollections.observableArrayList();
    
    /** 会话列表 */
    private final ObservableList<ChatSession> sessions = FXCollections.observableArrayList();
    /** 当前会话 */
    private ChatSession currentSession;

    /** 模型选择下拉框 */
    private final ComboBox<String> modelSelect = new ComboBox<>();
    /** 消息列表视图 */
    private final ListView<ChatMessage> messageList = new ListView<>();
    /** 消息滚动容器 */
    private final ScrollPane messageScrollShell = new ScrollPane();
    
    /** 会话列表视图 */
    private final ListView<ChatSession> sessionList = new ListView<>();
    /** 侧边栏容器 */
    private final VBox sidebar = new VBox();
    /** 侧边栏切换按钮 */
    private final Button toggleSidebarBtn = new Button("☰");
    /** 新建会话按钮 */
    private final Button newSessionBtn = new Button("+ 新建会话");
    /** 侧边栏是否显示 */
    private boolean sidebarVisible = true;

    /** 附件上传按钮 */
    private final Button attachBtn = new Button();

    /** 输入文本区域 */
    private final TextArea input = new TextArea();
    /** 发送按钮 */
    private final Button sendBtn = new Button("发送");
    
    /** 当前线程ID */
    private String threadId;

    /**
     * 构造函数，初始化聊天助手视图
     */
    public ChatAssistantView() {
        getStyleClass().add("page");
        setPrefWidth(DESKTOP_WIDTH);

        // 创建主内容区域，使用VBox确保历史消息和输入框紧密相连
        var mainContent = new VBox();
        mainContent.setFillWidth(true);
        mainContent.setSpacing(0); // 移除间距，实现无缝衔接
        
        // 构建聊天区域（历史消息）
        var chatArea = buildChatArea();
        VBox.setVgrow(chatArea, Priority.ALWAYS); // 让聊天区域占据所有可用空间
        
        // 构建输入区域
        var inputArea = buildInputArea();
        
        // 将聊天区域和输入区域添加到主内容区域
        mainContent.getChildren().addAll(chatArea, inputArea);

        // 构建侧边栏
        buildSidebar();
        
        // 创建主布局容器（包含侧边栏和主内容）
        var mainContainer = new HBox();
        mainContainer.getChildren().addAll(sidebar, mainContent);
        HBox.setHgrow(mainContent, Priority.ALWAYS);
        
        setTop(buildHeader());
        setCenter(mainContainer);

        // 创建初始会话
        createNewSession();
        
        seedWelcome();
        wireBehavior();
    }

    /**
     * 构建顶部标题栏
     * @return 标题栏节点
     */
    private Node buildHeader() {
        var header = new HBox(16);
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 16, 12, 16));

        // 添加侧边栏切换按钮
        toggleSidebarBtn.getStyleClass().addAll("icon-btn", "toggle-sidebar-btn");
        toggleSidebarBtn.setTooltip(new Tooltip("显示/隐藏历史会话"));
        toggleSidebarBtn.setOnAction(e -> toggleSidebar());

        var brand = new HBox(10);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("brand");

        var icon = new Label("🤖");
        icon.getStyleClass().add("brand-icon");
        var title = new Label("智能对话助手");
        title.getStyleClass().add("brand-title");

        brand.getChildren().addAll(icon, title);

        header.getChildren().addAll(toggleSidebarBtn, brand);
        return header;
    }
    
    /**
     * 构建侧边栏
     */
    private void buildSidebar() {
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(280);
        sidebar.setMinWidth(250);
        sidebar.setMaxWidth(350);
        sidebar.setPadding(new Insets(16, 12, 16, 12));
        sidebar.setSpacing(12);
        
        // 新建会话按钮
        newSessionBtn.getStyleClass().add("new-session-btn");
        newSessionBtn.setMaxWidth(Double.MAX_VALUE);
        newSessionBtn.setOnAction(e -> createNewSession());
        
        // 会话列表
        sessionList.setItems(sessions);
        sessionList.getStyleClass().add("session-list");
        sessionList.setCellFactory(v -> new SessionCell());
        sessionList.setPrefHeight(Region.USE_COMPUTED_SIZE);
        VBox.setVgrow(sessionList, Priority.ALWAYS);
        
        // 会话列表选择监听
        sessionList.getSelectionModel().selectedItemProperty().addListener((obs, oldSession, newSession) -> {
            if (newSession != null) {
                switchToSession(newSession);
            }
        });
        
        // 添加鼠标点击事件，确保点击会话项时能切换
        sessionList.setOnMouseClicked(e -> {
            ChatSession selected = sessionList.getSelectionModel().getSelectedItem();
            if (selected != null && selected != currentSession) {
                switchToSession(selected);
            }
        });
        
        // 监听会话列表变化，刷新显示
        sessions.addListener((ListChangeListener<ChatSession>) c -> {
            Platform.runLater(() -> {
                sessionList.refresh();
            });
        });
        
        sidebar.getChildren().addAll(newSessionBtn, sessionList);
    }
    
    /**
     * 切换侧边栏显示/隐藏
     */
    private void toggleSidebar() {
        sidebarVisible = !sidebarVisible;
        sidebar.setVisible(sidebarVisible);
        sidebar.setManaged(sidebarVisible);
    }
    
    /**
     * 创建新会话
     */
    private void createNewSession() {
        ChatSession newSession = new ChatSession();
        sessions.add(newSession);
        switchToSession(newSession);
        sessionList.getSelectionModel().select(newSession);
    }
    
    /**
     * 切换到指定会话
     * @param session 要切换到的会话
     */
    private void switchToSession(ChatSession session) {
        // 保存当前会话状态（排除欢迎消息）
        if (currentSession != null && currentSession != session) {
            // 过滤掉欢迎消息，只保存真实的消息
            List<ChatMessage> realMessages = new ArrayList<>();
            for (ChatMessage msg : messages) {
                // 跳过欢迎消息（通过内容判断）
                if (!isWelcomeMessage(msg)) {
                    realMessages.add(msg);
                }
            }
            currentSession.setMessages(realMessages);
            currentSession.setUploadedFiles(new ArrayList<>(uploadedFiles));
        }
        
        // 切换到新会话
        currentSession = session;
        threadId = session.getId();
        
        // 更新消息列表
        messages.clear();
        List<ChatMessage> sessionMessages = session.getMessages();
        if (sessionMessages.isEmpty()) {
            // 如果会话为空，显示欢迎消息（但不保存到会话）
            seedWelcome();
        } else {
            // 如果会话有消息，直接显示会话消息
            messages.addAll(sessionMessages);
        }
        
        // 更新文件列表
        uploadedFiles.clear();
        uploadedFiles.addAll(session.getUploadedFiles());
        
        // 滚动到底部
        Platform.runLater(this::scrollToBottom);
    }
    
    /**
     * 判断是否是欢迎消息
     * @param message 消息对象
     * @return 是否是欢迎消息
     */
    private boolean isWelcomeMessage(ChatMessage message) {
        if (message.getRole() != ChatMessage.Role.AI) {
            return false;
        }
        String text = message.getText();
        return text != null && text.contains("您好！我是智能助手");
    }

    /**
     * 构建聊天区域
     * @return 聊天区域节点
     */
    private Node buildChatArea() {
        var outer = new StackPane();
        outer.getStyleClass().add("chat-outer");
        // 移除左右边距，只保留顶部边距，底部与输入框无缝衔接
        StackPane.setMargin(outer, new Insets(16, 16, 0, 16));

        messageList.setItems(messages);
        messageList.setFocusTraversable(false);
        messageList.getStyleClass().add("message-list");
        messageList.setCellFactory(v -> new MessageCell());
        messageList.setPrefWidth(Region.USE_COMPUTED_SIZE);
        messageList.setMaxWidth(Double.MAX_VALUE);

        messageScrollShell.setContent(messageList);
        messageScrollShell.setFitToWidth(true);
        messageScrollShell.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messageScrollShell.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        messageScrollShell.getStyleClass().add("message-scroll");
        // 确保滚动面板填充整个可用空间
        messageScrollShell.setFitToHeight(true);

        outer.getChildren().add(messageScrollShell);
        return outer;
    }

    /**
     * 构建输入区域
     * @return 输入区域节点
     */
    private Node buildInputArea() {
        // 创建主容器，使用VBox
        var wrap = new VBox(10);
        wrap.getStyleClass().add("composer");
        // 移除顶部边距，与聊天区域无缝衔接
        wrap.setPadding(new Insets(0, 16, 16, 16));

        // 创建文件上传区域，包含已选择文件和添加文件按钮
        var filesContainer = new HBox(10);
        filesContainer.setAlignment(Pos.CENTER_LEFT);
        filesContainer.setManaged(false); // 初始时不显示
        filesContainer.setVisible(false);
        filesContainer.getStyleClass().add("files-container");
        filesContainer.setPadding(new Insets(8, 0, 8, 0));

        // 创建添加文件按钮
        var addFileBtn = new Button();
        addFileBtn.getStyleClass().addAll("add-file-btn");
        var addFileIcon = new Text("+");
        addFileIcon.getStyleClass().add("add-file-icon");
        addFileBtn.setGraphic(addFileIcon);
        addFileBtn.setTooltip(new Tooltip("添加文件"));
        addFileBtn.setOnAction(e -> chooseFiles(false));

        // 监听文件列表变化，控制文件上传区域的显示/隐藏
        uploadedFiles.addListener((ListChangeListener<UploadedFileItem>) c -> {
            if (uploadedFiles.isEmpty()) {
                filesContainer.setManaged(false);
                filesContainer.setVisible(false);
                filesContainer.getChildren().clear(); // 清空文件容器内容
            } else {
                filesContainer.setManaged(true);
                filesContainer.setVisible(true);
                // 重新构建文件容器内容
                filesContainer.getChildren().clear();
                for (UploadedFileItem file : uploadedFiles) {
                    var fileItem = createFileItem(file);
                    filesContainer.getChildren().add(fileItem);
                }
                filesContainer.getChildren().add(addFileBtn);
            }
        });

        input.setPromptText("描述想要了解的内容");
        input.getStyleClass().add("input");
        input.setWrapText(true);
        input.setPrefRowCount(3);
        input.setMinHeight(60);
        input.setMaxHeight(150);

        // 创建模型选择区域
        var modelIcon = new Label("📋");
        modelIcon.getStyleClass().add("model-icon");
        modelIcon.setStyle("-fx-font-size: 16px;");

        var modelLabel = new Label("文心4.5T");
        modelLabel.getStyleClass().add("model-label");

        var modelArrowBtn = new Button();
        modelArrowBtn.getStyleClass().addAll("icon-btn", "model-arrow-btn");
        modelArrowBtn.setGraphic(loadIcon("/icons/arrow-exchange.png", "↔"));
        modelArrowBtn.setTooltip(new Tooltip("切换模型"));

        // 创建工具按钮区域
        var toolsRow = new HBox(10);
        toolsRow.setAlignment(Pos.CENTER_LEFT);

        var depthThinkingBtn = new Button("深度思考");
        depthThinkingBtn.getStyleClass().addAll("tool-btn", "depth-thinking-btn");

        var modelSelectBtn = new Button();
        modelSelectBtn.getStyleClass().addAll("icon-btn", "model-select-btn");
        modelSelectBtn.setGraphic(new HBox(6, modelIcon, modelLabel));
        modelSelectBtn.setTooltip(new Tooltip("选择模型"));

        attachBtn.getStyleClass().addAll("icon-btn", "attach-btn");
        attachBtn.setTooltip(new Tooltip("上传文档 (PDF/DOC/DOCX/TXT, 最大 5MB)"));
        attachBtn.setGraphic(loadIcon("/icons/attachment.png", "📎"));

        var micBtn = new Button();
        micBtn.getStyleClass().addAll("icon-btn", "mic-btn");
        micBtn.setTooltip(new Tooltip("语音输入"));
        micBtn.setGraphic(loadIcon("/icons/microphone.png", "🎤"));

        var imageBtn = new Button();
        imageBtn.getStyleClass().addAll("icon-btn", "image-btn");
        imageBtn.setTooltip(new Tooltip("上传图片"));
        imageBtn.setGraphic(loadIcon("/icons/image.png", "🖼️"));

        sendBtn.getStyleClass().add("send-btn");
        sendBtn.setDefaultButton(true);
        sendBtn.setDisable(true);

        toolsRow.getChildren().addAll(depthThinkingBtn, modelSelectBtn, modelArrowBtn, micBtn, attachBtn, imageBtn, sendBtn);

        wrap.getChildren().addAll(filesContainer, input, toolsRow);
        return wrap;
    }

    /**
     * 创建文件项组件
     * @param file 上传的文件项
     * @return 文件项组件
     */
    private HBox createFileItem(UploadedFileItem file) {
        var fileItem = new HBox(8);
        fileItem.getStyleClass().add("file-item");
        
        var fileIcon = new Label("📄");
        fileIcon.getStyleClass().add("file-icon");
        
        var fileInfo = new VBox(4);
        fileInfo.getStyleClass().add("file-info");
        
        var fileName = new Label(file.getName());
        fileName.getStyleClass().add("file-name");
        
        var fileReupload = new Label("点击图标重新上传");
        fileReupload.getStyleClass().add("file-reupload");
        
        fileInfo.getChildren().addAll(fileName, fileReupload);
        
        // 添加删除文件按钮
        var removeBtn = new Button("×");
        removeBtn.getStyleClass().add("file-remove-btn");
        removeBtn.setTooltip(new Tooltip("删除文件"));
        removeBtn.setOnAction(e -> uploadedFiles.remove(file));
        
        // 添加点击重新上传功能
        fileItem.setOnMouseClicked(e -> {
            if (!e.getTarget().equals(removeBtn)) {
                chooseFiles(false);
            }
        });
        
        fileItem.getChildren().addAll(fileIcon, fileInfo, removeBtn);
        return fileItem;
    }

    /**
     * 添加欢迎消息
     */
    private void seedWelcome() {
        // 只在消息列表为空时添加欢迎消息，且不保存到会话中
        if (messages.isEmpty()) {
            ChatMessage welcomeMsg = new ChatMessage(
                    ChatMessage.Role.AI,
                    "您好！我是智能助手，可以回答您的问题或帮助处理文档和图片。请告诉我您需要什么帮助？",
                    LocalDateTime.now()
            );
            messages.add(welcomeMsg);
            // 注意：欢迎消息不添加到会话中，因为它是临时显示的
            // 当用户发送第一条消息时，欢迎消息会被自动移除
        }
    }

    /**
     * 绑定事件处理
     */
    private void wireBehavior() {
        messages.addListener((ListChangeListener<ChatMessage>) c -> Platform.runLater(this::scrollToBottom));

        uploadedFiles.addListener((ListChangeListener<UploadedFileItem>) c -> updateSendEnabled());
        input.textProperty().addListener((obs, o, n) -> {
            clampTextAreaHeight();
            updateSendEnabled();
        });

        attachBtn.setOnAction(e -> chooseFiles(false));
        sendBtn.setOnAction(e -> send());

        input.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                if (e.isShiftDown()) {
                    return; // newline
                }
                e.consume();
                if (!sendBtn.isDisabled()) {
                    send();
                }
            }
        });

        widthProperty().addListener((obs, o, n) -> applyResponsive(n.doubleValue()));
        applyResponsive(getWidth());
    }

    /**
     * 应用响应式布局
     * @param width 当前宽度
     */
    private void applyResponsive(double width) {
        boolean mobile = width > 0 && width < MOBILE_BREAKPOINT;
        pseudoClassStateChanged(PseudoClass.getPseudoClass("mobile"), mobile);
        if (mobile) {
            sendBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(sendBtn, Priority.ALWAYS);
        } else {
            sendBtn.setMaxWidth(Region.USE_COMPUTED_SIZE);
            HBox.setHgrow(sendBtn, Priority.NEVER);
        }
    }

    /**
     * 调整文本区域高度
     */
    private void clampTextAreaHeight() {
        // Simple approximation: rows based on line count, clamped to [50,150]
        int lines = Math.max(1, input.getText().split("\n", -1).length);
        double target = Math.min(150, Math.max(50, 24 + (lines * 18)));
        input.setPrefHeight(target);
    }

    /**
     * 更新发送按钮状态
     */
    private void updateSendEnabled() {
        boolean hasText = input.getText() != null && !input.getText().trim().isEmpty();
        boolean hasFiles = !uploadedFiles.isEmpty();
        sendBtn.setDisable(!(hasText || hasFiles));
    }

    /**
     * 选择文件
     * @param imagesOnly 是否只选择图片
     */
    private void chooseFiles(boolean imagesOnly) {
        var chooser = new FileChooser();
        chooser.setTitle(imagesOnly ? "选择图片" : "选择文件");
        if (imagesOnly) {
            chooser.getExtensionFilters().setAll(
                    new FileChooser.ExtensionFilter("图片文件", "*.jpg", "*.jpeg", "*.png")
            );
        } else {
            chooser.getExtensionFilters().setAll(
                    new FileChooser.ExtensionFilter("文档文件", "*.pdf", "*.doc", "*.docx", "*.txt"),
                    new FileChooser.ExtensionFilter("图片文件", "*.jpg", "*.jpeg", "*.png")
            );
        }

        List<File> picked = chooser.showOpenMultipleDialog(getScene().getWindow());
        if (picked == null || picked.isEmpty()) {
            return;
        }

        for (File f : picked) {
            if (!validateAndAdd(f)) {
                return; // alert already shown, stop batch to avoid spamming
            }
        }
    }

    /**
     * 验证并添加文件
     * @param file 要添加的文件
     * @return 是否添加成功
     */
    private boolean validateAndAdd(File file) {
        String ext = getExt(file.getName());
        if (ext.isEmpty()) {
            alert("不支持的格式", "请选择 PDF/DOC/DOCX/TXT 或 JPG/PNG 文件。");
            return false;
        }

        boolean isDoc = DOC_EXT.contains(ext);
        boolean isImg = IMG_EXT.contains(ext);
        if (!isDoc && !isImg) {
            alert("不支持的格式", "不支持该文件格式：" + ext);
            return false;
        }

        long size = file.length();
        if (isDoc && size > DOC_MAX_BYTES) {
            alert("文件过大", "文档最大 5MB，请重新选择。");
            return false;
        }
        if (isImg && size > IMG_MAX_BYTES) {
            alert("文件过大", "图片最大 2MB，请重新选择。");
            return false;
        }

        boolean dup = uploadedFiles.stream().anyMatch(it -> it.getFile().getAbsolutePath().equals(file.getAbsolutePath()));
        if (!dup) {
            uploadedFiles.add(new UploadedFileItem(file));
        }
        return true;
    }

    /**
     * 发送消息
     */
    private void send() {
        if (currentSession == null) {
            createNewSession();
        }
        
        String text = input.getText() == null ? "" : input.getText().trim();
        var filesSnapshot = List.copyOf(uploadedFiles);

        // 移除欢迎消息（如果存在）
        messages.removeIf(this::isWelcomeMessage);

        // 添加用户消息
        ChatMessage userMessage = new ChatMessage(ChatMessage.Role.USER, buildUserPayload(text, filesSnapshot), LocalDateTime.now());
        messages.add(userMessage);
        currentSession.addMessage(userMessage);
        input.clear();
        uploadedFiles.clear();
        
        // 添加加载中的消息
        ChatMessage loadingMessage = new ChatMessage(ChatMessage.Role.AI, "loading", LocalDateTime.now());
        messages.add(loadingMessage);
        
        // 禁用发送按钮并更改文本为"处理中..."
        sendBtn.setDisable(true);
        String originalText = sendBtn.getText();
        sendBtn.setText("处理中...");

        // 异步调用AI接口
        new Thread(() -> {
            try {
                String aiResponse = callAiApi(text);
                // 在JavaFX应用线程中添加AI回复
                Platform.runLater(() -> {
                    // 移除加载中的消息
                    messages.remove(loadingMessage);
                    // 添加AI回复
                    ChatMessage aiMessage = new ChatMessage(ChatMessage.Role.AI, aiResponse, LocalDateTime.now());
                    messages.add(aiMessage);
                    currentSession.addMessage(aiMessage);
                    // 刷新会话列表以更新标题
                    sessionList.refresh();
                    // 恢复发送按钮状态
                    sendBtn.setText(originalText);
                    sendBtn.setDisable(false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                // 发生错误时也恢复发送按钮状态
                Platform.runLater(() -> {
                    // 移除加载中的消息
                    messages.remove(loadingMessage);
                    // 添加错误消息
                    ChatMessage errorMessage = new ChatMessage(ChatMessage.Role.AI, "抱歉，处理请求时发生错误，请稍后重试。", LocalDateTime.now());
                    messages.add(errorMessage);
                    currentSession.addMessage(errorMessage);
                    // 刷新会话列表以更新标题
                    sessionList.refresh();
                    // 恢复发送按钮状态
                    sendBtn.setText(originalText);
                    sendBtn.setDisable(false);
                });
            }
        }).start();
    }

    /**
     * 调用AI接口
     * @param query 用户查询文本
     * @return AI回复内容
     */
    private String callAiApi(String query) {
        try {
            // 构建请求URL
            URL url = new URL("http://localhost:8090/aiAgent/chat");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 设置请求方法和头信息
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("query", query);

            // 构建请求体
            String requestBody = JSON.toJSONString(queryMap);

            // 发送请求
            try (java.io.OutputStream os = connection.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 读取响应
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine);
                    }
                    return response.toString();
                }
            } else {
                return "抱歉，调用AI接口失败，错误码：" + responseCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "抱歉，调用AI接口时发生错误：" + e.getMessage();
        }
    }

    /**
     * 转义JSON字符串
     * @param value 需要转义的字符串
     * @return 转义后的字符串
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    /**
     * 构建用户消息内容
     * @param text 用户输入文本
     * @param files 上传的文件列表
     * @return 构建后的消息内容
     */
    private String buildUserPayload(String text, List<UploadedFileItem> files) {
        var sb = new StringBuilder();
        if (text != null && !text.isBlank()) {
            sb.append(text);
        }
        if (!files.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append("已上传文件：\n");
            for (var f : files) {
                sb.append("- ").append(f.getName()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    /**
     * 构建AI回复内容
     * @param userText 用户输入文本
     * @param files 上传的文件列表
     * @return 构建后的回复内容
     */
    private String buildAiReply(String userText, List<UploadedFileItem> files) {
        String model = modelSelect.getSelectionModel().getSelectedItem();
        if (!files.isEmpty()) {
            return "好的，我明白了。已收到 " + files.size() + " 个文件（模型：" + model + "），请告诉我你希望我从中提取或分析哪些信息。";
        }
        if (userText == null || userText.isBlank()) {
            return "好的，我明白了。";
        }
        return "好的，我明白了。让我来帮您解决这个问题。（模型：" + model + "）";
    }

    /**
     * 滚动到底部
     */
    private void scrollToBottom() {
        if (messages.isEmpty()) {
            return;
        }
        messageList.scrollTo(messages.size() - 1);
        messageScrollShell.setVvalue(1.0);
    }

    /**
     * 获取文件扩展名
     * @param name 文件名
     * @return 扩展名（小写）
     */
    private static String getExt(String name) {
        int idx = name.lastIndexOf('.');
        if (idx < 0 || idx == name.length() - 1) {
            return "";
        }
        return name.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 显示提示对话框
     * @param title 标题
     * @param content 内容
     */
    private static void alert(String title, String content) {
        var a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }

    /**
     * 加载图标
     * @param resourcePath 资源路径
     * @param fallbackEmoji  fallback表情
     * @return 图标节点
     */
    private static Node loadIcon(String resourcePath, String fallbackEmoji) {
        try {
            var url = ChatAssistantView.class.getResource(resourcePath);
            if (url == null) {
                var text = new Text(fallbackEmoji);
                text.setStyle("-fx-font-size: 20px;");
                return text;
            }
            var img = new Image(url.toExternalForm(), 24, 24, true, true);
            var iv = new ImageView(img);
            iv.setPreserveRatio(true);
            return iv;
        } catch (Exception ignored) {
            var text = new Text(fallbackEmoji);
            text.setStyle("-fx-font-size: 20px;");
            return text;
        }
    }

    /**
     * 消息单元格类，用于显示聊天消息
     */
    private static final class MessageCell extends ListCell<ChatMessage> {
        @Override
        protected void updateItem(ChatMessage item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            // 检查是否是加载中的消息
            if ("loading".equals(item.getText())) {
                // 显示加载动画
                var loadingContainer = new HBox();
                loadingContainer.setAlignment(Pos.CENTER_LEFT);
                loadingContainer.setPadding(new Insets(12, 16, 12, 16));
                
                var loadingLabel = new Label("正在思考...");
                loadingLabel.getStyleClass().add("loading-text");
                
                // 创建加载动画效果
                var loadingIndicator = new Label("● ● ●");
                loadingIndicator.getStyleClass().add("loading-indicator");
                
                // 添加加载动画效果
                Timeline timeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(loadingIndicator.opacityProperty(), 0.3)),
                    new KeyFrame(Duration.millis(500), new KeyValue(loadingIndicator.opacityProperty(), 1.0)),
                    new KeyFrame(Duration.millis(1000), new KeyValue(loadingIndicator.opacityProperty(), 0.3))
                );
                timeline.setCycleCount(Timeline.INDEFINITE);
                timeline.play();
                
                loadingContainer.getChildren().addAll(loadingLabel, loadingIndicator);
                loadingContainer.getStyleClass().add("loading-container");
                
                setText(null);
                setGraphic(loadingContainer);
                return;
            }

            var bubble = new Label(item.getText());
            bubble.setWrapText(true);
            bubble.getStyleClass().add("bubble");
            bubble.setMaxWidth(600); // 增加默认最大宽度

            var row = new HBox();
            row.getStyleClass().add("bubble-row");
            // 监听父容器宽度变化，动态调整气泡最大宽度
            row.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                if (newWidth.doubleValue() > 0) {
                    // 气泡最大宽度为父容器宽度的 90%，增加宽度
                    double maxWidth = newWidth.doubleValue() * 0.9;
                    bubble.setMaxWidth(maxWidth);
                }
            });

            if (item.getRole() == ChatMessage.Role.USER) {
                row.setAlignment(Pos.TOP_RIGHT);
                bubble.getStyleClass().add("bubble-user");
            } else {
                row.setAlignment(Pos.TOP_LEFT);
                bubble.getStyleClass().add("bubble-ai");
            }

            row.getChildren().add(bubble);

            setText(null);
            setGraphic(row);
        }
    }

    /**
     * 文件项单元格类，用于显示上传的文件
     */
    private static final class FileItemCell extends ListCell<UploadedFileItem> {
        private final ObservableList<UploadedFileItem> backing;

        private FileItemCell(ObservableList<UploadedFileItem> backing) {
            this.backing = backing;
        }

        @Override
        protected void updateItem(UploadedFileItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            var name = new Label(item.getName());
            name.getStyleClass().add("file-name");

            var remove = new Button("×");
            remove.getStyleClass().add("file-remove");
            remove.setOnAction(e -> backing.remove(item));

            var row = new HBox(8, name, new Region(), remove);
            HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("file-row");

            setText(null);
            setGraphic(row);
        }
    }
    
    /**
     * 会话单元格类，用于显示会话列表项
     */
    private static final class SessionCell extends ListCell<ChatSession> {
        private final VBox container = new VBox(4);
        private final Label titleLabel = new Label();
        private final Label timeLabel = new Label();
        
        public SessionCell() {
            container.getStyleClass().add("session-item");
            container.setPadding(new Insets(12));
            titleLabel.getStyleClass().add("session-title");
            titleLabel.setWrapText(true);
            timeLabel.getStyleClass().add("session-time");
            container.getChildren().addAll(titleLabel, timeLabel);
        }
        
        @Override
        protected void updateItem(ChatSession session, boolean empty) {
            super.updateItem(session, empty);
            if (empty || session == null) {
                setGraphic(null);
                setText(null);
                return;
            }

            titleLabel.setText(session.getTitle());
            timeLabel.setText(formatTime(session.getUpdatedAt()));
            
            // 根据选中状态更新样式
            if (isSelected()) {
                container.getStyleClass().removeAll("session-item");
                container.getStyleClass().addAll("session-item", "session-item-selected");
            } else {
                container.getStyleClass().removeAll("session-item-selected");
                if (!container.getStyleClass().contains("session-item")) {
                    container.getStyleClass().add("session-item");
                }
            }
            
            setText(null);
            setGraphic(container);
        }
        
        private String formatTime(LocalDateTime time) {
            if (time == null) {
                return "";
            }
            LocalDateTime now = LocalDateTime.now();
            if (time.toLocalDate().equals(now.toLocalDate())) {
                // 今天：显示时间
                return String.format("%02d:%02d", time.getHour(), time.getMinute());
            } else if (time.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
                // 昨天
                return "昨天";
            } else {
                // 更早：显示日期
                return String.format("%d/%d", time.getMonthValue(), time.getDayOfMonth());
            }
        }
    }
}

