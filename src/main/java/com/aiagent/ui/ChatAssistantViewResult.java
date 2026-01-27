package com.aiagent.ui;

import com.aiagent.model.ChatMessage;
import com.aiagent.model.ChatSession;
import com.aiagent.model.UploadedFileItem;
import com.aiagent.util.SessionStorageManager;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 聊天助手视图类，负责显示聊天界面和处理用户交互
 * 
 * 主要功能：
 * - 显示聊天消息列表，支持Markdown格式渲染
 * - 管理多个会话，支持会话切换
 * - 文件上传功能（文档和图片）
 * - 侧边栏显示/隐藏，带滑动动画效果
 * - 与后端AI接口通信，获取智能回复
 * 
 * @author jiangtao.shu
 */
public final class ChatAssistantViewResult extends BorderPane {
    /** 桌面版宽度 - 用于设置界面默认宽度 */
    private static final double DESKTOP_WIDTH = 1400;
    /** 移动版断点宽度 - 小于此宽度时应用移动端样式 */
    private static final double MOBILE_BREAKPOINT = 768;

    /** 文档最大字节数 (5MB) */
    private static final long DOC_MAX_BYTES = 5L * 1024 * 1024;
    /** 图片最大字节数 (2MB) */
    private static final long IMG_MAX_BYTES = 2L * 1024 * 1024;

    /** 支持的文档扩展名 */
    private static final Set<String> DOC_EXT = Set.of("pdf", "doc", "docx", "txt");
    /** 支持的图片扩展名 */
    private static final Set<String> IMG_EXT = Set.of("jpg", "jpeg", "png");
    private static final Logger log = LoggerFactory.getLogger(ChatAssistantViewResult.class);

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

    /** 会话存储管理器 */
    private final SessionStorageManager sessionStorageManager;

    /** 待处理的AI响应映射表，key为sessionId，value为响应内容 */
    private final Map<String, String> pendingResponses = new HashMap<>();

    /** 待处理的错误信息映射表，key为sessionId，value为错误信息 */
    private final Map<String, String> pendingErrors = new HashMap<>();

    /**
     * 构造函数，初始化聊天助手视图
     * 构建完整的UI布局：顶部标题栏、侧边栏、聊天区域和输入区域
     */
    public ChatAssistantViewResult() {
        // 初始化会话存储管理器
        this.sessionStorageManager = new SessionStorageManager();
        
        // 设置页面样式类
        getStyleClass().add("page");
        setPrefWidth(DESKTOP_WIDTH);

        // 创建主内容区域，使用VBox确保历史消息和输入框紧密相连
        var mainContent = new VBox();
        mainContent.setFillWidth(true);
        mainContent.setSpacing(0); // 移除间距，实现无缝衔接
        
        // 构建聊天区域（历史消息显示区域）
        var chatArea = buildChatArea();
        VBox.setVgrow(chatArea, Priority.ALWAYS); // 让聊天区域占据所有可用空间
        
        // 构建输入区域（包含输入框和工具栏）
        var inputArea = buildInputArea();
        
        // 将聊天区域和输入区域添加到主内容区域
        mainContent.getChildren().addAll(chatArea, inputArea);

        // 构建侧边栏（会话列表）
        buildSidebar();
        
        // 创建主布局容器（包含侧边栏和主内容）
        var mainContainer = new HBox();
        mainContainer.getChildren().addAll(sidebar, mainContent);
        HBox.setHgrow(mainContent, Priority.ALWAYS); // 主内容区域自动扩展
        
        // 设置顶部标题栏和中心内容区域
        setTop(buildHeader());
        setCenter(mainContainer);

        // 初始化：加载已保存的会话或创建初始会话、显示欢迎消息、绑定事件处理
        loadSavedSessions();
        if (sessions.isEmpty()) {
            createNewSession();
            seedWelcome(); // 显示欢迎消息
        }
        wireBehavior(); // 绑定各种事件处理器
    }
    
    /**
     * 格式化时间显示
     * @param time 时间
     * @return 格式化后的时间字符串
     */
    private static String formatTime(LocalDateTime time) {
        if (time == null) {
            return "";
        }
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }
        
    /**
     * 加载已保存的会话
     */
    private void loadSavedSessions() {
        List<ChatSession> savedSessions = sessionStorageManager.loadAllSessions();
        sessions.addAll(savedSessions);
            
        // 如果有保存的会话，切换到最新的一个
        if (!sessions.isEmpty()) {
            ChatSession latestSession = sessions.get(0); // 按更新时间排序，第一个是最新的
            switchToSession(latestSession);
            sessionList.getSelectionModel().select(latestSession);
        }
    }
        
