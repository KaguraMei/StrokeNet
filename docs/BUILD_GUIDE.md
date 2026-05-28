# 构建和部署指南

## 前置要求

### 开发环境
- **JDK 11** 或更高版本
- **Android SDK** (API 31+)
- **Gradle** (项目自带 Gradle Wrapper)

### 可选工具
- **Android Studio** - 推荐用于开发和调试
- **ADB (Android Debug Bridge)** - 用于命令行部署和测试

## 方法一：使用 Android Studio（推荐）

### 1. 导入项目
1. 打开 Android Studio
2. 选择 `File → Open`
3. 选择项目根目录
4. 等待 Gradle 同步完成

### 2. 配置设备
- **真机**：通过 USB 连接，启用开发者选项和 USB 调试
- **模拟器**：创建 API 31+ 的虚拟设备（需要支持 BLE）

### 3. 编译和运行
1. 点击工具栏的 `Run` 按钮（绿色三角形）
2. 选择目标设备
3. 等待编译和安装完成

### 4. 授予权限
首次运行后，需要手动授予权限：
1. 打开 `设置 → 应用 → StrokeNet → 权限`
2. 开启以下权限：
   - 蓝牙
   - 位置信息（精确位置）

## 方法二：命令行编译

### Windows

```powershell
# 编译 Debug 版本
.\gradlew.bat assembleDebug

# 编译 Release 版本
.\gradlew.bat assembleRelease

# 安装到设备
.\gradlew.bat installDebug

# 卸载
.\gradlew.bat uninstallDebug
```

### Linux / macOS / Termux

```bash
# 赋予执行权限
chmod +x gradlew

# 编译 Debug 版本
./gradlew assembleDebug

# 编译 Release 版本
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug

# 卸载
./gradlew uninstallDebug
```

### 输出位置
编译后的 APK 文件位于：
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

## 方法三：在 Termux 中编译

如果你想在 Android 设备上直接编译（无需电脑）：

### 1. 安装依赖
```bash
# 更新包列表
pkg update && pkg upgrade

# 安装 JDK
pkg install openjdk-17

# 安装 Git（如果需要克隆项目）
pkg install git

# 安装 Android SDK（可选，用于 adb）
pkg install android-tools
```

### 2. 编译项目
```bash
cd /path/to/StrokeNet

# 赋予执行权限
chmod +x gradlew

# 编译
./gradlew assembleDebug
```

### 3. 安装
```bash
# 使用 pm 命令安装
pm install app/build/outputs/apk/debug/app-debug.apk

# 或使用 adb（如果安装了）
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 签名 Release 版本

### 1. 生成密钥库
```bash
keytool -genkey -v -keystore strokenet.keystore \
  -alias strokenet \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### 2. 配置签名
在 `app/build.gradle.kts` 中添加：

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../strokenet.keystore")
            storePassword = "your_store_password"
            keyAlias = "strokenet"
            keyPassword = "your_key_password"
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### 3. 编译签名版本
```bash
./gradlew assembleRelease
```

输出：`app/build/outputs/apk/release/app-release.apk`

## 常见问题

### 1. Gradle 同步失败
```bash
# 清理构建缓存
./gradlew clean

# 重新下载依赖
./gradlew --refresh-dependencies
```

### 2. 编译错误：SDK 版本不匹配
确保安装了 Android SDK API 36：
- Android Studio: `Tools → SDK Manager → SDK Platforms`
- 命令行: `sdkmanager "platforms;android-36"`

### 3. 设备未检测到
```bash
# 检查设备连接
adb devices

# 重启 adb 服务
adb kill-server
adb start-server
```

### 4. 权限被拒绝
即使在代码中请求了权限，Android 12+ 仍需要手动授予：
```bash
# 通过 adb 授予权限
adb shell pm grant com.ec.strokenet android.permission.BLUETOOTH_ADVERTISE
adb shell pm grant com.ec.strokenet android.permission.BLUETOOTH_CONNECT
adb shell pm grant com.ec.strokenet android.permission.ACCESS_FINE_LOCATION
```

### 5. 蓝牙广播失败
- 确保蓝牙已开启
- 检查设备是否支持 BLE 广播（部分设备不支持）
- 查看 Logcat 日志：`adb logcat | grep BleAdvertiser`

## 测试部署

### 1. 安装 APP
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. 启动 APP
```bash
adb shell am start -n com.ec.strokenet/.MainActivity
```

### 3. 测试 Intent
```bash
# 使用测试脚本
./test_intents.sh  # Linux/macOS
test_intents.bat   # Windows

# 或手动发送
adb shell am start -n com.ec.strokenet/.MainActivity \
  --es action start \
  --ei depth 36 \
  --ei extend 8 \
  --ei retract 8
```

### 4. 查看日志
```bash
# 实时查看所有日志
adb logcat

# 只看 BLE 相关日志
adb logcat | grep BleAdvertiser

# 保存日志到文件
adb logcat > logcat.txt
```

## 性能优化

### 减小 APK 体积
在 `app/build.gradle.kts` 中启用：

```kotlin
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
}
```

### 启用 R8 优化
R8 默认启用，可以在 `proguard-rules.pro` 中添加规则：

```proguard
# 保留 BLE 相关类
-keep class android.bluetooth.** { *; }
-keep class com.ec.strokenet.BleAdvertiser { *; }
```

## 持续集成

### GitHub Actions 示例
创建 `.github/workflows/build.yml`：

```yaml
name: Build APK

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 11
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
    
    - name: Grant execute permission for gradlew
      run: chmod +x gradlew
    
    - name: Build with Gradle
      run: ./gradlew assembleDebug
    
    - name: Upload APK
      uses: actions/upload-artifact@v3
      with:
        name: app-debug
        path: app/build/outputs/apk/debug/app-debug.apk
```

## 下一步

1. **调整 UUID 编码**：根据实际抓包结果修改 `BleAdvertiser.kt`
2. **测试功能**：使用测试脚本验证各项功能
3. **部署 MCP 服务**：在 Termux 中运行 `mcp_server_example.py`
4. **集成 AI**：配置 AI 助手调用 MCP 工具

## 参考资源

- [Android Bluetooth LE 文档](https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview)
- [Jetpack Compose 文档](https://developer.android.com/jetpack/compose)
- [Gradle 构建指南](https://docs.gradle.org/current/userguide/userguide.html)
