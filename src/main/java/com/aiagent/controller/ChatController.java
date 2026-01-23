package com.aiagent.controller;


import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
    public String chat(@RequestBody Map<String, String> query) throws GraphRunnerException {
        log.info("query:{}", query);
        AssistantMessage response = chatBotAgent.call(query.get("query"));
        log.info(response.toString());
        return response.getText();
    }

}
