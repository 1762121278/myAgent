//package com.aiagent.rag;
//
//
//import org.springframework.ai.embedding.EmbeddingModel;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * RAG配置类
// *
// * @author jiangtao.shu
// */
//@Configuration
//public class RagConfig {
//
//
//    @Value("${spring.ai.dashscope.embedding.options.model:text-embedding-v2}")
//    private String embeddingModel;
//
//
//    // 由于SimpleVectorStore可能需要EmbeddingModel参数，
//    // 我们依赖Spring AI的自动配置来创建VectorStore Bean
//    // 或者通过application.yml配置来启用自动配置
//}
