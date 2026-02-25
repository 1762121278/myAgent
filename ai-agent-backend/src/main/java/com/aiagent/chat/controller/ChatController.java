package com.aiagent.chat.controller;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.aiagent.chat.util.MediaUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static com.alibaba.cloud.ai.dashscope.video.DashScopeVideoOptions.DEFAULT_MODEL;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createTempDirectory;

/**
 * @author jiangtao.shu
 */
@RestController
@RequestMapping("/aiAgent")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private static final Set<String> MULTIMODAL_MODELS = Set.of(
            "qwen-vl-plus", "qwen-vl-max", "qwen-vl-ocr",
            "qwen-audio-turbo", "qwen2-audio-instruct",
            "qwen2.5-omni", "qwen-omni","glm-4.7",
            "qwen3-vl-plus", "qwen3-vl-flash", "qwen3-omni-flash-realtime"
    );

    @Autowired
    private ReactAgent chatBotAgent;

    @Value("${spring.ai.dashscope.chat.options.model:deepseek-v3.2}")
    private String currentModel;


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

    @RequestMapping(value = "/chat/multimodal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String chatMultimodal(@RequestPart("query") String queryJson,
                                 @RequestPart(value = "files", required = false) MultipartFile[] files) throws Exception {
        log.info("multimodal query: {}", queryJson);
        JSONObject queryMap = JSON.parseObject(queryJson);
        String input = queryMap.getString("query");
        String threadId = queryMap.getString("threadId");
        List<Message> messages = createMutiMessage(input,files);
        RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();
        AssistantMessage response = chatBotAgent.call(messages,runnableConfig);
        log.info(response.toString());
        return response.getText();
    }

    private List<Message> createMutiMessage(String input, MultipartFile[] files) throws Exception {
        List<Message> messages = new ArrayList<>();

        if (files == null || files.length == 0) {
            UserMessage userMessage = UserMessage.builder()
                    .text(input)
                    .build();
            messages.add(userMessage);
            return messages;
        }

        boolean supportsMultimodal = isMultimodalModel(currentModel);
        log.info("当前模型: {}, 是否支持多模态: {}", currentModel, supportsMultimodal);

        List<Media> mediaList = new ArrayList<>();
        StringBuilder textContent = new StringBuilder(input != null ? input : "");
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            if (file.getSize() > 50 * 1024 * 1024) {
                throw new IllegalArgumentException("文件大小不能超过50MB: " + file.getOriginalFilename());
            }

            String fileName = file.getOriginalFilename();
            String contentType = file.getContentType();

            try {
                if (supportsMultimodal && (MediaUtil.isImageFile(contentType, fileName) || 
                    MediaUtil.isAudioFile(contentType, fileName) || 
                    MediaUtil.isVideoFile(contentType, fileName))) {
                    Media media = MediaUtil.createMedia(file);
                    mediaList.add(media);
                    log.info("媒体文件已添加到列表: {}", fileName);
                } else if (MediaUtil.isTextFile(contentType, fileName)) {
                    String content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    textContent.append("\n\n[文件: ").append(fileName).append("]\n");
                    if (content.length() > 10000) {
                        textContent.append(content.substring(0, 10000)).append("\n...（内容已截断）");
                    } else {
                        textContent.append(content);
                    }
                    log.info("文本文件内容已添加: {}", fileName);
                } else if (MediaUtil.isDocumentFile(contentType, fileName)) {
                    if (supportsMultimodal) {
                        Media media = MediaUtil.createMedia(file);
                        mediaList.add(media);
                        log.info("文档文件已添加到媒体列表: {}", fileName);
                    } else {
                        Path tempDir = createTempDirectory("aiagent_upload_");
                        String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
                        Path dest = tempDir.resolve(uniqueFileName);
                        try (var inputStream = file.getInputStream()) {
                            copy(inputStream, dest, StandardCopyOption.REPLACE_EXISTING);
                        }
                        textContent.append("\n\n[文档文件: ").append(fileName).append("]\n");
                        textContent.append("保存路径: ").append(dest.toString()).append("\n");
                        log.info("文档文件已保存: {} -> {}", fileName, dest);
                    }
                } else {
                    textContent.append("\n\n[文件: ").append(fileName).append("]\n");
                    textContent.append("类型: ").append(contentType).append(", 大小: ").append(file.getSize()).append(" bytes\n");
                    log.info("其他文件类型: {}", fileName);
                }
            } catch (Exception e) {
                log.error("处理文件失败: " + fileName, e);
                textContent.append("\n\n[文件处理失败: ").append(fileName).append(" - ").append(e.getMessage()).append("]");
            }
        }

        if (!mediaList.isEmpty()) {
            UserMessage userMessage = UserMessage.builder()
                    .text(textContent.toString())
                    .media(mediaList)
                    .build();
            messages.add(userMessage);
        } else {
            UserMessage userMessage = UserMessage.builder()
                    .text(textContent.toString())
                    .build();
            messages.add(userMessage);
        }
        
        return messages;
    }

    private boolean isMultimodalModel(String modelName) {
        if (modelName == null) {
            return false;
        }
        String lowerModel = modelName.toLowerCase();
        return MULTIMODAL_MODELS.stream()
                .anyMatch(m -> lowerModel.contains(m.toLowerCase()));
    }

    @PostMapping(value = "/stream/multimodal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamMultimodal(@RequestPart("query") String queryJson,
                                          @RequestPart(value = "files", required = false) MultipartFile[] files) throws Exception {
        return streamInternal(queryJson, files);
    }


    @PostMapping(value = "/stream", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamMultipart(@RequestPart("query") String queryJson,
                                        @RequestPart(value = "files", required = false) MultipartFile[] files) throws Exception {
        return streamInternal(queryJson, files);
    }

    // 向下兼容：如果客户端仍发送 application/json，则也接受并调用相同逻辑
    @PostMapping(value = "/stream", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamJson(@RequestBody String queryJson) throws Exception {
        return streamInternal(queryJson, null);
    }

    private Flux<String> streamInternal(String queryJson, MultipartFile[] files) throws Exception {
        log.info("stream query:{}", queryJson);
        JSONObject queryMap = JSON.parseObject(queryJson);
        String input = queryMap.getString("query");
        String threadId = queryMap.getString("threadId");

        List<Message> messages = createMutiMessage(input, files);

        RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();
        Flux<NodeOutput> stream = chatBotAgent.stream(messages, runnableConfig);
        return Flux.create(sink -> {
            stream.subscribe(
                    output -> {
                        if (output instanceof StreamingOutput streamingOutput) {
                            if (streamingOutput.getOutputType() == OutputType.AGENT_MODEL_STREAMING) {
                                // 发送 JSON 格式的流式数据
                                HashMap<String, String> map = new HashMap<>();
                                map.put("type", "chunk");
                                map.put("content", streamingOutput.message().getText());
                                sink.next(JSON.toJSONString(map));
                            }
                        }
                    },
                    sink::error,
                    () -> {
                        HashMap<String, String> map = new HashMap<>();
                        map.put("type", "end");
                        sink.next(JSON.toJSONString(map));
                        sink.complete();
                    }
            );
        });
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