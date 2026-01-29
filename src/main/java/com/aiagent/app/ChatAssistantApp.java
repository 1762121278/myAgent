package com.aiagent.app;

import com.aiagent.chat.ui.ChatAssistantView;
import com.aiagent.chat.util.SimpleIconGenerator;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author jiangtao.shu
 */
public class ChatAssistantApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(ChatAssistantApp.class);

    @Override
    public void start(Stage stage) {
        // Create ChatAssistantView instance directly
        ChatAssistantView root = new ChatAssistantView();
        // 窗口初始大小设为原来的80% (1400*0.8=1120, 800*0.8=640)
        Scene scene = new Scene(root, 1120, 640);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/styles/chat-assistant.css")).toExternalForm()
        );

        stage.setTitle("智能对话助手");
        stage.setMinWidth(800);
        stage.setMinHeight(600);
        
        // 设置窗口图标
        try {
            // 优先使用 app-icon.png
            var iconUrl = getClass().getResource("/icons/app-icon.png");
            if (iconUrl != null) {
                stage.getIcons().add(new Image(iconUrl.toExternalForm()));
                log.info("已加载应用图标: app-icon.png");
            } else {
                // 如果PNG不存在，尝试使用现有的 airobot.jpg
                var fallbackIconUrl = getClass().getResource("/icons/airobot.jpg");
                if (fallbackIconUrl != null) {
                    stage.getIcons().add(new Image(fallbackIconUrl.toExternalForm()));
                    log.info("使用备用图标: airobot.jpg");
                } else {
                    // 如果都不存在，使用程序生成的图标
                    Image generatedIcon = SimpleIconGenerator.generateIcon();
                    stage.getIcons().add(generatedIcon);
                    log.info("使用程序生成的图标");
                }
            }
        } catch (Exception e) {
            log.warn("加载应用图标失败: " + e.getMessage() + "，使用程序生成的图标");
            try {
                Image generatedIcon = SimpleIconGenerator.generateIcon();
                stage.getIcons().add(generatedIcon);
            } catch (Exception ex) {
                log.warn("生成图标失败: " + ex.getMessage());
            }
        }
        
        stage.setScene(scene);
        stage.show();
        
        // Stop Spring Boot application when JavaFX application exits
        stage.setOnCloseRequest(e -> {
            log.info("关闭智能对话助手");
            SpringBootApp.stop();
        });
    }

    public static void main(String[] args) {
        log.info("启动智能对话助手");
        // Start Spring Boot application first
        SpringBootApp.start(args);
        // Then launch JavaFX application
        launch(args);
    }
}

