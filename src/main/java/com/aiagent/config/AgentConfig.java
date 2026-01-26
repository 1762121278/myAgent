package com.aiagent.config;


import com.aiagent.outputSchema.TextAnalysisResult;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
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

    @Value("${spring.ai.dashscope.chat.options.model}")
    private String model;

    @Bean
    public ReactAgent chatBotAgent() {
        // 创建 DashScope API 实例
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(apiKey)
                .build();

        // 创建模型实例(例如指定模型名称，通义千问还是deepseek等)
        ChatModel chatModel = DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(DashScopeChatOptions.builder()
                        //指定模型名称
                        .model(model)
                        .build())
                .build();

        //创建大模型系统提示基础配置
        String systemPrompt = "你是一名专业的运维助手。请准确、简洁地回答问题。";

        //使用 instruction提供详细指令
        String instruction = """
                         你是一个经验丰富的软件运维师。在回答问题时，请：\
                          1. 首先理解用户的核心需求\
                          2. 分析可能的技术方案\
                          3. 提供清晰的建议和理由\
                          4. 如果需要更多信息，主动询问\
                          保持专业、友好的语气。\
                        """;

        //创建标准结构输，例如json，使用 BeanOutputConverter 生成 outputSchema
        BeanOutputConverter<TextAnalysisResult> outputConverter = new BeanOutputConverter<>(TextAnalysisResult.class);
        String responseFormat = outputConverter.getFormat();

        //消息保存的策略,本地可默认使用 MemorySaver
        //生产环境：使用 RedisSaver、MongoSaver 等持久化存储替代 MemorySaver。
        //       ReactAgent.builder()
        //                .name("运维助手")
        //                .saver(new MemorySaver())

        // 创建 Agent
        return ReactAgent.builder()
                .name("运维助手")
                .saver(new MemorySaver())
                .model(chatModel)
                //系统提示基础配置
                .systemPrompt(systemPrompt)
                //使用 instruction提供详细指令
                .instruction(instruction)
//                .outputSchema(responseFormat)
                .build();
    }
}
