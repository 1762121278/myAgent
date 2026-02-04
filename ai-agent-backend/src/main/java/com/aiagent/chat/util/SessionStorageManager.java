package com.aiagent.chat.util;

import com.aiagent.chat.model.ChatMessage;
import com.aiagent.chat.model.ChatSession;
import com.aiagent.chat.model.UploadedFileItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会话存储管理器
 * 负责将聊天会话数据持久化到本地文件系统
 * 
 * @author jiangtao.shu
 */
@Slf4j
@Component
public class SessionStorageManager {
    
    private static final String SESSION_FILE_EXTENSION = ".session";
    
    @Value("${session-storage-path:${user.home}/.chat-assistant/sessions}")
    private String storagePath;
    
    private Path sessionsDir;
    private final ObjectMapper objectMapper;
    
    /**
     * 默认构造函数，使用默认路径
     */
    public SessionStorageManager() {
        this(null);
    }
    
    /**
     * 带自定义路径的构造函数
     * @param customStoragePath 自定义存储路径，为null时使用默认路径
     */
    public SessionStorageManager(String customStoragePath) {
        this.objectMapper = new ObjectMapper();
        // 注册JavaTimeModule以支持LocalDateTime序列化
        this.objectMapper.registerModule(new JavaTimeModule());
        
        // 设置存储路径
        if (customStoragePath != null && !customStoragePath.isEmpty()) {
            this.storagePath = customStoragePath;
        } else {
            // 使用默认路径
            this.storagePath = System.getProperty("user.home") + "/.chat-assistant/sessions";
        }
        
        // 立即初始化（非Spring环境）
        initializeStorage();
    }
    
    @PostConstruct
    public void init() {
        // Spring环境初始化，storagePath已通过@Value注入
        initializeStorage();
    }
    
    /**
     * 初始化存储目录
     */
    private void initializeStorage() {
        // 解析路径，支持 ${user.home} 等占位符
        String resolvedPath = storagePath;
        if (resolvedPath.contains("${user.home}")) {
            resolvedPath = resolvedPath.replace("${user.home}", System.getProperty("user.home"));
        }
        this.sessionsDir = Paths.get(resolvedPath);
        
        // 确保目录存在
        ensureDirectoriesExist();
        log.info("会话存储路径: " + sessionsDir.toAbsolutePath());
    }
    
