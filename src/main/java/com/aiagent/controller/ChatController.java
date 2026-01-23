package com.aiagent.controller;


import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author jiangtao.shu
 */
@RestController
@RequestMapping("/aiAgent")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ReactAgent chatBotAgent;



    @RequestMapping("/chat")
    public String chatText(@RequestBody String queryJson) throws GraphRunnerException {
        log.info("query:{}", queryJson);
        //将JSON字符串转换为Map
        JSONObject queryMap = JSON.parseObject(queryJson);
        String input = queryMap.getString("query");
        //同一个会话ID，用于关联上下文会话
        String threadId = queryMap.getString("threadId");
        RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();
        AssistantMessage response = chatBotAgent.call(input,runnableConfig);
        log.info(response.toString());
        return response.getText();
    }

    @RequestMapping("/chatStream")
    public String chatStram(@RequestBody String queryJson) throws GraphRunnerException {
        log.info("query:{}", queryJson);
        //将JSON字符串转换为Map
        JSONObject queryMap = JSON.parseObject(queryJson);
        String input = queryMap.getString("query");
        //同一个会话ID，用于关联上下文会话
        String threadId = queryMap.getString("threadId");
        RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();
        Optional<OverAllState> result = chatBotAgent.invoke(input,runnableConfig);
        log.info("result:{}", result.toString());
        if (result.isPresent()) {
            OverAllState state = result.get();
            // 访问消息历史
            List<Message> messages = state.value("messages", new ArrayList<>());
            // 访问其他状态信息
            System.out.println(state);
        }
        return result.toString();
    }

}
