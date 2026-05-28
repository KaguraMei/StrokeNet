# 调试指南

## 🐛 APP 启动后立即崩溃

### 查看崩溃日志

在 Android Studio 的 **Logcat** 窗口中查看错误信息：

1. 打开 **View → Tool Windows → Logcat**
2. 选择你的设备/模拟器
3. 在过滤器中输入：`package:com.ec.strokenet`
4. 查找红色的错误信息（FATAL EXCEPTION）

### 常见问题和解决方案

#### 1. 模拟器不支持 BLE
**症状**: 启动后立即崩溃，日志显示 `BluetoothAdapter is null`

**解决方案**:
- 使用真机测试（推荐）
- 或者在模拟器设置中启用蓝牙支持
- 代码已添加异常处理，即使蓝牙不可用也应该能显示 UI

#### 2. 权限问题
**症状**: 启动后崩溃，日志显示 `SecurityException`

**解决方案**:
```bash
# 手动授予权限
adb shell pm grant com.ec.strokenet android.permission.BLUETOOTH_ADVERTISE
adb shell pm grant com.ec.strokenet android.permission.BLUETOOTH_CONNECT
adb shell pm grant com.ec.strokenet android.permission.ACCESS_FINE_LOCATION
```

#### 3. Compose 相关错误
**症状**: 日志显示 `IllegalStateException` 或 `ClassNotFoundException`

**解决方案**:
```bash
# 清理并重新构建
./gradlew clean
./gradlew assembleDebug
```

或在 Android Studio 中：
- `Build → Clean Project`
- `Build → Rebuild Project`

#### 4. 依赖问题
**症状**: 日志显示 `NoClassDefFoundError`

**解决方案**:
```bash
# 刷新依赖
./gradlew --refresh-dependencies
```

### 查看详细日志

#### 方法 1: Android Studio Logcat
1. 打开 Logcat 窗口
2. 选择 "No Filters"
3. 搜索 "FATAL" 或 "Exception"

#### 方法 2: 命令行
```bash
# 查看所有日志
adb logcat

# 只看错误
adb logcat *:E

# 只看我们的 APP
adb logcat | grep strokenet

# 保存日志到文件
adb logcat > logcat.txt
```

### 调试步骤

1. **清理项目**
   ```bash
   ./gradlew clean
   ```

2. **重新构建**
   ```bash
   ./gradlew assembleDebug
   ```

3. **卸载旧版本**
   ```bash
   adb uninstall com.ec.strokenet
   ```

4. **安装新版本**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

5. **启动并查看日志**
   ```bash
   adb shell am start -n com.ec.strokenet/.MainActivity
   adb logcat | grep -E "AndroidRuntime|strokenet"
   ```

### 创建最小测试版本

如果问题持续，可以创建一个最小版本测试：

```kotlin
// 在 MainActivity.kt 的 onCreate 中
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    Log.d("MainActivity", "onCreate started")
    
    try {
        enableEdgeToEdge()
        Log.d("MainActivity", "enableEdgeToEdge success")
        
        setContent {
            StrokeNetTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Text(
                        text = "Hello StrokeNet!",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        Log.d("MainActivity", "setContent success")
    } catch (e: Exception) {
        Log.e("MainActivity", "Error in onCreate", e)
        e.printStackTrace()
    }
}
```

### 检查 build.gradle.kts

确保依赖版本正确：

```kotlin
dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite")
}
```

### 模拟器配置

如果使用模拟器，确保：
1. API Level ≥ 31 (Android 12)
2. 系统镜像包含 Google APIs
3. 在 AVD 设置中启用蓝牙（如果可用）

### 真机测试

推荐使用真机测试，因为：
- 真机有真实的蓝牙硬件
- 性能更好
- 更接近实际使用场景

### 获取帮助

如果以上方法都无法解决，请提供：
1. 完整的 Logcat 日志（特别是 FATAL EXCEPTION 部分）
2. Android 版本和设备型号
3. 是否使用模拟器还是真机

---

**提示**: 大多数启动崩溃问题都可以通过查看 Logcat 日志快速定位。