    /**
     * 确保必要的目录存在
     */
    private void ensureDirectoriesExist() {
        try {
            Files.createDirectories(sessionsDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建会话存储目录: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取当前存储路径
     */
    public String getStoragePath() {
        return sessionsDir.toAbsolutePath().toString();
    }
    
    /**
     * 保存单个会话到本地
     * 
     * @param session 要保存的会话
     */
    public void saveSession(ChatSession session) {
        try {
            Path sessionFile = sessionsDir.resolve(session.getId() + SESSION_FILE_EXTENSION);
            
            // 创建会话数据包装类
            SessionData sessionData = new SessionData();
            sessionData.setId(session.getId());
            sessionData.setTitle(session.getTitle());
            sessionData.setCreatedAt(session.getCreatedAt());
            sessionData.setUpdatedAt(session.getUpdatedAt());
            
            // 转换消息列表
            List<MessageData> messageDataList = session.getMessages().stream()
                    .map(this::convertToMessageData)
                    .collect(Collectors.toList());
            sessionData.setMessages(messageDataList);
            
            // 转换上传文件列表
            List<FileData> fileDataList = session.getUploadedFiles().stream()
                    .map(this::convertToFileData)
                    .collect(Collectors.toList());
            sessionData.setUploadedFiles(fileDataList);
            
            // 写入文件
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(sessionFile.toFile(), sessionData);
        } catch (IOException e) {
            throw new RuntimeException("保存会话失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 批量保存会话列表
     * 
     * @param sessions 会话列表
     */
    public void saveSessions(List<ChatSession> sessions) {
        for (ChatSession session : sessions) {
            saveSession(session);
        }
    }
    
    /**
     * 加载所有会话
     * 
     * @return 会话列表
     */
    public List<ChatSession> loadAllSessions() {
        List<ChatSession> sessions = new ArrayList<>();
        
        try {
            if (!Files.exists(sessionsDir)) {
                return sessions; // 目录不存在则返回空列表
            }
            
            Files.list(sessionsDir)
                    .filter(path -> path.toString().endsWith(SESSION_FILE_EXTENSION))
                    .forEach(sessionFile -> {
                        try {
                            ChatSession session = loadSessionFromFile(sessionFile.toFile());
                            if (session != null) {
                                sessions.add(session);
                            }
                        } catch (IOException e) {
                            System.err.println("加载会话文件失败: " + sessionFile + ", 错误: " + e.getMessage());
                        }
                    });
            
            // 按更新时间排序，最新的在前面
            sessions.sort((s1, s2) -> s2.getUpdatedAt().compareTo(s1.getUpdatedAt()));
            
        } catch (IOException e) {
            throw new RuntimeException("加载会话列表失败: " + e.getMessage(), e);
        }
        
        return sessions;
    }
    
    /**
     * 从文件加载单个会话
     * 
     * @param sessionFile 会话文件
     * @return 会话对象
     * @throws IOException 读取文件失败
     */
    private ChatSession loadSessionFromFile(File sessionFile) throws IOException {
        SessionData sessionData = objectMapper.readValue(sessionFile, SessionData.class);
        
        // 创建新的会话对象
        ChatSession session = new ChatSession(sessionData.getTitle());
        
        // 使用反射设置ID
        try {
            Field idField = ChatSession.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(session, sessionData.getId());
            
            Field createdAtField = ChatSession.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(session, sessionData.getCreatedAt());
            
            Field updatedAtField = ChatSession.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(session, sessionData.getUpdatedAt());
        } catch (Exception e) {
            throw new RuntimeException("无法设置会话属性: " + e.getMessage(), e);
        }
        
        // 设置消息
        List<ChatMessage> messages = sessionData.getMessages().stream()
                .map(this::convertFromMessageData)
                .collect(Collectors.toList());
        
        // 使用反射设置消息列表
        try {
            Field messagesField = ChatSession.class.getDeclaredField("messages");
            messagesField.setAccessible(true);
            messagesField.set(session, messages);
        } catch (Exception e) {
            throw new RuntimeException("无法设置会话消息: " + e.getMessage(), e);
        }
        
        // 设置上传文件
        List<UploadedFileItem> files = sessionData.getUploadedFiles().stream()
                .map(this::convertFromFileData)
                .collect(Collectors.toList());
        
        // 使用反射设置文件列表
        try {
            Field uploadedFilesField = ChatSession.class.getDeclaredField("uploadedFiles");
            uploadedFilesField.setAccessible(true);
            uploadedFilesField.set(session, files);
        } catch (Exception e) {
            throw new RuntimeException("无法设置会话文件: " + e.getMessage(), e);
        }
        
        return session;
    }
    
    /**
     * 删除指定会话（同时删除本地文件）
     * 
     * @param sessionId 会话ID
     * @return 是否成功删除
     */
    public boolean deleteSession(String sessionId) {
        try {
            Path sessionFile = sessionsDir.resolve(sessionId + SESSION_FILE_EXTENSION);
            boolean deleted = Files.deleteIfExists(sessionFile);
            if (deleted) {
                System.out.println("已删除会话文件: " + sessionFile);
            }
            return deleted;
        } catch (IOException e) {
            System.err.println("删除会话文件失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 清空所有会话数据
     */
    public void clearAllSessions() {
        try {
            if (!Files.exists(sessionsDir)) {
                return;
            }
            
            Files.list(sessionsDir)
                    .filter(path -> path.toString().endsWith(SESSION_FILE_EXTENSION))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            System.out.println("已删除会话文件: " + path);
                        } catch (IOException e) {
                            System.err.println("删除会话文件失败: " + path + ", 错误: " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("清空会话失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 转换消息对象为数据传输对象
     */
    private MessageData convertToMessageData(ChatMessage message) {
        MessageData data = new MessageData();
        data.setRole(message.getRole());
        data.setText(message.getText());
        data.setCreatedAt(message.getCreatedAt());
        return data;
    }
    
    /**
     * 从数据传输对象转换为消息对象
     */
    private ChatMessage convertFromMessageData(MessageData data) {
        return new ChatMessage(data.getRole(), data.getText(), data.getCreatedAt());
    }
    
    /**
     * 转换文件对象为数据传输对象
     */
    private FileData convertToFileData(UploadedFileItem file) {
        FileData data = new FileData();
        data.setName(file.getName());
        data.setPath(file.getFile().getAbsolutePath());
        data.setSize(file.getSizeBytes());
        data.setLastModified(file.getFile().lastModified());
        return data;
    }
    
    /**
     * 从数据传输对象转换为文件对象
     */
    private UploadedFileItem convertFromFileData(FileData data) {
        // 由于UploadedFileItem只接受File对象，我们创建一个文件引用
        File file = new File(data.getPath());
        if (!file.exists()) {
            // 如果原始文件不存在，创建一个虚拟文件
            file = new File(System.getProperty("java.io.tmpdir"), data.getName());
        }
        return new UploadedFileItem(file);
    }
    
    /**
     * 会话数据传输类
     */
    public static class SessionData {
        private String id;
        private String title;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<MessageData> messages = new ArrayList<>();
        private List<FileData> uploadedFiles = new ArrayList<>();
        
        // getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
        
        public List<MessageData> getMessages() { return messages; }
        public void setMessages(List<MessageData> messages) { this.messages = messages; }
        
        public List<FileData> getUploadedFiles() { return uploadedFiles; }
        public void setUploadedFiles(List<FileData> uploadedFiles) { this.uploadedFiles = uploadedFiles; }
    }
    
    /**
     * 消息数据传输类
     */
    public static class MessageData {
        private ChatMessage.Role role;
        private String text;
        private LocalDateTime createdAt;
        
        // getters and setters
        public ChatMessage.Role getRole() { return role; }
        public void setRole(ChatMessage.Role role) { this.role = role; }
        
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
    
    /**
     * 文件数据传输类
     */
    public static class FileData {
        private String name;
        private String path;
        private long size;
        private long lastModified;
        
        // getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        
        public long getLastModified() { return lastModified; }
        public void setLastModified(long lastModified) { this.lastModified = lastModified; }
    }
}
