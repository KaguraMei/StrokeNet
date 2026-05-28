# StrokeNet 项目指南

> 面向 AI Agent 的完整项目文档

## 📋 项目概述

**StrokeNet** 是一个基于 BLE 广播协议的 Android 控制应用，采用构成主义/蓝图工程图纸设计风格。

- **技术栈**: Kotlin + Jetpack Compose + BLE Advertiser API
- **最低系统**: Android 12 (API 31)
- **设计风格**: 构成主义 + 工程蓝图（直角、粗边框、网格背景、高对比度）
- **核心功能**: 通过 BLE 广播控制设备，支持手动控制和 Intent 调用

## 🏗️ 项目架构

```
app/src/main/java/com/ec/strokenet/
├── MainActivity.kt                      # 主入口（权限、Intent处理）
├── BleAdvertiser.kt                     # BLE 广播核心逻辑
│
├── data/model/                          # 数据模型
│   ├── ControlParams.kt                 # 控制参数（depth, speed, strength, temp）
│   └── Preset.kt                        # 预设模式（轻柔/标准/强力）
│
└── ui/                                  # UI 层
    ├── navigation/Screen.kt             # 路由定义（Control/Presets/Settings）
    ├── screens/                         # 页面
    │   ├── ControlScreen.kt             # 主控制页面
    │   ├── PresetsScreen.kt             # 预设模式页面
    │   └── SettingsScreen.kt            # 设置页面
    ├── components/                      # 可复用组件
    │   ├── ControlSlider.kt             # 滑块组件
    │   ├── PresetCard.kt                # 预设卡片
    │   └── StatusIndicator.kt           # 状态指示器
    └── theme/
        ├── ConstructStyle.kt            # 构成主义设计系统
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

## 🎨 设计系统

### 配色方案
```kotlin
val ConstructBlue = Color(0xFF1332A6)      // 克莱因蓝/工程蓝
val ConstructYellow = Color(0xFFFFD600)    // 工业黄
val ConstructBlack = Color(0xFF0F0F0F)     // 极黑
val ConstructWhite = Color(0xFFF0F4F8)     // 蓝图纸白
val ConstructGray = Color(0xFF9E9E9E)      // 注释灰
val ConstructGridLine = Color(0xFFD0D7E2)  // 网格线
```

### 设计原则
- ✅ **直角矩形** - 所有容器使用 `RectangleShape`
- ✅ **粗边框** - 2dp 黑色/蓝色边框
- ✅ **网格背景** - Canvas 绘制几何网格、准星、圆环
- ✅ **技术字体** - Monospace（等宽）+ Serif（衬线）
- ✅ **高对比度** - 纯色色块，无渐变

### 核心组件
```kotlin
// 几何背景
ConstructBackground()

// 容器块（替代 Card）
ConstructBlock(label = "DATA") { content() }

// 滑块
ControlSlider(
    label = "DEPTH",
    value = depth,
    valueRange = 0f..72f,
    onValueChange = { depth = it },
    valueText = "${depth.toInt()}",
    description = "STROKE AMPLITUDE"
)

// 状态指示器
StatusIndicator(
    isRunning = isRunning,
    isBluetoothEnabled = isBluetoothEnabled
)
```

## 🔌 BLE 控制协议

### UUID 格式
```
710003f8-1f00-bbbf-XXXX-XXXXXXXXXXXX
                    │    │
                    │    └─ 操作类型
                    └────── 参数编码
```

### 控制参数
| 参数 | 范围 | 说明 |
|------|------|------|
| depth | 0-72 | 推拉深度 |
| extendSpeed | 0-15 | 伸出速度 |
| retractSpeed | 0-15 | 缩回速度 |
| strength | 0-100 | 入体端强度 |
| temp | 0-60 | 加热温度（℃）|

### 操作类型
- `start` - 启动推拉（需同时提供 depth/extend/retract）
- `thrust` - 调节推拉参数
- `strength` - 设置强度
- `temp` - 设置温度
- `stop` - 停止所有功能

### BleAdvertiser 核心方法
```kotlin
fun advertise(
    action: String,
    depth: Int = 0,
    extendSpeed: Int = 0,
    retractSpeed: Int = 0,
    strength: Int = 0,
    temp: Int = 0,
    onSuccess: () -> Unit = {},
    onFailure: (Int) -> Unit = {}
)
```

## 📱 Intent 调用接口

### 启动推拉
```bash
am start -n com.ec.strokenet/.MainActivity \
  --es action start \
  --ei depth 36 \
  --ei extend 8 \
  --ei retract 8
```

### 设置强度
```bash
am start -n com.ec.strokenet/.MainActivity \
  --es action strength \
  --ei value 80
```

### 设置温度
```bash
am start -n com.ec.strokenet/.MainActivity \
  --es action temp \
  --ei value 40
```

### 停止
```bash
am start -n com.ec.strokenet/.MainActivity \
  --es action stop
```

## 🔧 开发指南

### 编译和安装
```bash
# 编译
./gradlew assembleDebug

# 安装
adb install app/build/outputs/apk/debug/app-debug.apk

# 授予权限
adb shell pm grant com.ec.strokenet android.permission.BLUETOOTH_ADVERTISE
adb shell pm grant com.ec.strokenet android.permission.BLUETOOTH_CONNECT
adb shell pm grant com.ec.strokenet android.permission.ACCESS_FINE_LOCATION
```

### 查看日志
```bash
adb logcat | grep BleAdvertiser
```

### 测试脚本
```bash
# Linux/macOS
./test_intents.sh

