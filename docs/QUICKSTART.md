# 快速开始指南

5 分钟快速上手 StrokeNet！

## 步骤 1：编译 APK

### 使用 Android Studio（推荐）
1. 打开 Android Studio
2. 选择 `File → Open`，打开项目目录
3. 等待 Gradle 同步完成
4. 点击 `Build → Build Bundle(s) / APK(s) → Build APK(s)`
5. 等待编译完成，APK 位于 `app/build/outputs/apk/debug/`

### 使用命令行
```bash
# Windows
.\gradlew.bat assembleDebug

# Linux/macOS/Termux
chmod +x gradlew
./gradlew assembleDebug
```

## 步骤 2：安装到手机

### 方法 A：通过 USB
```bash
# 连接手机，启用 USB 调试
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 方法 B：直接传输
1. 将 `app-debug.apk` 复制到手机
2. 在手机上打开文件管理器
3. 点击 APK 文件安装

## 步骤 3：授予权限

⚠️ **重要**：必须手动授予权限！

1. 打开 `设置 → 应用 → StrokeNet → 权限`
2. 开启以下权限：
   - ✅ 蓝牙
   - ✅ 位置信息（精确位置）

或使用命令行：
```bash
adb shell pm grant com.ec.strokenet android.permission.BLUETOOTH_ADVERTISE
adb shell pm grant com.ec.strokenet android.permission.BLUETOOTH_CONNECT
adb shell pm grant com.ec.strokenet android.permission.ACCESS_FINE_LOCATION
```

## 步骤 4：测试功能

### 测试 1：手动控制
1. 打开 StrokeNet APP
2. 确保蓝牙已开启
3. 调节滑块设置参数
4. 点击"启动"按钮
5. 观察设备是否响应

### 测试 2：Intent 控制
```bash
# 启动推拉
adb shell am start -n com.ec.strokenet/.MainActivity \
  --es action start \
  --ei depth 36 \
  --ei extend 8 \
  --ei retract 8

# 停止
adb shell am start -n com.ec.strokenet/.MainActivity \
  --es action stop
```

### 测试 3：使用测试脚本
```bash
# Windows
test_intents.bat

# Linux/macOS
chmod +x test_intents.sh
./test_intents.sh
```

## 步骤 5：调整 UUID 编码（重要！）

当前的 UUID 编码是示例，需要根据实际抓包结果调整。

### 5.1 抓取真实 UUID
1. 手机上安装 nRF Connect
2. 打开 nRF Connect，切换到 Scanner 页面
3. 打开官方 APP，连接设备
4. 在官方 APP 中操作一个功能（如调节深度）
5. 在 nRF Connect 中找到新出现的广播记录
6. 记录 128 位 Service UUID

### 5.2 分析编码规律
重复上述步骤，记录每个参数值对应的 UUID：

| 操作 | 参数 | UUID |
|------|------|------|
| 深度 0 | depth=0 | 710003f8-1f00-bbbf-????-???????????? |
| 深度 36 | depth=36 | 710003f8-1f00-bbbf-????-???????????? |
| 深度 72 | depth=72 | 710003f8-1f00-bbbf-????-???????????? |
| ... | ... | ... |

### 5.3 修改编码函数
编辑 `app/src/main/java/com/ec/strokenet/BleAdvertiser.kt`：

```kotlin
private fun buildControlUuid(...): UUID {
    // 根据分析结果修改这里的编码逻辑
    val param1 = when (action) {
        "start", "thrust" -> {
            // 你的编码逻辑
            ((depth and 0xFF) shl 8) or 
            ((extendSpeed and 0x0F) shl 4) or 
            (retractSpeed and 0x0F)
        }
        // ...
    }
    // ...
}
```

详细说明见 `UUID_ENCODING.md`

### 5.4 重新编译测试
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 步骤 6：部署 MCP 服务（可选）

如果要集成 AI 控制：

### 6.1 安装依赖（Termux）
```bash
pkg install python
pip install mcp
```

### 6.2 运行服务
```bash
python mcp_server_example.py
```

### 6.3 配置 AI
在 AI 配置中添加 MCP 服务器：
```json
{
  "mcpServers": {
    "strokenet": {
      "command": "python",
      "args": ["/path/to/mcp_server_example.py"],
      "env": {}
    }
  }
}
```

### 6.4 测试 AI 控制
对 AI 说：
- "启动推拉，深度 36"
- "设置强度 80"
- "停止"

## 常见问题

### Q1: APP 安装后无法打开
**A**: 检查 Android 版本是否 ≥ 12 (API 31)

### Q2: 点击启动没反应
**A**: 
1. 检查蓝牙是否开启
2. 检查权限是否已授予
3. 查看日志：`adb logcat | grep BleAdvertiser`

### Q3: 设备没有响应
**A**: 
1. UUID 编码可能不正确，需要根据抓包调整
2. 设备距离太远（BLE 范围通常 10 米内）
3. 设备未处于监听状态

### Q4: 权限请求失败
**A**: Android 12+ 必须手动在设置中授予权限，代码请求不够

### Q5: 编译失败
**A**: 
```bash
# 清理缓存
./gradlew clean

# 重新下载依赖
./gradlew --refresh-dependencies
```

## 调试技巧

### 查看实时日志
```bash
adb logcat | grep -E "BleAdvertiser|StrokeNet"
```

### 验证广播
1. 打开 nRF Connect Scanner
2. 运行 StrokeNet APP 发送指令
3. 在 Scanner 中查看是否出现对应的 UUID 广播

### 对比 UUID
```bash
# 官方 APP 的 UUID
710003f8-1f00-bbbf-2488-000000000001

# 你的 APP 的 UUID
710003f8-1f00-bbbf-????-????????????

# 对比差异，调整编码
```

## 下一步

✅ 完成基础功能测试  
✅ 调整 UUID 编码使其正确工作  
✅ 部署 MCP 服务实现 AI 控制  
⬜ 添加预设模式  
⬜ 实现定时控制  
⬜ 记录使用历史  

## 获取帮助

- 📖 详细文档：`README.md`
- 🔧 构建指南：`BUILD_GUIDE.md`
- 🔐 编码配置：`UUID_ENCODING.md`
- 📊 项目总览：`PROJECT_SUMMARY.md`

## 成功标志

当你看到以下情况，说明一切正常：

1. ✅ APP 成功安装并打开
2. ✅ 权限已授予（蓝牙 + 位置）
3. ✅ 点击启动后 Toast 提示"指令已发送"
4. ✅ nRF Connect 能抓到广播
5. ✅ 设备正确响应控制指令

恭喜！你已经成功部署 StrokeNet！🎉