    /**
     * 保存当前会话
     */
    private void saveCurrentSession() {
        if (currentSession != null) {
            sessionStorageManager.saveSession(currentSession);
        }
    }
        
    /**
     * 保存所有会话
     */
    private void saveAllSessions() {
        sessionStorageManager.saveSessions(new ArrayList<>(sessions));
    }
        
    /**
     * 应用程序关闭时保存所有会话
     */
    public void saveAllSessionsOnExit() {
        saveAllSessions();
    }
        
    /**
     * 构建顶部标题栏
     * 包含：侧边栏切换按钮、应用图标和标题
     * 
     * @return 标题栏节点（HBox容器）
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

        var icon = new ImageView(new Image(getClass().getResourceAsStream("/icons/airobot.jpg")));
        icon.getStyleClass().add("brand-icon");
        icon.setFitWidth(24);
        icon.setFitHeight(24);
        var title = new Label("智能对话助手");
        title.getStyleClass().add("brand-title");

        brand.getChildren().addAll(icon, title);

        header.getChildren().addAll(toggleSidebarBtn, brand);
        return header;
    }
    
    /**
     * 构建侧边栏
     * 包含：新建会话按钮和会话列表
     * 侧边栏支持滑动动画显示/隐藏
     */
    private void buildSidebar() {
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(280);
        sidebar.setMinWidth(250);
        sidebar.setMaxWidth(350);
        sidebar.setPadding(new Insets(16, 12, 16, 12));
        sidebar.setSpacing(12);
        // 初始化时侧边栏可见，translateX为0
        sidebar.setTranslateX(0);
        
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
     * 切换侧边栏显示/隐藏（带滑动动画）
     * 使用Timeline动画实现平滑的宽度变化效果，提升用户体验
     */
    private void toggleSidebar() {
        sidebarVisible = !sidebarVisible;
        double targetWidth = 280; // 侧边栏目标宽度
        
        if (sidebarVisible) {
            // 显示侧边栏：从宽度0滑入到目标宽度（400毫秒动画）
            sidebar.setVisible(true);
            sidebar.setManaged(true);
            sidebar.setPrefWidth(0); // 初始宽度为0
            sidebar.setMinWidth(0);
            
            // 创建滑入动画：宽度从0增加到目标宽度
            Timeline slideIn = new Timeline(
                new KeyFrame(Duration.ZERO, 
                    new KeyValue(sidebar.prefWidthProperty(), 0),
                    new KeyValue(sidebar.minWidthProperty(), 0)),
                new KeyFrame(Duration.millis(400), // 动画时长400毫秒
                    new KeyValue(sidebar.prefWidthProperty(), targetWidth),
                    new KeyValue(sidebar.minWidthProperty(), 250))
            );
            slideIn.setCycleCount(1); // 只播放一次
            slideIn.play();
        } else {
            // 隐藏侧边栏：从当前宽度滑出到0（400毫秒动画）
            double currentWidth = sidebar.getWidth() > 0 ? sidebar.getWidth() : targetWidth;
            
            // 创建滑出动画：宽度从当前宽度减少到0
            Timeline slideOut = new Timeline(
                new KeyFrame(Duration.ZERO, 
                    new KeyValue(sidebar.prefWidthProperty(), currentWidth),
                    new KeyValue(sidebar.minWidthProperty(), 250)),
                new KeyFrame(Duration.millis(400), // 动画时长400毫秒
                    new KeyValue(sidebar.prefWidthProperty(), 0),
                    new KeyValue(sidebar.minWidthProperty(), 0))
            );
            slideOut.setCycleCount(1); // 只播放一次
            // 动画完成后隐藏侧边栏并恢复原始宽度设置
            slideOut.setOnFinished(event -> {
                sidebar.setVisible(false);
                sidebar.setManaged(false);
                // 恢复原始宽度设置，以便下次显示时使用
                sidebar.setPrefWidth(targetWidth);
                sidebar.setMinWidth(250);
            });
            slideOut.play();
        }
    }
    
    /**
     * 创建新会话
     * 创建一个新的ChatSession对象，添加到会话列表，并切换到该会话
     */
    private void createNewSession() {
        ChatSession newSession = new ChatSession();
        sessions.add(newSession);
        switchToSession(newSession);
        sessionList.getSelectionModel().select(newSession);
        
        // 保存新会话到本地
        saveCurrentSession();
    }
    
    /**
     * 切换到指定会话
     * 保存当前会话状态，加载目标会话的消息和文件列表
     * 
     * @param session 要切换到的会话对象
     */
    private void switchToSession(ChatSession session) {
        // 保存当前会话状态（排除欢迎消息和loading消息）
        if (currentSession != null && currentSession != session) {
            // 过滤掉欢迎消息和loading消息，只保存真实的消息
            List<ChatMessage> realMessages = new ArrayList<>();
            for (ChatMessage msg : messages) {
                // 跳过欢迎消息（通过内容判断）
                if (!isWelcomeMessage(msg) && !"loading".equals(msg.getText())) {
                    realMessages.add(msg);
                }
            }
            currentSession.setMessages(realMessages);
            currentSession.setUploadedFiles(new ArrayList<>(uploadedFiles));
            
            // 保存当前会话到本地
            saveCurrentSession();
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
        
        // 检查是否存在待处理的响应，并添加到当前会话
        String sessionId = session.getId();
        if (pendingResponses.containsKey(sessionId)) {
            String response = pendingResponses.get(sessionId);
            if (response != null) {
                // 添加AI回复
                ChatMessage aiMessage = new ChatMessage(ChatMessage.Role.AI, response, LocalDateTime.now());
                messages.add(aiMessage);
                currentSession.addMessage(aiMessage); // 保存到会话历史
                // 刷新会话列表以更新标题
                sessionList.refresh();
                // 从待处理列表中移除
                pendingResponses.remove(sessionId);
            }
        }
        
        // 检查是否存在待处理的错误，并添加到当前会话
        if (pendingErrors.containsKey(sessionId)) {
            String error = pendingErrors.get(sessionId);
            if (error != null) {
                // 添加错误消息
                ChatMessage errorMessage = new ChatMessage(ChatMessage.Role.AI, error, LocalDateTime.now());
                messages.add(errorMessage);
                currentSession.addMessage(errorMessage);
                // 刷新会话列表以更新标题
                sessionList.refresh();
                // 从待处理列表中移除
                pendingErrors.remove(sessionId);
            }
        }
        
        // 更新文件列表
        uploadedFiles.clear();
        uploadedFiles.addAll(session.getUploadedFiles());
        
        // 滚动到底部
        Platform.runLater(this::scrollToBottom);
    }
    
    /**
     * 判断是否是欢迎消息
     * 欢迎消息是AI发送的初始提示消息，不应保存到会话历史中
     * 
     * @param message 消息对象
     * @return 如果是欢迎消息返回true，否则返回false
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
     * 包含消息列表和滚动容器，用于显示聊天历史
     * 
     * @return 聊天区域节点（StackPane容器）
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
     * 包含：文件上传区域、文本输入框、工具栏（模型选择、附件、发送等按钮）
     * 
     * @return 输入区域节点（VBox容器）
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
        modelArrowBtn.setGraphic(loadIcon("/icons/arrow-exchange.png", "\uD83D\uDD04"));
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
        imageBtn.setGraphic(loadIcon("/icons/image.png", "\uD83D\uDCC6"));

        sendBtn.getStyleClass().add("send-btn");
        sendBtn.setDefaultButton(true);
        sendBtn.setDisable(true);

        toolsRow.getChildren().addAll(depthThinkingBtn, modelSelectBtn, modelArrowBtn, micBtn, attachBtn, imageBtn, sendBtn);

        wrap.getChildren().addAll(filesContainer, input, toolsRow);
        return wrap;
    }

    /**
     * 创建文件项组件
     * 显示文件图标、文件名、重新上传提示和删除按钮
     * 
     * @param file 上传的文件项对象
     * @return 文件项组件（HBox容器）
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
     * 在消息列表为空时显示欢迎消息，提示用户如何使用应用
     * 欢迎消息不会保存到会话历史中
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
     * 设置各种UI组件的事件监听器：
     * - 消息列表变化时自动滚动到底部
     * - 文件列表和输入框变化时更新发送按钮状态
     * - 键盘快捷键（Enter发送，Shift+Enter换行）
     * - 窗口大小变化时应用响应式布局
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
     * 根据窗口宽度调整UI布局，小于768px时应用移动端样式
     * 
     * @param width 当前窗口宽度
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
     * 根据输入文本的行数动态调整输入框高度，范围在50-150像素之间
     * 每行大约18像素高度
     */
    private void clampTextAreaHeight() {
        // Simple approximation: rows based on line count, clamped to [50,150]
        int lines = Math.max(1, input.getText().split("\n", -1).length);
        double target = Math.min(150, Math.max(50, 24 + (lines * 18)));
        input.setPrefHeight(target);
    }

    /**
     * 更新发送按钮状态
     * 当有输入文本或上传文件时启用发送按钮，否则禁用
     */
    private void updateSendEnabled() {
        boolean hasText = input.getText() != null && !input.getText().trim().isEmpty();
        boolean hasFiles = !uploadedFiles.isEmpty();
        sendBtn.setDisable(!(hasText || hasFiles));
    }

    /**
     * 选择文件
     * 打开文件选择对话框，允许用户选择一个或多个文件
     * 
     * @param imagesOnly 如果为true，只允许选择图片文件；如果为false，可以选择文档或图片
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
     * 检查文件格式、大小和是否重复，验证通过后添加到上传文件列表
     * 
     * @param file 要添加的文件对象
     * @return 如果文件验证通过并成功添加返回true，否则返回false
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
     * 发送消息：处理用户输入，调用AI接口，更新UI
     * 流程：1. 创建会话（如需要） 2. 添加用户消息 3. 显示加载状态 4. 异步调用AI 5. 显示AI回复
     */
    private void send() {
        // 如果当前没有会话，创建一个新会话
        if (currentSession == null) {
            createNewSession();
        }

        // 获取用户输入的文本和上传的文件
        String text = input.getText() == null ? "" : input.getText().trim();
        var filesSnapshot = List.copyOf(uploadedFiles); // 创建文件列表的快照

        // 移除欢迎消息（如果存在），因为用户已经开始对话
        messages.removeIf(this::isWelcomeMessage);

        // 添加用户消息到消息列表和当前会话
        ChatMessage userMessage = new ChatMessage(ChatMessage.Role.USER, buildUserPayload(text, filesSnapshot), LocalDateTime.now());
        messages.add(userMessage);
        currentSession.addMessage(userMessage);
        
        // 清空输入框和文件列表，准备下一次输入
        input.clear();
        uploadedFiles.clear();
        
        // 添加加载中的消息，提示用户AI正在处理
        ChatMessage loadingMessage = new ChatMessage(ChatMessage.Role.AI, "loading", LocalDateTime.now());
        messages.add(loadingMessage);
        
        // 禁用发送按钮并更改文本为"处理中..."，防止重复发送
        sendBtn.setDisable(true);
        String originalText = sendBtn.getText();
        sendBtn.setText("处理中...");
        
        // 保存当前会话的引用，用于后续验证
        ChatSession activeSession = currentSession;
        String activeThreadId = threadId;
        String activeSessionId = currentSession.getId(); // 保存会话ID

        // 在后台线程中异步调用AI接口（避免阻塞UI线程）
        new Thread(() -> {
            try {
                // 调用AI接口获取回复
                String aiResponse = callAiApi(text, activeThreadId);
                // 在JavaFX应用线程中更新UI（必须在UI线程中操作JavaFX组件）
                Platform.runLater(() -> {
                    // 检查当前会话是否仍是发起请求时的会话
                    if (currentSession == activeSession && threadId.equals(activeThreadId)) {
                        // 移除加载中的消息
                        messages.remove(loadingMessage);
                        // 添加AI回复（确保响应文本不为null）
                        String responseText = aiResponse != null ? aiResponse : "抱歉，未收到有效响应。";
                        ChatMessage aiMessage = new ChatMessage(ChatMessage.Role.AI, responseText, LocalDateTime.now());
                        messages.add(aiMessage);
                        currentSession.addMessage(aiMessage); // 保存到会话历史
                        // 刷新会话列表以更新标题（会话标题可能基于第一条消息生成）
                        sessionList.refresh();
                        // 恢复发送按钮状态
                        sendBtn.setText(originalText);
                        sendBtn.setDisable(false);
                        
                        // 保存会话到本地
                        saveCurrentSession();
                    } else {
                        // 如果当前会话已改变，将响应暂存到对应的会话ID
                        messages.remove(loadingMessage);
                        
                        // 恢复发送按钮状态
                        sendBtn.setText(originalText);
                        sendBtn.setDisable(false);
                        
                        // 保存当前会话
                        saveCurrentSession();
                        
                        // 将响应暂存到对应的会话ID
                        if (aiResponse != null) {
                            pendingResponses.put(activeSessionId, aiResponse);
                        }
                    }
                });
            } catch (Exception e) {
                // 捕获异常并显示错误消息
                e.printStackTrace();
                // 在UI线程中显示错误信息
                Platform.runLater(() -> {
                    // 检查当前会话是否仍是发起请求时的会话
                    if (currentSession == activeSession && threadId.equals(activeThreadId)) {
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
                        
                        // 保存会话到本地
                        saveCurrentSession();
                    } else {
                        // 如果当前会话已改变，将错误暂存到对应的会话ID
                        messages.remove(loadingMessage);
                        
                        // 恢复发送按钮状态
                        sendBtn.setText(originalText);
                        sendBtn.setDisable(false);
                        
                        // 保存当前会话
                        saveCurrentSession();
                        
                        // 将错误信息暂存到对应的会话ID
                        pendingErrors.put(activeSessionId, "抱歉，处理请求时发生错误，请稍后重试。");
                    }
                });
            }
        }).start();
    }

    /**
     * 调用AI接口，发送用户查询并获取AI回复
     * 
     * @param query 用户查询文本
     * @return AI回复内容，如果出错则返回错误提示信息
     */
    private String callAiApi(String query, String threadId) {
        try {
            // 构建请求URL（后端AI服务地址）
            URL url = new URL("http://localhost:8090/aiAgent/chat");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 构建请求参数
            Map<String, Object> queryMap = new HashMap<>();
            queryMap.put("query", query);
            queryMap.put("threadId", threadId);

            // 关键配置：设置请求方式和请求头，确保后台识别 JSON 格式（之前可能缺少这部分）
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");

            // 发送请求体到服务器
            try (java.io.OutputStream os = connection.getOutputStream()) {
                byte[] input = createJsonString(queryMap).getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 读取服务器响应
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 成功响应：读取响应内容
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    boolean firstLine = true;
                    // 逐行读取响应，保留换行符以支持Markdown格式
                    while ((responseLine = br.readLine()) != null) {
                        if (!firstLine) {
                            response.append("\n"); // 保留换行符，这对Markdown格式很重要
                        }
                        response.append(responseLine);
                        firstLine = false;
                    }
                    return response.toString();
                }
            } else {
                // HTTP错误响应
                return "抱歉，调用AI接口失败，错误码：" + responseCode;
            }
        } catch (Exception e) {
            // 捕获所有异常并返回友好的错误提示
            log.error(e.getMessage(),e);
            return "抱歉，调用AI接口时发生错误：" + e.getMessage();
        }
    }

    /**
     * 创建JSON字符串
     * 将Map转换为JSON格式的字符串
     * 
     * @param map 要转换的Map对象
     * @return JSON格式的字符串
     */
    private String createJsonString(Map<String, Object> map) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(escapeJson(entry.getKey())).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(escapeJson((String) value)).append("\"");
            } else {
                json.append(value.toString());
            }
            
