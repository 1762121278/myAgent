package com.aiagent.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * 简单的图标生成器，用于在运行时生成应用图标
 */
public class SimpleIconGenerator {
    
    /**
     * 生成应用图标
     * @return Image 对象
     */
    public static Image generateIcon() {
        int size = 256;
        WritableImage image = new WritableImage(size, size);
        PixelWriter writer = image.getPixelWriter();
        
        // 绘制背景（蓝色）
        Color bgColor = Color.web("#3b82f6");
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                writer.setColor(x, y, bgColor);
            }
        }
        
        // 绘制机器人头部（白色圆角矩形）
        int headX = 50;
        int headY = 50;
        int headWidth = 156;
        int headHeight = 156;
        Color headColor = Color.WHITE;
        Color borderColor = Color.web("#1e40af");
        
        // 填充头部（简化版，使用矩形）
        for (int y = headY + 15; y < headY + headHeight - 15; y++) {
            for (int x = headX + 15; x < headX + headWidth - 15; x++) {
                writer.setColor(x, y, headColor);
            }
        }
        
        // 绘制边框
        drawRect(writer, headX, headY, headWidth, headHeight, borderColor, 4);
        
        // 绘制眼睛
        drawCircle(writer, 100, 110, 10, Color.web("#3b82f6"));
        drawCircle(writer, 156, 110, 10, Color.web("#3b82f6"));
        
        // 绘制嘴巴（简单弧线）
        for (int x = 100; x <= 156; x++) {
            int y = (int)(145 + 10 * Math.sin((x - 100) * Math.PI / 56));
            if (y >= 0 && y < size && x >= 0 && x < size) {
                writer.setColor(x, y, Color.web("#3b82f6"));
                if (x > 100 && x < 156) {
                    writer.setColor(x, y + 1, Color.web("#3b82f6"));
                }
            }
        }
        
        // 绘制天线
        for (int y = 30; y < 50; y++) {
            for (int x = 120; x < 136; x++) {
                writer.setColor(x, y, Color.web("#1e40af"));
            }
        }
        drawCircle(writer, 128, 30, 5, Color.web("#1e40af"));
        
        return image;
    }
    
    private static void drawRect(PixelWriter writer, int x, int y, int width, int height, Color color, int strokeWidth) {
        // 绘制边框
        for (int i = 0; i < strokeWidth; i++) {
            // 上边
            for (int px = x; px < x + width; px++) {
                if (px >= 0 && px < 256 && y + i >= 0 && y + i < 256) {
                    writer.setColor(px, y + i, color);
                }
            }
            // 下边
            for (int px = x; px < x + width; px++) {
                if (px >= 0 && px < 256 && y + height - i >= 0 && y + height - i < 256) {
                    writer.setColor(px, y + height - i, color);
                }
            }
            // 左边
            for (int py = y; py < y + height; py++) {
                if (x + i >= 0 && x + i < 256 && py >= 0 && py < 256) {
                    writer.setColor(x + i, py, color);
                }
            }
            // 右边
            for (int py = y; py < y + height; py++) {
                if (x + width - i >= 0 && x + width - i < 256 && py >= 0 && py < 256) {
                    writer.setColor(x + width - i, py, color);
                }
            }
        }
    }
    
    private static void drawCircle(PixelWriter writer, int centerX, int centerY, int radius, Color color) {
        for (int y = centerY - radius; y <= centerY + radius; y++) {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                double dx = x - centerX;
                double dy = y - centerY;
                if (dx * dx + dy * dy <= radius * radius) {
                    if (x >= 0 && x < 256 && y >= 0 && y < 256) {
                        writer.setColor(x, y, color);
                    }
                }
            }
        }
    }
}
