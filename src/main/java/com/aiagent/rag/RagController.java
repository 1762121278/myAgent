//package com.aiagent.rag;
//
//import jakarta.annotation.Resource;
//import lombok.Data;
//import lombok.Getter;
//import org.springframework.ai.document.Document;
//import org.springframework.ai.reader.TextReader;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.core.io.ByteArrayResource;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.http.ResponseEntity;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.charset.StandardCharsets;
//import java.util.List;
//import java.util.Map;
//import java.util.HashMap;
//import java.util.UUID;
//
///**
// * RAG文件上传和管理控制器
// *
// * @author jiangtao.shu
// */
//@RestController
//@RequestMapping("/rag")
//public class RagController {
//
//    @Resource
//    public VectorStore vectorStore;
//
//    /**
//     * 上传的文件信息映射，键为文件ID，值为文件信息
//     */
//    private final Map<String, UploadedFileInfo> uploadedFiles = new HashMap<>();
//
//    /**
//     * 上传RAG文档
//     */
//    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
//    public ResponseEntity<Map<String, Object>> uploadFile(
//            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
//
//        Map<String, Object> response = new HashMap<>(2);
//
//        try {
//            // 验证文件类型
//            String fileName = file.getOriginalFilename();
//            if (fileName == null || !isValidFileType(fileName)) {
//                response.put("success", false);
//                response.put("message", "不支持的文件类型。仅支持：.txt, .pdf, .doc, .docx");
//                return ResponseEntity.badRequest().body(response);
//            }
//
//            // 读取文件内容
//            byte[] contentBytes = file.getBytes();
//            String content = new String(contentBytes, StandardCharsets.UTF_8);
//
//            // 创建文档对象
//            Document document = new Document(content, Map.of("filename", fileName));
//
//            // 直接添加到向量存储
//            vectorStore.add(List.of(document));
//
//            // 生成文件ID并存储文件信息
//            String fileId = UUID.randomUUID().toString();
//            UploadedFileInfo fileInfo = new UploadedFileInfo(fileId, fileName, file.getSize(), System.currentTimeMillis());
//            uploadedFiles.put(fileId, fileInfo);
//
//            response.put("success", true);
//            response.put("message", "文件上传成功");
//            response.put("fileInfo", fileInfo);
//
//        } catch (IOException e) {
//            response.put("success", false);
//            response.put("message", "文件上传失败: " + e.getMessage());
//            return ResponseEntity.status(500).body(response);
//        }
//
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * 获取已上传的文件列表
//     */
//    @GetMapping("/files")
//    public ResponseEntity<Map<String, Object>> getUploadedFiles() {
//        Map<String, Object> response = new HashMap<>();
//        response.put("success", true);
//        response.put("files", uploadedFiles.values());
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * 删除上传的文件
//     */
//    @DeleteMapping("/files/{fileId}")
//    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable String fileId) {
//        Map<String, Object> response = new HashMap<>();
//
//        UploadedFileInfo fileInfo = uploadedFiles.get(fileId);
//        if (fileInfo != null) {
//            uploadedFiles.remove(fileId);
//            response.put("success", true);
//            response.put("message", "文件删除成功");
//        } else {
//            response.put("success", false);
//            response.put("message", "文件不存在");
//        }
//
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * 搜索与查询相关的文档
//     */
//    @PostMapping("/search")
//    public ResponseEntity<Map<String, Object>> searchDocuments(@RequestBody Map<String, String> request) {
//        Map<String, Object> response = new HashMap<>();
//
//        try {
//            String query = request.get("query");
//            if (query == null || query.trim().isEmpty()) {
//                response.put("success", false);
//                response.put("message", "查询内容不能为空");
//                return ResponseEntity.badRequest().body(response);
//            }
//
//            // 从向量存储中检索相关文档
//            List<Document> relevantDocs = vectorStore.similaritySearch(query);
//
//            response.put("success", true);
//            response.put("documents", relevantDocs);
//
//        } catch (Exception e) {
//            response.put("success", false);
//            response.put("message", "搜索失败: " + e.getMessage());
//            return ResponseEntity.status(500).body(response);
//        }
//
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * 验证文件类型
//     */
//    private boolean isValidFileType(String fileName) {
//        String lowerCaseName = fileName.toLowerCase();
//        return lowerCaseName.endsWith(".txt") ||
//               lowerCaseName.endsWith(".pdf") ||
//               lowerCaseName.endsWith(".doc") ||
//               lowerCaseName.endsWith(".docx");
//    }
//
//    /**
//     * 上传文件信息内部类
//     */
//    @Data
//    public static class UploadedFileInfo {
//        // Getter方法
//        private String id;
//        private String filename;
//        private long size;
//        private long uploadTime;
//
//        public UploadedFileInfo(String id, String filename, long size, long uploadTime) {
//            this.id = id;
//            this.filename = filename;
//            this.size = size;
//            this.uploadTime = uploadTime;
//        }
//
//        public String getFormattedSize() {
//            if (size < 1024) {
//                return size + " B";
//            }
//            if (size < 1024 * 1024) {
//                return String.format("%.2f KB", size / 1024.0);
//            }
//            return String.format("%.2f MB", size / (1024.0 * 1024.0));
//        }
//    }
//}
