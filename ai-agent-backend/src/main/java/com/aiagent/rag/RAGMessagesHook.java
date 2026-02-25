package com.aiagent.rag;

import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description: 创建 RAG Hook：在模型调用前检索文档并添加到消息中
 * @Author: jiangtao.shu
 */
@Slf4j
@HookPositions({HookPosition.BEFORE_MODEL})
public class RAGMessagesHook extends MessagesModelHook {

    private final VectorStore vectorStore;
    private static final int TOP_K = 5;

    public RAGMessagesHook(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public String getName() {
        return "rag_messages_hook";
    }

    @Override
    public AgentCommand beforeModel(List<Message> previousMessages, RunnableConfig config) {
        // 1. 防御性检查
        if (previousMessages == null || previousMessages.isEmpty()) {
            return new AgentCommand(previousMessages);
        }

        // 2. 提取用户问题（用于检索）
        String userQuestion = extractUserQuestion(previousMessages);
        if (userQuestion == null || userQuestion.isEmpty()) {
            return new AgentCommand(previousMessages);
        }

        // 【新增】3. 简单路由判断（可选）
        if (!needRetrieval(userQuestion)) {
            log.debug("简单问题，跳过检索");
            return new AgentCommand(previousMessages);
        }

        // 4. 检索相关文档（添加异常处理）
        List<Document> relevantDocs;
        try {
            relevantDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(userQuestion)
                            .topK(TOP_K)
//                            .similarityThreshold(0.7)
                            .build()
            );
        } catch (Exception e) {
            log.error("检索失败: {}", e.getMessage());
            // 失败时返回原消息
            return new AgentCommand(previousMessages);
        }

        // 如果没有检索到文档，直接返回原消息
        if (relevantDocs.isEmpty()) {
            return new AgentCommand(previousMessages);
        }

        // 7. 构建上下文字符串（优化格式）
        String context = buildContext(relevantDocs);

        // 8. 构建新的消息列表
        List<Message> modifiedMessages = buildMessagesWithContext(previousMessages, context, userQuestion);

        // 9. 替换原消息
        return new AgentCommand(modifiedMessages, UpdatePolicy.REPLACE);
    }

    /**
     * 提取消息中的文件内容
     * @param messages 用户消息
     * @param content 检索内容
     */
    private void extractFileContent(List<Message> messages, StringBuilder content) {
        for (Message msg : messages) {
            if (msg instanceof UserMessage userMsg) {
                // 1. 检查是否有 Media 对象
                List<Media> mediaList = userMsg.getMedia();
                if (mediaList != null && !mediaList.isEmpty()) {
                    for (Media media : mediaList) {
                        // 提取媒体内容（如果有）
                        // 注意：这里需要根据实际的 Media 实现来提取内容
                        // 例如，如果 Media 包含文件路径或内容，可以在这里处理
                        log.debug("发现媒体文件: {}", media);
                        // 暂时添加媒体文件信息到检索内容中
                        content.append("\n[媒体文件]");
                    }
                }
                
                // 2. 检查消息文本中是否包含文件上传信息（流式接口的情况）
                String messageText = userMsg.getText();
                if (messageText != null && (messageText.contains("[Uploaded files]") || messageText.contains("[多模态文件上传]"))) {
                    log.debug("发现文件上传信息在消息文本中");
                    // 这里可以选择是否将文件信息添加到检索内容中
                    // 由于流式接口已经将文件信息添加到了 input 中，
                    // 而 input 已经被作为 userQuestion 提取，所以这里可以不重复添加
                }
            }
        }
    }

    /**
     * 【新增】简单路由判断
     */
    private boolean needRetrieval(String question) {
        // 可以根据需要添加更多规则
        String lower = question.toLowerCase();
        // 排除简单问候
        if (lower.contains("你好") || lower.contains("hi") || lower.contains("hello")) {
            return false;
        }
        // 问题太短可能不需要检索
        if (question.length() < 3) {
            return false;
        }
        // 默认需要检索
        return true;
    }

    /**
     * 【优化】构建更好的上下文格式
     */
    private String buildContext(List<Document> docs) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < docs.size(); i++) {
            String text = docs.get(i).getText();
            // 截断过长的文档
            assert text != null;
            if (text.length() > 200) {
                text = text.substring(0, 200) + "...";
            }
            sb.append(String.format("【文档%d】%s\n\n", i + 1, text));
        }
        return sb.toString();
    }

    /**
     * 【优化】构建消息 - 两种方案供选择
     */
    private List<Message> buildMessagesWithContext(List<Message> original, String context, String userQuestion) {
        List<Message> modified = new ArrayList<>();

        // 方案A：保持您的原样（上下文在用户消息）
        // return buildUserMessageContext(original, context);

        // 方案B：推荐 - 上下文在系统消息（效果更好）
        return buildSystemMessageContext(original, context, userQuestion);
    }

    /**
     * 方案A：上下文放在用户消息（保持您原有逻辑）
     */
    private List<Message> buildUserMessageContext(List<Message> original, String context) {
        List<Message> modified = new ArrayList<>();

        for (Message msg : original) {
            if (msg instanceof UserMessage) {
                String userText = msg.getText();
                String enhanced = userText + "\n\n参考信息：\n" + context;
                modified.add(new UserMessage(enhanced));
            } else {
                modified.add(msg);
            }
        }
        return modified;
    }

    /**
     * 方案B：上下文放在系统消息（推荐）
     * @param original :之前的对话消息
     * @param context ：数据库检索到的知识
     * @param userQuestion ：用户的最后一次提问
     * @return List
     */
    private List<Message> buildSystemMessageContext(List<Message> original, String context, String userQuestion) {
        List<Message> modified = new ArrayList<>();

        // 1. 先添加系统消息
        String systemPrompt = String.format("""
            请基于以下参考信息回答问题：
            %s
            
            如果参考信息中没有相关内容，请使用你的通用知识。
            """, context);

        for (Message message : original) {
            if (message instanceof UserMessage) {
                modified.add(message);
            }else if (message instanceof SystemMessage) {
                modified.add(new SystemMessage(systemPrompt));
            }else {
                modified.add(message);
            }
        }

        // 2. 添加原始消息（排除原来的系统消息）
        modified.addAll(original);

        return modified;
    }

    private String extractUserQuestion(List<Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg instanceof UserMessage) {
                return ((UserMessage) msg).getText();
            }
        }
        return null;
    }
}