# StrokeNet - AI Agent 开发指南

> 📌 **本文档专为 AI Agent 设计**，整合了项目所有关键信息，便于快速理解和继续开发。

**最后更新**: 2026-05-28  
**当前版本**: 1.0.0  
**设计语言**: iOS Glassmorphism  
**状态**: ✅ UI 重构完成，等待真机测试和 UUID 调整

---

## 📋 目录

1. [项目概述](#项目概述)
2. [当前状态](#当前状态)
3. [技术架构](#技术架构)
4. [设计系统](#设计系统)
5. [核心功能](#核心功能)
6. [文件结构](#文件结构)
7. [开发指南](#开发指南)
8. [已知问题](#已知问题)
9. [下一步工作](#下一步工作)
10. [快速参考](#快速参考)

---

## 项目概述

### 🎯 项目目标

StrokeNet 是一个基于 **BLE 广播协议**的 Android 控制应用，用于通过蓝牙广播控制特定设备（无需配对连接）。

### ✨ 核心特性

- **BLE 广播控制** - 使用 `BluetoothLeAdvertiser` 发送 128 位 UUID 广播
- **5 种控制参数** - depth(0-72), extendSpeed(0-15), retractSpeed(0-15), strength(0-100), temp(0-60)
- **预设模式** - 内置温柔/标准/强劲三种预设
- **Intent 接口** - 支持外部调用，可与 MCP 服务集成
- **iOS 玻璃拟态 UI** - 半透明面板、大圆角、苹果美学

### 🎨 设计风格

**当前**: iOS Glassmorphism（玻璃拟态）
- 半透明白色玻璃面板（75% 透明度）
- 24dp 大圆角
- iOS 配色：#007AFF 蓝、#34C759 绿、#FF3B30 红
- 浅灰背景：#F2F2F7
- 悬浮 Dock 式导航

**历史**: Constructivism（构成主义）- 已废弃，文件保留

---

## 当前状态

### ✅ 已完成

1. **UI 重构** (2026-05-28)
   - 从构成主义转变为 iOS 玻璃拟态
   - 创建 `GlassComponents.kt` 核心组件库
   - 重写所有页面（Control/Presets/Settings）
   - 修复 NullPointerException 和 kotlinOptions 错误

2. **核心功能**
   - BLE 广播实现 (`BleAdvertiser.kt`)
   - 5 种控制参数
   - Intent 接口支持
   - 权限管理

3. **文档系统**
   - 完整的设计文档
   - 架构说明
   - 构建指南

### ⚠️ 待完成

1. **UUID 编码调整** - 当前是示例代码，需根据实际设备调整
2. **真机测试** - 需在 Android 12+ 设备上测试
3. **BLE 功能验证** - 验证广播是否被设备接收

### 🐛 已修复的问题

- ✅ `kotlinOptions` 编译错误（删除不必要的配置块）
- ✅ `NullPointerException` in Navigation（使用字符串常量代替 sealed class）
- ✅ `compileSdk` 语法错误（改为 `compileSdk = 36`）

---

## 技术架构

### 📦 技术栈

```
Android 12+ (API 31)
├── Kotlin 2.0
├── Jetpack Compose (最新 BOM)
├── Material 3
├── BluetoothLeAdvertiser (BLE 广播)
└── Gradle 8.x
```

### 🏗️ 架构模式

```
MVVM-like (简化版)
├── UI Layer (Compose)
│   ├── Screens (页面)
│   ├── Components (组件)
│   └── Theme (主题)
├── Data Layer
│   └── Models (数据模型)
└── BLE Layer
    └── BleAdvertiser (广播控制)
```

### 📁 项目结构

```
app/src/main/java/com/ec/strokenet/
├── MainActivity.kt              # 主入口，Scaffold + 导航
├── BleAdvertiser.kt            # BLE 广播核心 ⚠️ UUID 需调整
│
├── data/model/
│   ├── ControlParams.kt        # 参数范围定义
│   └── Preset.kt              # 预设模式数据
│
└── ui/
    ├── navigation/
    │   └── Screen.kt          # 导航定义（已废弃，直接用字符串）
    │
    ├── screens/
    │   ├── ControlScreen.kt   # 控制中心（滑块 + 按钮）
    │   ├── PresetsScreen.kt   # 预设模式列表
    │   └── SettingsScreen.kt  # 设置信息页
    │
    ├── components/
    │   ├── GlassComponents.kt # 玻璃拟态核心组件 ⭐
    │   └── StatusIndicator.kt # 状态指示器
    │
    └── theme/
        ├── Color.kt           # iOS 颜色系统
        ├── Theme.kt           # 主题定义
        └── Type.kt            # 字体定义
```

---

## 设计系统

### 🎨 iOS 颜色系统

```kotlin
// 定义在 ui/theme/Color.kt
val iOSBg = Color(0xFFF2F2F7)           // 背景
val iOSBlue = Color(0xFF007AFF)         // 主操作
val iOSGreen = Color(0xFF34C759)        // 成功/运行
val iOSRed = Color(0xFFFF3B30)          // 危险/停止
val iOSTextPrimary = Color(0xFF1C1C1E) // 主文本
val iOSTextSecondary = Color(0xFF8E8E93) // 次要文本
```

### 📦 核心组件

#### 1. GlassPanel - 玻璃面板

```kotlin
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
)
```

**用途**: 所有内容区域的容器  
**特性**: 75% 透明白色、24dp 圆角、极淡阴影

#### 2. GlassTopBar - 顶部导航栏

```kotlin
@Composable
fun GlassTopBar(title: String)
```

**用途**: 页面标题栏  
**特性**: 85% 透明白色、自动适配状态栏

#### 3. GlassBottomDock - 底部导航

```kotlin
@Composable
fun GlassBottomDock(
    currentRoute: String,
    onNavigate: (String) -> Unit
)
```

**用途**: 悬浮 Dock 式导航  
**特性**: 胶囊形状、图标+文字、选中高亮

#### 4. AppleStyleSlider - iOS 风格滑块

```kotlin
@Composable
fun AppleStyleSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String = "",
    onValueChange: (Float) -> Unit
)
```

**用途**: 参数调节  
**特性**: iOS 蓝激活轨道、白色滑块、标签+数值显示

### 📐 设计规范

```kotlin
// 圆角
RoundedCornerShape(24.dp)  // 大面板
RoundedCornerShape(16.dp)  // 按钮
RoundedCornerShape(8.dp)   // 小图标背景
CircleShape                // Dock 导航

// 间距
horizontal = 20.dp         // 页面边距
vertical = 16.dp
spacedBy(20.dp)           // 组件间距
padding(20.dp)            // 面板内边距

// 阴影
elevation = 16.dp         // 玻璃面板
spotColor = Color.Black.copy(alpha = 0.03f)

// 分割线
Divider(color = Color.Black.copy(alpha = 0.05f))
```

---

## 核心功能

### 🔵 BLE 广播控制

**文件**: `BleAdvertiser.kt`

#### 核心方法

```kotlin
fun advertise(
    action: String,              // "start", "thrust", "strength", "temp", "stop"
    depth: Int = 0,             // 0-72
    extendSpeed: Int = 0,       // 0-15
    retractSpeed: Int = 0,      // 0-15
    strength: Int = 0,          // 0-100
    temp: Int = 0,              // 0-60
    onSuccess: () -> Unit = {},
    onFailure: (Int) -> Unit = {}
)
```

#### UUID 编码 ⚠️

**当前状态**: 示例代码，需要调整

```kotlin
private fun buildControlUuid(
    action: String,
    depth: Int = 0,
    extendSpeed: Int = 0,
    retractSpeed: Int = 0,
    strength: Int = 0,
    temp: Int = 0
): UUID {
    // ⚠️ 这里的编码逻辑是示例，需要根据实际设备调整
    val param1 = when (action) {
        "start", "thrust" -> {
            ((depth and 0xFF) shl 8) or 
            ((extendSpeed and 0x0F) shl 4) or 
            (retractSpeed and 0x0F)
        }
        "strength" -> 0x1000 or (strength and 0xFF)
        "temp" -> 0x2000 or (temp and 0xFF)
        "stop" -> 0x0000
        else -> 0x0000
    }
    
    val param2 = when (action) {
        "start" -> 0x000000000001L
        "thrust" -> 0x000000000002L
        "strength" -> 0x000000000003L
        "temp" -> 0x000000000004L
        "stop" -> 0x000000000005L
        else -> 0x000000000000L
    }
    
    val uuidString = String.format(UUID_TEMPLATE, param1, param2)
    return UUID.fromString(uuidString)
}
```

**调整方法**:
1. 使用 nRF Connect Scanner 抓取官方 APP 的 UUID
2. 对比不同参数值的 UUID 变化
3. 找出字节位编码规律
4. 修改 `buildControlUuid()` 函数

### 🎚️ 控制参数

```kotlin
// 定义在 data/model/ControlParams.kt
object ControlParams {
    const val DEPTH_MIN = 0
    const val DEPTH_MAX = 72
    
    const val SPEED_MIN = 0
    const val SPEED_MAX = 15
    
    const val STRENGTH_MIN = 0
    const val STRENGTH_MAX = 100
    
    const val TEMP_MIN = 0
    const val TEMP_MAX = 60
}
```

**重要规则**: 推拉的三个参数（depth/extendSpeed/retractSpeed）必须同时设置，设备才会响应。

### 📋 预设模式

```kotlin
// 定义在 data/model/Preset.kt
data class Preset(
    val id: String,
    val name: String,
    val description: String,
    val params: ControlParams
)

// 内置预设
fun getBuiltInPresets(): List<Preset> = listOf(
    Preset("gentle", "温柔模式", "轻柔舒适", ...),
    Preset("standard", "标准模式", "适中体验", ...),
    Preset("intense", "强劲模式", "强力刺激", ...)
)
```

### 🔌 Intent 接口

```bash
# 启动推拉
am start -n com.ec.strokenet/.MainActivity \
  --es action start \
  --ei depth 36 \
  --ei extend 8 \
  --ei retract 8

# 调节参数（运行中）
am start -n com.ec.strokenet/.MainActivity \
  --es action thrust \
  --ei depth 50 \
  --ei extend 10 \
  --ei retract 10

# 设置强度
am start -n com.ec.strokenet/.MainActivity \
  --es action strength \
  --ei value 80

# 设置温度
am start -n com.ec.strokenet/.MainActivity \
  --es action temp \
  --ei value 40

# 停止
am start -n com.ec.strokenet/.MainActivity \
  --es action stop
```

**处理逻辑**: 在 `MainActivity.handleIntent()` 中

---

## 文件结构

### 🔑 关键文件说明

| 文件 | 用途 | 状态 | 注意事项 |
|------|------|------|---------|
| `MainActivity.kt` | 主入口、导航 | ✅ 完成 | 使用 Scaffold + 自定义导航 |
| `BleAdvertiser.kt` | BLE 广播 | ⚠️ 需调整 | UUID 编码是示例代码 |
| `GlassComponents.kt` | UI 组件库 | ✅ 完成 | 玻璃拟态核心 |
| `ControlScreen.kt` | 控制页面 | ✅ 完成 | 滑块 + 按钮 |
| `PresetsScreen.kt` | 预设页面 | ✅ 完成 | 列表 + 点击应用 |
| `SettingsScreen.kt` | 设置页面 | ✅ 完成 | 信息展示 |
| `Color.kt` | 颜色系统 | ✅ 完成 | iOS 配色 |
| `ControlParams.kt` | 参数定义 | ✅ 完成 | 范围常量 |
| `Preset.kt` | 预设数据 | ✅ 完成 | 内置 3 个预设 |

### 🗑️ 废弃文件（可删除）

```
ui/components/ControlSlider.kt      # 旧的构成主义滑块
ui/components/PresetCard.kt         # 旧的构成主义卡片
ui/theme/ConstructStyle.kt          # 构成主义样式
DESIGN_CONSTRUCTIVISM.md            # 构成主义设计文档
```

### 📚 文档文件

| 文件 | 用途 | 推荐阅读 |
|------|------|---------|
| `AI_AGENT_GUIDE.md` | **本文档** | ⭐⭐⭐⭐⭐ |
| `README.md` | 项目介绍 | ⭐⭐⭐⭐ |
| `DESIGN_IOS_GLASSMORPHISM.md` | 设计系统详解 | ⭐⭐⭐⭐ |
| `ARCHITECTURE.md` | 架构说明 | ⭐⭐⭐ |
| `BUILD_GUIDE.md` | 构建部署 | ⭐⭐⭐ |
| `UUID_ENCODING.md` | UUID 编码说明 | ⭐⭐⭐⭐⭐ |
| `UI_REDESIGN_SUMMARY.md` | UI 重构总结 | ⭐⭐ |
| `CRASH_FIX.md` | 崩溃修复记录 | ⭐⭐ |

---

## 开发指南

### 🚀 快速开始

#### 1. 环境准备

```bash
# 要求
- Android Studio (最新版)
- JDK 11+
- Android SDK (API 31+)
- 支持 BLE 的 Android 12+ 设备
```

#### 2. 编译安装

```bash
# 编译
./gradlew clean assembleDebug

# 安装
adb install app/build/outputs/apk/debug/app-debug.apk

# 授予权限
adb shell pm grant com.ec.strokenet android.permission.BLUETOOTH_ADVERTISE
adb shell pm grant com.ec.strokenet android.permission.BLUETOOTH_CONNECT
adb shell pm grant com.ec.strokenet android.permission.ACCESS_FINE_LOCATION
```

#### 3. 测试

```bash
# 测试启动
adb shell am start -n com.ec.strokenet/.MainActivity \
  --es action start --ei depth 36 --ei extend 8 --ei retract 8

# 查看日志
adb logcat | grep BleAdvertiser
```

### 🔧 常见开发任务

#### 添加新的控制参数

1. 在 `ControlParams.kt` 添加范围常量
2. 在 `BleAdvertiser.buildControlUuid()` 添加编码逻辑
3. 在 `ControlScreen.kt` 添加滑块
4. 在 `MainActivity.handleIntent()` 添加 Intent 处理

#### 添加新的预设模式

1. 在 `Preset.kt` 的 `getBuiltInPresets()` 添加新预设
2. 预设会自动显示在预设页面

#### 修改 UI 样式

1. 颜色：修改 `ui/theme/Color.kt`
2. 组件：修改 `ui/components/GlassComponents.kt`
3. 页面：修改对应的 Screen 文件

#### 调整 UUID 编码

1. 使用 nRF Connect 抓取官方 APP 的 UUID
2. 分析字节位变化规律
3. 修改 `BleAdvertiser.buildControlUuid()`
4. 重新编译测试

### 🐛 调试技巧

```bash
# 查看 BLE 日志
adb logcat | grep BleAdvertiser

# 查看崩溃日志
adb logcat *:E

# 查看应用日志
adb logcat | grep strokenet

# 清除应用数据
adb shell pm clear com.ec.strokenet

# 重启应用
adb shell am force-stop com.ec.strokenet
adb shell am start -n com.ec.strokenet/.MainActivity
```

### ⚠️ 权限问题

Android 12+ 必须手动授予权限：

1. 打开设置 → 应用 → StrokeNet → 权限
2. 开启：蓝牙、位置信息（精确位置）
3. 或使用 adb 命令授予（见上方）

---

## 已知问题

### ⚠️ UUID 编码未验证

**问题**: `BleAdvertiser.buildControlUuid()` 中的编码逻辑是示例代码  
**影响**: 设备可能无法识别广播  
**解决**: 需要根据实际设备调整编码逻辑

**详细步骤**:
1. 手机上同时打开 nRF Connect Scanner 和官方 APP
2. 在官方 APP 中操作功能（如调节深度）
3. 在 nRF Connect 中查看新出现的广播 UUID
4. 记录不同参数对应的 UUID
5. 分析字节位变化规律
6. 修改 `buildControlUuid()` 函数

参考文档: `UUID_ENCODING.md`

### ✅ 已修复的问题

1. **NullPointerException in Navigation**
   - 原因: Sealed class 序列化问题
   - 解决: 使用字符串常量 "control", "presets", "settings"

2. **kotlinOptions 编译错误**
   - 原因: kotlin.compose 插件已自动处理
   - 解决: 删除 kotlinOptions 块

3. **compileSdk 语法错误**
   - 原因: 错误的语法 `compileSdk { version = release(36) }`
   - 解决: 改为 `compileSdk = 36`

---

## 下一步工作

### 🎯 优先级 1 - 核心功能验证

- [ ] **真机测试** - 在 Android 12+ 设备上安装测试
- [ ] **权限授予** - 手动授予蓝牙和位置权限
- [ ] **BLE 广播测试** - 使用 nRF Connect 验证广播是否发出
- [ ] **UUID 编码调整** - 根据实际设备调整编码逻辑
- [ ] **设备响应测试** - 验证设备是否接收并执行指令

### 🎯 优先级 2 - 功能增强

- [ ] **错误处理** - 添加更详细的错误提示
- [ ] **日志系统** - 添加操作日志记录
- [ ] **自定义预设** - 允许用户创建和保存预设
- [ ] **参数验证** - 添加参数范围验证
- [ ] **广播状态监控** - 显示广播发送状态

### 🎯 优先级 3 - UI 优化

- [ ] **动画效果** - 添加页面切换动画
- [ ] **触觉反馈** - 添加按钮点击震动
- [ ] **深色模式** - 支持系统深色模式
- [ ] **横屏适配** - 优化横屏布局
- [ ] **平板适配** - 优化大屏幕布局

### 🎯 优先级 4 - 高级功能

- [ ] **MCP 服务** - 完善 MCP 服务集成
- [ ] **定时任务** - 支持定时执行指令
- [ ] **场景模式** - 支持多步骤场景
- [ ] **数据统计** - 记录使用统计
- [ ] **远程控制** - 支持网络远程控制

---

## 快速参考

### 📝 常用命令

```bash
# 编译
./gradlew assembleDebug

# 安装
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 启动
adb shell am start -n com.ec.strokenet/.MainActivity

# 测试控制
adb shell am start -n com.ec.strokenet/.MainActivity \
  --es action start --ei depth 36 --ei extend 8 --ei retract 8

# 查看日志
adb logcat | grep BleAdvertiser

# 清除数据
adb shell pm clear com.ec.strokenet
```

### 🎨 设计规范速查

```kotlin
// 颜色
iOSBg           // #F2F2F7 背景
iOSBlue         // #007AFF 主操作
iOSGreen        // #34C759 成功
iOSRed          // #FF3B30 危险
iOSTextPrimary  // #1C1C1E 主文本
iOSTextSecondary // #8E8E93 次要文本

// 圆角
24.dp  // 大面板
16.dp  // 按钮
8.dp   // 小图标

// 间距
20.dp  // 页面边距
16.dp  // 组件间距
```

### 🔧 关键代码位置

```
BLE 广播逻辑:     BleAdvertiser.kt:buildControlUuid()
Intent 处理:      MainActivity.kt:handleIntent()
控制页面:         ui/screens/ControlScreen.kt
玻璃组件:         ui/components/GlassComponents.kt
颜色定义:         ui/theme/Color.kt
参数范围:         data/model/ControlParams.kt
预设数据:         data/model/Preset.kt
```

### 📞 问题排查

| 问题 | 可能原因 | 解决方法 |
|------|---------|---------|
| 应用崩溃 | 权限未授予 | 手动授予蓝牙和位置权限 |
| 广播无效 | UUID 编码错误 | 调整 buildControlUuid() |
| 设备无响应 | 参数不完整 | 确保 depth/extend/retract 同时设置 |
| 编译失败 | Gradle 配置 | 检查 build.gradle.kts |
| UI 显示异常 | 组件导入错误 | 检查 import 语句 |

---

## 📚 相关文档

### 核心文档
- `README.md` - 项目介绍和快速开始
- `DESIGN_IOS_GLASSMORPHISM.md` - 设计系统详解
- `UUID_ENCODING.md` - UUID 编码说明

### 参考文档
- `ARCHITECTURE.md` - 架构设计
- `BUILD_GUIDE.md` - 构建部署
- `UI_REDESIGN_SUMMARY.md` - UI 重构记录
- `CRASH_FIX.md` - 问题修复记录

### 外部资源
- [Android BLE 文档](https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview)
- [Jetpack Compose 文档](https://developer.android.com/jetpack/compose)
- [Material 3 设计](https://m3.material.io/)
- [nRF Connect 工具](https://www.nordicsemi.com/Products/Development-tools/nrf-connect-for-mobile)

---

## 🤝 AI Agent 协作建议

### 理解项目

1. **先读本文档** - 获取全局视图
2. **查看代码结构** - 理解文件组织
3. **运行测试** - 验证当前状态

### 开发新功能

1. **明确需求** - 确认要实现什么
2. **查找相关代码** - 找到需要修改的文件
3. **遵循设计规范** - 使用 iOS 玻璃拟态风格
4. **测试验证** - 确保功能正常

### 修复问题

1. **重现问题** - 理解问题现象
2. **查看日志** - 使用 adb logcat
3. **定位代码** - 找到问题根源
4. **测试修复** - 验证问题解决

### 代码风格

- 使用 Kotlin 惯用语法
- 遵循 Compose 最佳实践
- 保持代码简洁清晰
- 添加必要的注释

---

**文档维护**: 请在重大更改后更新本文档  
**反馈**: 如有问题或建议，请更新相关章节

---

*本文档由 AI Agent 生成和维护，最后更新于 2026-05-28*