# 或手动测试
adb shell am start -n com.ec.strokenet/.MainActivity --es action start --ei depth 36 --ei extend 8 --ei retract 8
```

## 📝 添加新功能

### 1. 添加新页面
```kotlin
// 1. 在 Screen.kt 添加路由
object NewPage : Screen(
    route = "newpage",
    title = "新页面",
    icon = Icons.Default.Add
)

// 2. 创建 NewPageScreen.kt
@Composable
fun NewPageScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        ConstructBackground()
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "NEW PAGE",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black,
                fontSize = 42.sp,
                color = ConstructBlue
            )
            // 内容
        }
    }
}

// 3. 在 MainActivity.kt 添加路由
when (currentScreen) {
    Screen.NewPage.route -> NewPageScreen()
}
```

### 2. 添加新组件
```kotlin
@Composable
fun NewComponent() {
    Box(
        modifier = Modifier
            .border(2.dp, ConstructBlue)
            .background(ConstructWhite)
            .padding(12.dp)
    ) {
        // 组件内容
    }
}
```

### 3. 添加新预设
```kotlin
// 在 Preset.kt 的 getBuiltInPresets() 中添加
Preset(
    id = "custom",
    name = "自定义模式",
    description = "描述",
    icon = "star",
    params = ControlParams(
        depth = 50,
        extendSpeed = 10,
        retractSpeed = 10,
        strength = 70,
        temp = 35
    )
)
```

## ⚠️ 重要注意事项

### UUID 编码调整
当前 `BleAdvertiser.kt` 中的 UUID 编码是**示例代码**，需要根据实际设备抓包结果调整：

1. 使用 nRF Connect Scanner 抓取官方 APP 的 UUID
2. 分析字节位变化规律
3. 修改 `buildControlUuid()` 函数中的编码逻辑
4. 重新编译测试

**位置**: `app/src/main/java/com/ec/strokenet/BleAdvertiser.kt` 第 30-70 行

### 权限要求
Android 12+ 必须**手动**在设置中授予权限：
- 设置 → 应用 → StrokeNet → 权限
- 开启：蓝牙、位置信息（精确位置）

### 推拉参数规则
推拉的三个参数（depth / extendSpeed / retractSpeed）**必须同时设置**，设备才会执行动作。

## 🔍 调试技巧

### 验证广播
1. 打开 nRF Connect Scanner
2. 运行 StrokeNet APP 发送指令
3. 在 Scanner 中查看是否出现对应的 UUID 广播
4. 对比官方 APP 的 UUID，调整编码

### 常见问题

**Q: 点击启动没反应？**
- 检查蓝牙是否开启
- 检查权限是否已授予
- 查看日志：`adb logcat | grep BleAdvertiser`

**Q: 设备没有响应？**
- UUID 编码可能不正确，需要根据抓包调整
- 设备距离太远（BLE 范围通常 10 米内）
- 设备未处于监听状态

**Q: 编译失败？**
```bash
./gradlew clean
./gradlew --refresh-dependencies
```

## 📚 代码规范

### 文件命名
- Activity: `MainActivity.kt`
- Screen: `ControlScreen.kt`
- Component: `ControlSlider.kt`
- Model: `ControlParams.kt`

### Composable 命名
- 大写开头：`ControlScreen()`
- 描述性名称：`StatusIndicator()`
- Preview 后缀：`ControlScreenPreview()`

### 设计系统规范
```kotlin
// 字体
fontFamily = FontFamily.Monospace  // 技术文字
fontFamily = FontFamily.Serif      // 标题

// 颜色
color = ConstructBlue    // 主色
color = ConstructYellow  // 强调
color = ConstructBlack   // 文字

// 形状
shape = RectangleShape   // 强制直角

// 边框
border(2.dp, ConstructBlue)  // 主边框
border(1.dp, ConstructBlue)  // 次边框
```

## 🚀 扩展方向

### 短期
- [ ] 添加 ViewModel 管理状态
- [ ] 实现参数持久化（DataStore）
- [ ] 添加自定义预设功能
- [ ] 完善错误处理

### 中期
- [ ] 添加历史记录功能
- [ ] 实现定时控制
- [ ] 添加参数序列播放
- [ ] 优化 UI 动画

### 长期
- [ ] 迁移到 Navigation Compose（如需复杂导航）
- [ ] 添加 Room 数据库
- [ ] 实现网络控制
- [ ] 添加数据分析

## 📦 依赖管理

### 当前依赖
```kotlin
// Compose
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material3:material3-adaptive-navigation-suite")

// Activity & Lifecycle
implementation("androidx.activity:activity-compose")
implementation("androidx.lifecycle:lifecycle-runtime-ktx")
```

### 可选依赖（按需添加）
```kotlin
// Navigation
implementation("androidx.navigation:navigation-compose:2.7.0")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.0.0")

// Room
implementation("androidx.room:room-runtime:2.6.0")
implementation("androidx.room:room-ktx:2.6.0")
```

## 🎯 快速参考

### 启动 APP
```bash
adb shell am start -n com.ec.strokenet/.MainActivity
```

### 发送测试指令
```bash
# 启动推拉
adb shell am start -n com.ec.strokenet/.MainActivity --es action start --ei depth 36 --ei extend 8 --ei retract 8

# 停止
adb shell am start -n com.ec.strokenet/.MainActivity --es action stop
```

### 查看实时日志
```bash
adb logcat | grep -E "BleAdvertiser|StrokeNet"
```

### 验证广播
使用 nRF Connect Scanner 抓取 APP 发出的广播，验证 UUID 是否正确。

---

**项目状态**: ✅ 核心功能完成，等待 UUID 编码调整  
**设计风格**: 构成主义 / 蓝图工程图纸  
**版本**: 1.0.0  
**最后更新**: 2026-05-28
