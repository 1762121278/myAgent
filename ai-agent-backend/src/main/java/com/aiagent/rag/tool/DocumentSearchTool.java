package com.aiagent.rag.tool;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description: TODO
 * @Author: jiangtao.shu
 */
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

    class WebSearchTool {
        public record Request(String query) {}
        public record Response(String content) {}

        public Response search(Request request) {
            // 实际实现中调用网络搜索 API
            return new Response("网络搜索结果: " + request.query());
        }
    }
}
