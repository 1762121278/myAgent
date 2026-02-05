package com.aiagent.rag;


import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.messages.AgentCommand;
import com.alibaba.cloud.ai.graph.agent.hook.messages.UpdatePolicy;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

        // 3. 检索相关文档
        List<Document> relevantDocs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(userQuestion)
                        .topK(TOP_K)
                        // 只返回相似度>0.7的文档
                        .similarityThreshold(0.7)
                        .build()
        );

        // 如果没有检索到文档，直接返回原消息
        if (relevantDocs.isEmpty()) {
            return new AgentCommand(previousMessages);
        }

        // 4. 构建上下文字符串
        String context = relevantDocs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // 5. 构建新的消息列表（关键修改在这里）
        List<Message> modifiedMessages = new ArrayList<>();

        for (Message msg : previousMessages) {
            // 打印日志保留你的调试习惯
            log.debug("Actual class is: " + msg.getClass().getName());
            if (msg instanceof UserMessage) {
                // 【关键修改】如果是用户消息，我们将上下文拼接到用户的问题后面
                // 这样大模型就能看到："用户的问题 + 参考文档"
                String originalUserText = msg.getText();

                String ragInjectedUserMessage = originalUserText + "\n\n\n" +
                        String.format("""
                        === 参考上下文 ===
                        请基于以下已知信息回答问题。如果上下文中没有找到相关信息，请使用你自身的通用知识进行回答。
                        
                        %s
                        ==================
                        """, context);

                // 创建一个新的 UserMessage 替换原来的
                modifiedMessages.add(new UserMessage(ragInjectedUserMessage));
            } else {
                // 系统提示词或其他消息保持不变
                modifiedMessages.add(msg);
            }
        }

        // 6. 替换原消息
        return new AgentCommand(modifiedMessages, UpdatePolicy.REPLACE);
    }

    private String extractUserQuestion(List<Message> messages) {
// 从消息列表中提取最后一个用户消息
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            if (msg instanceof UserMessage) {
                return ((UserMessage) msg).getText();
            }
        }
        return null;
    }
}

