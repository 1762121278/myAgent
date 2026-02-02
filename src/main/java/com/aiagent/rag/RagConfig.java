package com.aiagent.rag;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import java.util.List;

/**
 * RAG配置类
 *
 * @author jiangtao.shu
 */
@Configuration
public class RagConfig{

    @Autowired(required=false)
    VectorStore vectorStore;

    @PostConstruct
    public void init() {
        try {
            // 检查vectorStore是否为null
            if (vectorStore == null) {
                System.out.println("VectorStore 未注入，可能尚未初始化完成或配置有误");
                return;
            }

            // 1. 加载文档
            Resource resource = new ClassPathResource("ragfile/phoneNumber.txt");
            TextReader textReader = new TextReader(resource);
            List<Document> documents = textReader.get();

            // 2. 分割文档为块
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> chunks = splitter.apply(documents);

            // 3. 将块添加到向量存储
            vectorStore.add(chunks);

            System.out.println("成功将文档添加到 Milvus 向量存储");

            // 测试检索功能
            List<Document> results = vectorStore.similaritySearch("查询文本");
            System.out.println("检索结果数量: " + results.size());
        } catch (Exception e) {
            System.err.println("RAG 初始化过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}