            first = false;
        }
        
        json.append("}");
        return json.toString();
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
     * 将用户输入的文本和上传的文件信息组合成完整的消息内容
     * 
     * @param text 用户输入的文本
     * @param files 上传的文件列表
     * @return 构建后的消息内容字符串
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
     * 将消息列表滚动到最底部，显示最新的消息
     * 在添加新消息或切换会话时调用
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
     * 从文件名中提取扩展名并转换为小写
     * 
     * @param name 文件名（可能包含路径）
     * @return 文件扩展名（小写），如果没有扩展名则返回空字符串
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
     * 显示一个信息提示对话框，用于向用户展示错误或提示信息
     * 
     * @param title 对话框标题
     * @param content 对话框内容文本
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
     * 从资源文件加载图标，如果加载失败则使用fallback表情符号
     * 
     * @param resourcePath 图标资源路径（相对于resources目录）
     * @param fallbackEmoji 如果图标文件不存在时使用的fallback表情符号
     * @return 图标节点（ImageView或Text）
     */
    private static Node loadIcon(String resourcePath, String fallbackEmoji) {
        try {
            var url = ChatAssistantViewResult.class.getResource(resourcePath);
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
     * 负责渲染单个消息气泡，支持Markdown格式、加载动画等
     * 根据消息角色（用户/AI）显示不同的样式和对齐方式
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

            // 使用Markdown渲染器渲染消息内容
            // 根据消息角色设置不同的文本颜色：用户消息为蓝色，AI消息为灰色
            String textColor = item.getRole() == ChatMessage.Role.USER ? "#1e40af" : "#334155";
            VBox markdownContent = MarkdownRenderer.render(item.getText(), 16, textColor);
            markdownContent.setMaxWidth(600); // 设置最大宽度，防止消息气泡过宽
            markdownContent.getStyleClass().add("bubble-content");
                        
            // 创建复制按钮
            Button copyButton = new Button();
            copyButton.setGraphic(new Text("📋"));
            copyButton.getStyleClass().add("copy-button");
            copyButton.setTooltip(new Tooltip("复制此消息"));
            copyButton.setOnAction(e -> {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(item.getText());
                clipboard.setContent(content);
                                        
                // 临时改变按钮外观以提供视觉反馈
                String originalText = copyButton.getText();
                copyButton.setText("✓");
                copyButton.setTooltip(new Tooltip("已复制"));
                                        
                // 1秒后恢复原始图标
                new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
                    copyButton.setGraphic(new Text("📋"));
                    copyButton.setText("");
                    copyButton.setTooltip(new Tooltip("复制此消息"));
                })).play();
            });
                                    
            // 设置复制按钮的最小尺寸和样式
            copyButton.setMinSize(24, 24);
            copyButton.setMaxSize(24, 24);
            copyButton.setStyle("-fx-background-color: transparent; -fx-cursor: hand;" +
                              "-fx-text-fill: #999; -fx-font-size: 14px; -fx-padding: 0;" +
                              "-fx-border-color: transparent; -fx-alignment: center;");
                        
            // 创建时间标签
            Label timeLabel = new Label(formatTime(item.getCreatedAt()));
            timeLabel.getStyleClass().add("message-time");
            timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-opacity: 0.8;");
                        
            // 创建时间与复制按钮的容器（在同一行）
            HBox timeAndCopyContainer = new HBox(8, timeLabel, copyButton);
            timeAndCopyContainer.setAlignment(Pos.CENTER_LEFT);
                        
            // 将时间标签和复制按钮放在内容下方
            VBox messageWithTime = new VBox(markdownContent, timeAndCopyContainer);
            messageWithTime.setMaxWidth(600);
            messageWithTime.getStyleClass().add("bubble-content");
                        
            VBox finalMarkdownContent = messageWithTime;
                        
            // 创建消息气泡容器（VBox用于垂直布局，支持代码块等块级元素）
            var bubble = new VBox();
            bubble.getStyleClass().add("bubble");
            bubble.getChildren().add(finalMarkdownContent);
            bubble.setMaxWidth(600);
                        
            // 创建消息内容容器
            var contentContainer = new HBox();
            if (item.getRole() == ChatMessage.Role.USER) {
                // 用户消息：对齐到右侧
                contentContainer.getChildren().add(bubble);
                contentContainer.setAlignment(Pos.CENTER_RIGHT);
            } else {
                // AI消息：对齐到左侧
                contentContainer.getChildren().add(bubble);
                contentContainer.setAlignment(Pos.CENTER_LEFT);
            }
                        
            contentContainer.getStyleClass().add("message-content-wrapper");
            
            // 创建消息行容器（HBox用于水平布局，控制消息对齐方式）
            var row = new HBox();
            row.getStyleClass().add("bubble-row");
            // 监听父容器宽度变化，动态调整气泡最大宽度（响应式布局）
            row.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                if (newWidth.doubleValue() > 0) {
                    // 气泡最大宽度为父容器宽度的 90%，留出边距
                    double maxWidth = newWidth.doubleValue() * 0.9;
                    bubble.setMaxWidth(maxWidth);
                    finalMarkdownContent.setMaxWidth(maxWidth - 24); // 减去气泡的padding（左右各12px）
                    // 更新代码块的最大宽度（代码块需要额外的边距）
                    finalMarkdownContent.getChildren().forEach(child -> {
                        if (child instanceof Region && child.getStyleClass().contains("code-block")) {
                            ((Region) child).setMaxWidth(maxWidth - 48); // 减少更多的边距
                        }
                    });
                }
            });
            
            if (item.getRole() == ChatMessage.Role.USER) {
                row.setAlignment(Pos.TOP_RIGHT);
                bubble.getStyleClass().add("bubble-user");
            } else {
                row.setAlignment(Pos.TOP_LEFT);
                bubble.getStyleClass().add("bubble-ai");
            }
        
            row.getChildren().add(contentContainer);
        
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
     * 显示会话标题和更新时间，支持选中状态高亮
     * 时间显示格式：今天显示时间，昨天显示"昨天"，更早显示日期
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
            timeLabel.setText(ChatAssistantViewResult.formatTime(session.getUpdatedAt()));
            
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
    }
}

