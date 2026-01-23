package com.aiagent.config;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.stereotype.Component;

/**
 * @author jiangtao.shu
 */
@Component
public class AgentConfig {

    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;

    @Bean
    public ReactAgent chatBotAgent() {
        // 创建 DashScope API 实例
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(apiKey)
                .build();
        // 创建模型实例
        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .build();

        // 创建 Agent
        return ReactAgent.builder()
                .name("运维助手")
                .saver(new MemorySaver())
                .model(chatModel)
                .instruction("你是一名运维助手.")
                .build();
    }
}
