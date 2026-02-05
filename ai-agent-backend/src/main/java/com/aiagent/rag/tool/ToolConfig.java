package com.aiagent.rag.tool;

import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Description: TODO
 * @Author: jiangtao.shu
 */

@Component
public class ToolConfig {

    @Autowired
    @Qualifier("milvusVectorStore")
    private VectorStore vectorStore;

    public class DocumentSearchTool {

        private final VectorStore vectorStore;

        public DocumentSearchTool(VectorStore vectorStore) {
            this.vectorStore = vectorStore;
        }

        public record Request(String query) {}
        public record Response(String content) {}

        public Response search(Request request) {
            List<Document> docs = vectorStore.similaritySearch(
                    org.springframework.ai.vectorstore.SearchRequest.builder()
                            .query(request.query())
                            .topK(5)
                            .build()
            );
            String content = docs.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining(""));
            return new Response(content);
        }
    }

    /**
     * 网络搜索工具（示例）
     */
    class WebSearchTool {
        public record Request(String query) {}
        public record Response(String content) {}

        public Response search(Request request) {
            // 实际实现中调用网络搜索 API
            return new Response("网络搜索结果: " + request.query());
        }
    }

    DocumentSearchTool docSearchTool = new DocumentSearchTool(vectorStore);
    WebSearchTool webSearchTool = new WebSearchTool();

    ToolCallback documentSearchCallback = FunctionToolCallback.builder("document_search",
                    (Function<DocumentSearchTool.Request, DocumentSearchTool.Response>)
                            req -> docSearchTool.search(req))
            .description("从文档库中搜索相关信息")
            .inputType(DocumentSearchTool.Request.class)
            .build();

    ToolCallback webSearchCallback = FunctionToolCallback.builder("web_search",
                    (Function<WebSearchTool.Request, WebSearchTool.Response>)
                            req -> webSearchTool.search(req))
            .description("从互联网搜索最新信息")
            .inputType(WebSearchTool.Request.class)
            .build();
}
