package com.aiagent.app;

import com.aiagent.ui.ChatAssistantView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class ChatAssistantApp extends Application {
    private static final Logger log = LoggerFactory.getLogger(ChatAssistantApp.class);

    @Override
    public void start(Stage stage) {
        // Create ChatAssistantView instance directly
        ChatAssistantView root = new ChatAssistantView();
        Scene scene = new Scene(root, 1400, 800);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/styles/chat-assistant.css")).toExternalForm()
        );

        stage.setTitle("智能对话助手");
        stage.setMinWidth(800);
        stage.setMinHeight(600);
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

