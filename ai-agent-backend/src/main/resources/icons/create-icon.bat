@echo off
REM 使用ImageMagick将SVG转换为PNG（如果已安装）
REM 或者使用在线工具：https://convertio.co/svg-png/

if exist "C:\Program Files\ImageMagick-7.*\magick.exe" (
    "C:\Program Files\ImageMagick-7.*\magick.exe" app-icon.svg -resize 256x256 app-icon.png
    echo 图标已创建: app-icon.png
) else (
    echo ImageMagick未安装，请使用以下方法之一：
    echo 1. 在线转换: https://convertio.co/svg-png/
    echo 2. 安装ImageMagick: https://imagemagick.org/script/download.php
    echo 3. 使用Inkscape: inkscape app-icon.svg --export-png=app-icon.png --export-width=256
)
