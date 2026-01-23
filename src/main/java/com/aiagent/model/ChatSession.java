package com.aiagent.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 聊天会话模型类
 * @author jiangtao.shu
 */
public final class ChatSession {
    private final String id;
    private String title;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final List<ChatMessage> messages;
    private final List<UploadedFileItem> uploadedFiles;

    public ChatSession() {
        this.id = UUID.randomUUID().toString();
        this.title = "新会话";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.messages = new ArrayList<>();
        this.uploadedFiles = new ArrayList<>();
    }

    public ChatSession(String title) {
        this.id = UUID.randomUUID().toString();
        this.title = title == null || title.trim().isEmpty() ? "新会话" : title;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.messages = new ArrayList<>();
        this.uploadedFiles = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title == null || title.trim().isEmpty() ? "新会话" : title;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<ChatMessage> getMessages() {
        return new ArrayList<>(messages);
    }
    
    /**
     * 获取消息列表的直接引用（用于内部操作）
     * @return 消息列表
     */
    List<ChatMessage> getMessagesDirect() {
        return messages;
    }

    public void addMessage(ChatMessage message) {
        messages.add(Objects.requireNonNull(message, "message"));
        this.updatedAt = LocalDateTime.now();
        updateTitleFromMessages();
    }
    
    public void clearMessages() {
        messages.clear();
    }
    
    public void setMessages(List<ChatMessage> newMessages) {
        messages.clear();
        messages.addAll(newMessages);
        this.updatedAt = LocalDateTime.now();
    }

    public List<UploadedFileItem> getUploadedFiles() {
        return new ArrayList<>(uploadedFiles);
    }
    
    /**
     * 获取上传文件列表的直接引用（用于内部操作）
     * @return 上传文件列表
     */
    List<UploadedFileItem> getUploadedFilesDirect() {
        return uploadedFiles;
    }

    public void addUploadedFile(UploadedFileItem file) {
        uploadedFiles.add(Objects.requireNonNull(file, "file"));
    }

    public void clearUploadedFiles() {
        uploadedFiles.clear();
    }
    
    public void setUploadedFiles(List<UploadedFileItem> newFiles) {
        uploadedFiles.clear();
        uploadedFiles.addAll(newFiles);
    }

    /**
     * 根据消息内容自动更新会话标题
     */
    private void updateTitleFromMessages() {
        if (title.equals("新会话") && !messages.isEmpty()) {
            // 找到第一条用户消息作为标题
            for (ChatMessage msg : messages) {
                if (msg.getRole() == ChatMessage.Role.USER) {
                    String text = msg.getText();
                    if (text != null && !text.trim().isEmpty()) {
                        // 取前30个字符作为标题
                        String newTitle = text.trim();
                        if (newTitle.length() > 30) {
                            newTitle = newTitle.substring(0, 30) + "...";
                        }
                        this.title = newTitle;
                        break;
                    }
                }
            }
        }
    }
}
