@echo off
REM StrokeNet 正式版 APK 打包脚本
REM 使用方法：双击运行或在命令行执行 build-release.bat

echo ========================================
echo StrokeNet Release APK 构建脚本
echo ========================================
echo.

REM 检查 keystore.properties 是否存在
if not exist "keystore.properties" (
    echo [错误] 未找到 keystore.properties 文件
    echo.
    echo 请先配置签名信息：
    echo 1. 复制 keystore.properties.template 为 keystore.properties
    echo 2. 填写你的签名密钥信息
    echo 3. 或使用 Android Studio 生成签名 APK
    echo.
    echo 详见: docs\RELEASE_BUILD_GUIDE.md
    echo.
    pause
    exit /b 1
)

echo [1/3] 清理旧的构建文件...
call gradlew.bat clean
if errorlevel 1 (
    echo [错误] 清理失败
    pause
    exit /b 1
)

echo.
echo [2/3] 构建 Release APK...
call gradlew.bat assembleRelease
if errorlevel 1 (
    echo [错误] 构建失败
    pause
    exit /b 1
)

echo.
echo [3/3] 构建完成！
echo.
echo ========================================
echo APK 位置:
echo app\build\outputs\apk\release\app-release.apk
echo ========================================
echo.

REM 检查 APK 是否存在
if exist "app\build\outputs\apk\release\app-release.apk" (
    echo [成功] APK 已生成
    echo.
    
    REM 显示 APK 信息
    for %%A in ("app\build\outputs\apk\release\app-release.apk") do (
        echo 文件大小: %%~zA 字节
    )
    
    echo.
    echo 下一步：
    echo 1. 安装测试: adb install app\build\outputs\apk\release\app-release.apk
    echo 2. 授予权限: 见 README.md
    echo.
) else (
    echo [错误] APK 文件未生成
)

pause
