# 应用图标说明

## 图标文件

应用图标文件应命名为 `app-icon.png`，放置在 `src/main/resources/icons/` 目录下。

## 创建图标

已提供 SVG 格式的图标设计文件：`app-icon.svg`

### 方法1：使用在线工具转换
1. 访问 https://convertio.co/svg-png/
2. 上传 `app-icon.svg` 文件
3. 设置输出尺寸为 256x256 像素
4. 下载转换后的 PNG 文件
5. 重命名为 `app-icon.png` 并放置在 `src/main/resources/icons/` 目录

### 方法2：使用 ImageMagick
```bash
magick app-icon.svg -resize 256x256 app-icon.png
```

### 方法3：使用 Inkscape
```bash
inkscape app-icon.svg --export-png=app-icon.png --export-width=256
```

## 图标设计说明

- **尺寸**: 256x256 像素
- **背景色**: #3b82f6（蓝色）
- **机器人头部**: 白色圆角矩形，蓝色边框
- **眼睛**: 两个蓝色圆形
- **嘴巴**: 蓝色弧形（微笑）
- **天线**: 顶部蓝色矩形和圆形

## 如果图标文件不存在

如果 `app-icon.png` 不存在，应用将使用默认的 JavaFX 图标。
