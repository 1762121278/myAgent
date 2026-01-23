
@echo off
set JAVA_HOME=D:\WorkSoftware\IDE\IDEA2024\APP\jdk-21.0.5
set PATH=%JAVA_HOME%\bin;%PATH%

set M2_HOME=C:\Users\wangfengzhang\.m2\repository
set JAVAFX_VERSION=21.0.5
set SLF4J_VERSION=1.7.36
set JAVAFX_PATH=%M2_HOME%\org\openjfx\javafx-base\%JAVAFX_VERSION%\javafx-base-%JAVAFX_VERSION%.jar;%M2_HOME%\org\openjfx\javafx-base\%JAVAFX_VERSION%\javafx-base-%JAVAFX_VERSION%-win.jar;%M2_HOME%\org\openjfx\javafx-graphics\%JAVAFX_VERSION%\javafx-graphics-%JAVAFX_VERSION%.jar;%M2_HOME%\org\openjfx\javafx-graphics\%JAVAFX_VERSION%\javafx-graphics-%JAVAFX_VERSION%-win.jar;%M2_HOME%\org\openjfx\javafx-controls\%JAVAFX_VERSION%\javafx-controls-%JAVAFX_VERSION%.jar;%M2_HOME%\org\openjfx\javafx-controls\%JAVAFX_VERSION%\javafx-controls-%JAVAFX_VERSION%-win.jar
set SLF4J_PATH=%M2_HOME%\org\slf4j\slf4j-api\%SLF4J_VERSION%\slf4j-api-%SLF4J_VERSION%.jar;%M2_HOME%\org\slf4j\slf4j-simple\%SLF4J_VERSION%\slf4j-simple-%SLF4J_VERSION%.jar
set CLASSPATH=%JAVAFX_PATH%;%SLF4J_PATH%

echo Compiling with JDK 21...
javac -d target\classes -cp "%CLASSPATH%" src\main\java\com\aiagent\app\ChatAssistantApp.java src\main\java\com\aiagent\model\*.java src\main\java\com\aiagent\ui\*.java

if %errorlevel% equ 0 (
    echo Compilation successful!
    echo Running application...
    java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls -cp "target\classes;%SLF4J_PATH%" com.aiagent.app.ChatAssistantApp
) else (
    echo Compilation failed!
    pause
)
