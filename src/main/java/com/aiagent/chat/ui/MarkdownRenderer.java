package com.aiagent.chat.ui;

import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown渲染器，用于将Markdown文本转换为JavaFX节点
 * 支持代码块、行内代码、粗体、斜体等格式
 * @author jiangtao.shu
 */
public class MarkdownRenderer {
    /**
     *     代码块模式：支持语言标识符，如 ```java 或 ```python
     */
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(?:\\w+)?\\n?([\\s\\S]*?)```", Pattern.MULTILINE);
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)");
    
    /**
     * 将Markdown文本渲染为VBox节点（支持代码块）
     * @param markdown Markdown文本
     * @param baseFontSize 基础字体大小
     * @param textColor 文本颜色
     * @return VBox节点
     */
    public static VBox render(String markdown, double baseFontSize, String textColor) {
        VBox container = new VBox(8);
        container.setFillWidth(true);
        
        if (markdown == null || markdown.isEmpty()) {
            return container;
        }
        
        // 处理代码块和文本
        processContent(markdown, container, baseFontSize, textColor);
        
        return container;
    }
    
    /**
     * 处理内容，分离代码块和普通文本
     */
    private static void processContent(String text, VBox container, double baseFontSize, String textColor) {
        Matcher matcher = CODE_BLOCK_PATTERN.matcher(text);
        int lastEnd = 0;
        
        while (matcher.find()) {
            // 处理代码块之前的文本
            if (matcher.start() > lastEnd) {
                String beforeCode = text.substring(lastEnd, matcher.start());
                if (!beforeCode.trim().isEmpty()) {
                    TextFlow textFlow = createTextFlow(beforeCode, baseFontSize, textColor);
                    container.getChildren().add(textFlow);
                }
            }
            
            // 处理代码块
            String code = matcher.group(1);
            // 移除开头的换行符（如果有语言标识符）
            if (code.startsWith("\n")) {
                code = code.substring(1);
            }
            // 移除结尾的换行符
            if (code.endsWith("\n")) {
                code = code.substring(0, code.length() - 1);
            }
            Region codeBlock = createCodeBlock(code, baseFontSize);
            container.getChildren().add(codeBlock);
            
            lastEnd = matcher.end();
        }
        
        // 处理剩余的文本
        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd);
            if (!remaining.trim().isEmpty()) {
                TextFlow textFlow = createTextFlow(remaining, baseFontSize, textColor);
                container.getChildren().add(textFlow);
            }
        } else if (lastEnd == 0) {
            // 如果没有代码块，直接处理全部文本
            TextFlow textFlow = createTextFlow(text, baseFontSize, textColor);
            container.getChildren().add(textFlow);
        }
    }
    
    /**
     * 创建代码块节点
     */
    private static Region createCodeBlock(String code, double baseFontSize) {
        VBox codeBox = new VBox(4);
        codeBox.getStyleClass().add("code-block");
        codeBox.setStyle(
            "-fx-background-color: #1e293b; " +
            "-fx-background-radius: 8; " +
            "-fx-padding: 12; " +
            "-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace;"
        );
        
        // 按行处理代码
        String[] lines = code.split("\n", -1);
        for (String line : lines) {
            Text codeText = new Text(line);
            codeText.setStyle("-fx-fill: #e2e8f0;");
            codeText.setFont(Font.font("Consolas", baseFontSize * 0.9));
            codeBox.getChildren().add(codeText);
        }
        
        return codeBox;
    }
    
    /**
     * 创建文本流，处理行内格式
     */
    private static TextFlow createTextFlow(String text, double baseFontSize, String textColor) {
        TextFlow flow = new TextFlow();
        flow.setLineSpacing(4);
        flow.setMaxWidth(Region.USE_COMPUTED_SIZE);
        // 确保TextFlow可以换行
        flow.setPrefWidth(Region.USE_COMPUTED_SIZE);
        
        // 先处理行内代码
        processInlineCode(text, flow, baseFontSize, textColor);
        
        return flow;
    }
    
    /**
     * 处理行内代码
     */
    private static void processInlineCode(String text, TextFlow flow, double baseFontSize, String textColor) {
        Matcher codeMatcher = INLINE_CODE_PATTERN.matcher(text);
        int lastEnd = 0;
        
        while (codeMatcher.find()) {
            // 处理代码之前的文本
            if (codeMatcher.start() > lastEnd) {
                String beforeCode = text.substring(lastEnd, codeMatcher.start());
                processInlineFormatting(beforeCode, flow, baseFontSize, textColor);
            }
            
            // 处理行内代码
            String code = codeMatcher.group(1);
            Text codeText = new Text(code);
            codeText.setStyle(
                "-fx-fill: #dc2626; " +
                "-fx-background-color: #fef2f2; " +
                "-fx-background-radius: 4; " +
                "-fx-padding: 2 4 2 4; " +
                "-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; " +
                "-fx-font-size: " + (int)(baseFontSize * 0.9) + "px;"
            );
            codeText.getStyleClass().add("inline-code");
            flow.getChildren().add(codeText);
            
            lastEnd = codeMatcher.end();
        }
        
        // 处理剩余的文本
        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd);
            processInlineFormatting(remaining, flow, baseFontSize, textColor);
        } else if (lastEnd == 0) {
            // 如果没有行内代码，直接处理格式
            processInlineFormatting(text, flow, baseFontSize, textColor);
        }
    }
    
    /**
     * 处理行内格式（粗体、斜体）
     */
    private static void processInlineFormatting(String text, TextFlow flow, double baseFontSize, String textColor) {
        if (text == null || text.isEmpty()) {
            return;
        }
        
        // 处理粗体
        Matcher boldMatcher = BOLD_PATTERN.matcher(text);
        int lastEnd = 0;
        
        while (boldMatcher.find()) {
            // 处理粗体之前的文本
            if (boldMatcher.start() > lastEnd) {
                String beforeBold = text.substring(lastEnd, boldMatcher.start());
                processItalic(beforeBold, flow, baseFontSize, textColor);
            }
            
            // 处理粗体文本
            String boldText = boldMatcher.group(1);
            Text bold = new Text(boldText);
            bold.setFont(Font.font(null, FontWeight.BOLD, baseFontSize));
            bold.setStyle("-fx-fill: " + textColor + ";");
            flow.getChildren().add(bold);
            
            lastEnd = boldMatcher.end();
        }
        
        // 处理剩余的文本
        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd);
            processItalic(remaining, flow, baseFontSize, textColor);
        } else if (lastEnd == 0) {
            // 如果没有粗体，直接处理斜体
            processItalic(text, flow, baseFontSize, textColor);
        }
    }
    
    /**
     * 处理斜体
     */
    private static void processItalic(String text, TextFlow flow, double baseFontSize, String textColor) {
        if (text == null || text.isEmpty()) {
            return;
        }
        
        Matcher italicMatcher = ITALIC_PATTERN.matcher(text);
        int lastEnd = 0;
        
        while (italicMatcher.find()) {
            // 处理斜体之前的文本
            if (italicMatcher.start() > lastEnd) {
                String beforeItalic = text.substring(lastEnd, italicMatcher.start());
                addPlainText(beforeItalic, flow, baseFontSize, textColor);
            }
            
            // 处理斜体文本
            String italicText = italicMatcher.group(1);
            Text italic = new Text(italicText);
            italic.setFont(Font.font(null, FontPosture.ITALIC, baseFontSize));
            italic.setStyle("-fx-fill: " + textColor + ";");
            flow.getChildren().add(italic);
            
            lastEnd = italicMatcher.end();
        }
        
        // 处理剩余的文本
        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd);
            addPlainText(remaining, flow, baseFontSize, textColor);
        } else if (lastEnd == 0) {
            // 如果没有斜体，直接添加全部文本
            addPlainText(text, flow, baseFontSize, textColor);
        }
    }
    
    /**
     * 添加纯文本
     */
    private static void addPlainText(String text, TextFlow flow, double baseFontSize, String textColor) {
        if (text == null || text.isEmpty()) {
            return;
        }
        
        // 处理换行
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].isEmpty() || i == 0) {
                Text textNode = new Text(lines[i]);
                textNode.setFont(Font.font(null, baseFontSize));
                textNode.setStyle("-fx-fill: " + textColor + ";");
                flow.getChildren().add(textNode);
            }
            
            if (i < lines.length - 1) {
                Text newline = new Text("\n");
                flow.getChildren().add(newline);
            }
        }
    }
}
