package com.aiagent.rag;

import com.alibaba.fastjson.JSON;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Milvus VectorStore 配置类
 * 修复了代码结构错误（确保所有Bean定义在类内部，括号闭合）
 *
 * @author jiangtao.shu
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MilvusConfig {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Value("${spring.ai.vectorstore.milvus.client.host:localhost}")
    private String milvusHost;

    @Value("${spring.ai.vectorstore.milvus.client.port:19530}")
    private int milvusPort;

    @Value("${spring.ai.vectorstore.milvus.client.username:}")
    private String username;

    @Value("${spring.ai.vectorstore.milvus.client.password:}")
    private String password;

    @Value("${spring.ai.vectorstore.milvus.databaseName:default}")
    private String databaseName;

    private final ObjectProvider<MilvusServiceClient> milvusClientProvider;

    // 定义全局map缓存 VectorStore，key 是 collectionName（每个知识库一个 collection）
    private final Map<String, VectorStore> vectorStoreCache = new ConcurrentHashMap<>();


    @Value("${spring.ai.vectorstore.milvus.collectionName}")
    private String collectionName;

    @Value("${spring.ai.vectorstore.milvus.embeddingDimension:1536}")
    private int embeddingDimension;

    /**
     * 创建 Milvus 客户端
     */
    @Bean
    public MilvusServiceClient milvusClient() {
        log.info("初始化 Milvus 客户端: {}:{}, 数据库: {}", milvusHost, milvusPort, databaseName);

        ConnectParam.Builder builder = ConnectParam.newBuilder()
                .withHost(milvusHost)
                .withPort(milvusPort)
                .withDatabaseName(databaseName);

        return new MilvusServiceClient(builder.build());
    }


    @Bean("milvusVectorStore")
    public MilvusVectorStore getVectorStore() {

        // 通过ObjectProvider获取client
        MilvusServiceClient milvusClient = milvusClientProvider.getIfAvailable();
        if (milvusClient == null) {
            System.err.println("无法获取 Milvus 客户端");
            return null;
        }

        // 调用 client 创建集合
        MilvusVectorStore vectorStore = MilvusVectorStore.builder(milvusClient, embeddingModel)
                .databaseName(databaseName)
                .collectionName(collectionName)
                .embeddingDimension(embeddingDimension)
                .initializeSchema(true)
                .build();

        // 调用 afterPropertiesSet() 触发 Milvus collection 的创建,
        // 注意：新创建的 collection 没有索引，会有 "index not found" 的错误日志，这是正常的
        // 索引会在第一次插入数据时自动创建
        try {
            vectorStore.afterPropertiesSet();
            vectorStoreCache.put(collectionName, vectorStore);
        } catch (Exception e) {
            // 忽略索引检查相关的错误，这不影响功能
            log.debug("VectorStore 初始化警告（可忽略）: {}", e.getMessage());
        }
        return vectorStore;
    }

    public void init(MilvusVectorStore vectorStore) {
        try {
            // 1. 加载文档
            Resource resource = new ClassPathResource("ragfile/phoneNumber.txt");
            if (!resource.exists()) {
                log.error("文档文件不存在: ragfile/phoneNumber.txt");
                return ;
            }

            TextReader textReader = new TextReader(resource);
            List<Document> documents = textReader.get();

            // 2. 分割文档为块
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> chunks = splitter.apply(documents);

            // 3. 将块添加到向量存储
            vectorStore.add(chunks);

            log.info("成功将文档添加到 Milvus 向量存储");
            log.info("处理的文档块数量: " + chunks.size());

            // 测试检索功能
            List<Document> results = vectorStore.similaritySearch("舒江涛的电话号码");
            log.info("检索结果数量: " + results.size());
            if (!results.isEmpty()) {
                log.info("第一个检索结果: " + JSON.toJSONString(results.get(0)));
            }

        } catch (Exception e) {
            log.error("RAG 初始化过程中发生错误: " + e.getMessage());
        }
    }

}