# StrokeNet - AI Agent 开发指南

> 📌 **本文档专为 AI Agent 设计**，整合了项目所有关键信息，便于快速理解和继续开发。

**最后更新**: 2026-05-29  
**当前版本**: 1.0.0  
**状态**: ✅ 核心功能完成，前台服务已集成

---

## 📋 目录

1. [项目概述](#项目概述)
2. [当前状态](#当前状态)
3. [技术架构](#技术架构)
4. [核心功能](#核心功能)
5. [文件结构](#文件结构)
6. [开发指南](#开发指南)
7. [下一步工作](#下一步工作)
8. [快速参考](#快速参考)

---

## 项目概述

### 🎯 项目目标

StrokeNet 是一个基于 **BLE 广播协议**的 Android 控制应用，用于通过蓝牙广播控制特定设备（无需配对连接）。

### ✨ 核心特性

- **BLE 广播控制** - 使用 `BluetoothLeAdvertiser` 发送 128 位 UUID 广播
- **前台服务** - 后台可靠发送，失败自动重试（最多2次）
- **5 种控制参数** - depth(0-72), extendSpeed(0-15), retractSpeed(0-15), strength(0-100), temp(0-60)
- **预设模式** - 内置温柔/标准/强劲三种预设
- **Intent 接口** - 支持外部调用，可与 MCP 服务集成
- **通知反馈** - 实时显示发送状态和结果

---

## 当前状态

### ✅ 已完成

1. **前台服务集成** (2026-05-29)
   - 创建 `BleService.kt` 前台服务
   - 实现重试逻辑（失败最多重试2次）
   - 通知栏状态显示
   - 所有 UI 操作统一通过 Service

2. **核心功能**
   - BLE 广播实现 (`BleAdvertiser.kt`)
   - 5 种控制参数
   - Intent 接口支持
   - 权限管理

3. **文档系统**
   - Service 集成文档
   - 架构说明
   - 构建指南

### ⚠️ 待完成

1. **UUID 编码调整** - 当前是示例代码，需根据实际设备调整
2. **真机测试** - 需在 Android 12+ 设备上测试
3. **BLE 功能验证** - 验证广播是否被设备接收

---

## 技术架构

### � 技术栈

```
Android 12+ (API 31)
├── Kotlin 2.0
├── Jetpack Compose (最新 BOM)
├── Material 3
├── BluetoothLeAdvertiser (BLE 广播)
├── Foreground Service (前台服务)
└── Gradle 8.x
```

### 🏗️ 三层架构

```
┌─────────────────────────────────────────┐
│  UI 层 (ControlScreen, PresetsScreen)   │
│  - 用户交互、显示界面、启动 Service      │
└──────────────┬──────────────────────────┘
               │ Intent
               ↓
┌─────────────────────────────────────────┐
│  BleService (前台服务)                   │
│  - 生命周期管理、重试逻辑、通知栏显示    │
└──────────────┬──────────────────────────┘
               │ 调用
               ↓
┌─────────────────────────────────────────┐
│  BleAdvertiser (蓝牙广播工具类)          │
│  - BLE 广播功能、UUID 编码、权限检查     │
└─────────────────────────────────────────┘
```

### 📁 项目结构

```
app/src/main/java/aya/strokenet/
├── MainActivity.kt              # 主入口，处理 Intent 调用
├── BleService.kt               # 前台服务，重试逻辑 ⭐ 新增
├── BleAdvertiser.kt            # BLE 广播工具类 ⚠️ UUID 需调整
│
├── data/model/
│   ├── ControlParams.kt        # 参数范围定义
│   └── Preset.kt              # 预设模式数据
│
└── ui/
    ├── screens/
    │   ├── ControlScreen.kt   # 控制中心（滑块 + 按钮）
    │   ├── PresetsScreen.kt   # 预设模式列表
    │   └── SettingsScreen.kt  # 设置信息页
    │
    ├── components/
    │   ├── GlassComponents.kt # UI 核心组件
    │   └── StatusIndicator.kt # 状态指示器
    │
    └── theme/
        ├── Color.kt           # 颜色系统
        ├── Theme.kt           # 主题定义
        └── Type.kt            # 字体定义
```

---

## 核心功能

### 🔵 前台服务 (BleService)

**文件**: `BleService.kt`

**职责**:
- 前台服务（通知栏显示，系统不杀）
- 重试逻辑（失败时最多重试 2 次，间隔 300ms）
- 通知更新（显示发送状态）
- 自动停止（成功后 2 秒，失败后 3 秒）

**工作流程**:
```
Intent 调用 → MainActivity → 启动 BleService
    ↓
BleService 显示通知："发送中..."
    ↓
调用 BleAdvertiser.advertise()
    ↓
成功？→ 通知"✓ 已发送"，2秒后停止
失败？→ 重试（最多2次）→ 通知"✗ 发送失败"，3秒后停止
```

**通知栏状态示例**:
```
发送中: 推拉: 深度=50 伸=10 缩=10
重试中 (1/2): 推拉: 深度=50 伸=10 缩=10
✓ 已发送: 推拉: 深度=50 伸=10 缩=10
✗ 发送失败: 推拉: 深度=50 (错误码: -4)
```

详见: `docs/SERVICE_INTEGRATION.md`

### � BLE 广播控制 (BleAdvertiser)

**文件**: `BleAdvertiser.kt`

**核心方法**:
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

**UUID 编码** ⚠️:

当前状态: 示例代码，需要调整

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

详见: `docs/UUID_ENCODING.md`

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

### 🔌 Intent 接口

```bash
# 启动推拉
am start -n aya.strokenet/.MainActivity \
  --es action start \
  --ei depth 36 \
  --ei extend 8 \
  --ei retract 8

# 运行中调节参数
am start -n aya.strokenet/.MainActivity \
  --es action thrust \
  --ei depth 50 \
  --ei extend 10 \
  --ei retract 10

# 设置强度
am start -n aya.strokenet/.MainActivity \
  --es action strength \
  --ei value 80

# 设置温度
am start -n aya.strokenet/.MainActivity \
  --es action temp \
  --ei value 40

# 停止
am start -n aya.strokenet/.MainActivity \
  --es action stop
```

**处理逻辑**: 
1. `MainActivity.handleIntent()` 接收 Intent
2. 启动 `BleService` 处理指令
3. Service 调用 `BleAdvertiser` 发送广播
4. 通知栏显示结果

### 🤝 MCP 服务集成

```python
# daxiu_mcp.py
from mcp.server.fastmcp import FastMCP
import subprocess

mcp = FastMCP("daxiu")

def am(action, **kwargs):
    cmd = ["am", "start", "-n", "aya.strokenet/.MainActivity",
           "--es", "action", action]
    for k, v in kwargs.items():
        cmd += ["--ei", k, str(v)]
    subprocess.run(cmd, capture_output=True, text=True, timeout=5)

@mcp.tool()
def daxiu_start(depth: int = 36, extend_speed: int = 8, retract_speed: int = 8) -> str:
    """启动推拉。depth 0-72, extend_speed 0-15, retract_speed 0-15"""
    am("start", depth=depth, extend=extend_speed, retract=retract_speed)
    return f"推拉启动: 深浅={depth}/72 伸出={extend_speed}/15 缩回={retract_speed}/15"

@mcp.tool()
def daxiu_stop() -> str:
    """全部停止"""
    am("stop")
    return "已停止"

if __name__ == "__main__":
    mcp.run(transport="sse", host="0.0.0.0", port=3459)
```

---

## 文件结构

### � 关键文件说明

| 文件 | 用途 | 状态 | 注意事项 |
|------|------|------|---------|
| `MainActivity.kt` | 主入口、Intent 处理 | ✅ 完成 | 启动 Service |
| `BleService.kt` | 前台服务、重试逻辑 | ✅ 完成 | 新增文件 |
| `BleAdvertiser.kt` | BLE 广播工具类 | ⚠️ 需调整 | UUID 编码是示例 |
| `ControlScreen.kt` | 控制页面 | ✅ 完成 | 通过 Service 发送 |
| `PresetsScreen.kt` | 预设页面 | ✅ 完成 | 通过 Service 发送 |
| `SettingsScreen.kt` | 设置页面 | ✅ 完成 | 信息展示 |
| `ControlParams.kt` | 参数定义 | ✅ 完成 | 范围常量 |
| `Preset.kt` | 预设数据 | ✅ 完成 | 内置 3 个预设 |

### � 文档文件

| 文件 | 用途 | 推荐阅读 |
|------|------|---------|
| `AI_AGENT_GUIDE.md` | **本文档** | ⭐⭐⭐⭐⭐ |
| `README.md` | 项目介绍 | ⭐⭐⭐⭐⭐ |
| `SERVICE_INTEGRATION.md` | Service 集成说明 | ⭐⭐⭐⭐⭐ |
| `UUID_ENCODING.md` | UUID 编码说明 | ⭐⭐⭐⭐⭐ |
| `ARCHITECTURE.md` | 架构说明 | ⭐⭐⭐⭐ |
| `BUILD_GUIDE.md` | 构建部署 | ⭐⭐⭐ |
| `PROJECT_GUIDE.md` | 完整项目指南 | ⭐⭐⭐ |

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
adb shell pm grant aya.strokenet android.permission.BLUETOOTH_ADVERTISE
adb shell pm grant aya.strokenet android.permission.BLUETOOTH_CONNECT
adb shell pm grant aya.strokenet android.permission.ACCESS_FINE_LOCATION
```

#### 3. 测试

```bash
# 测试启动
adb shell am start -n aya.strokenet/.MainActivity \
  --es action start --ei depth 36 --ei extend 8 --ei retract 8

# 查看日志
adb logcat | grep -E "BleAdvertiser|BleService"

# 查看 Service 状态
adb shell dumpsys activity services aya.strokenet
```

### 🔧 常见开发任务

#### 添加新的控制参数

1. 在 `ControlParams.kt` 添加范围常量
2. 在 `BleAdvertiser.buildControlUuid()` 添加编码逻辑
3. 在 `ControlScreen.kt` 添加滑块
4. 在 `MainActivity.handleIntent()` 添加 Intent 处理
5. 在 `BleService.kt` 添加 action 处理

#### 调整 UUID 编码

1. 使用 nRF Connect 抓取官方 APP 的 UUID
2. 分析字节位变化规律
3. 修改 `BleAdvertiser.buildControlUuid()`
4. 重新编译测试

#### 修改重试逻辑

在 `BleService.kt` 中调整：
```kotlin
companion object {
    private const val MAX_RETRY = 2           // 最多重试次数
    private const val RETRY_DELAY_MS = 300L   // 重试间隔
}
```

### 🐛 调试技巧

```bash
# 查看 BLE 和 Service 日志
adb logcat | grep -E "BleAdvertiser|BleService"

# 查看崩溃日志
adb logcat *:E

# 查看 Service 状态
adb shell dumpsys activity services aya.strokenet

# 查看通知
adb shell dumpsys notification

# 清除应用数据
adb shell pm clear aya.strokenet

# 重启应用
adb shell am force-stop aya.strokenet
adb shell am start -n aya.strokenet/.MainActivity
```

### ⚠️ 权限问题

Android 12+ 必须手动授予权限：

1. 打开设置 → 应用 → StrokeNet → 权限
2. 开启：蓝牙、位置信息（精确位置）
3. 或使用 adb 命令授予（见上方）

---

## 下一步工作

### 🎯 优先级 1 - 核心功能验证

- [ ] **真机测试** - 在 Android 12+ 设备上安装测试
- [ ] **权限授予** - 手动授予蓝牙和位置权限
- [ ] **BLE 广播测试** - 使用 nRF Connect 验证广播是否发出
- [ ] **UUID 编码调整** - 根据实际设备调整编码逻辑
- [ ] **设备响应测试** - 验证设备是否接收并执行指令
- [ ] **Service 测试** - 验证后台运行和重试机制

### 🎯 优先级 2 - 功能增强

- [ ] **自定义动作循环** - 实现动作序列自动变化（见 SERVICE_INTEGRATION.md）
- [ ] **错误处理** - 添加更详细的错误提示
- [ ] **日志系统** - 添加操作日志记录
- [ ] **自定义预设** - 允许用户创建和保存预设
- [ ] **参数验证** - 添加参数范围验证

### 🎯 优先级 3 - 高级功能

- [ ] **MCP 服务完善** - 完善 MCP 服务集成
- [ ] **定时任务** - 支持定时执行指令
- [ ] **场景模式** - 支持多步骤场景
- [ ] **数据统计** - 记录使用统计

---

## 快速参考

### � 常用命令

```bash
# 编译
./gradlew assembleDebug

# 安装
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 启动
adb shell am start -n aya.strokenet/.MainActivity

# 测试控制
adb shell am start -n aya.strokenet/.MainActivity \
  --es action start --ei depth 36 --ei extend 8 --ei retract 8

# 查看日志
adb logcat | grep -E "BleAdvertiser|BleService"

# 查看 Service
adb shell dumpsys activity services aya.strokenet

# 清除数据
adb shell pm clear aya.strokenet
```

### 🔧 关键代码位置

```
前台服务:         BleService.kt
BLE 广播逻辑:     BleAdvertiser.kt:buildControlUuid()
Intent 处理:      MainActivity.kt:handleIntent()
控制页面:         ui/screens/ControlScreen.kt
预设页面:         ui/screens/PresetsScreen.kt
参数范围:         data/model/ControlParams.kt
预设数据:         data/model/Preset.kt
```

### 📞 问题排查

| 问题 | 可能原因 | 解决方法 |
|------|---------|---------|
| 应用崩溃 | 权限未授予 | 手动授予蓝牙和位置权限 |
| 广播无效 | UUID 编码错误 | 调整 buildControlUuid() |
| 设备无响应 | 参数不完整 | 确保 depth/extend/retract 同时设置 |
| Service 未启动 | 前台服务权限 | 检查 AndroidManifest.xml |
| 通知不显示 | 通知渠道未创建 | 检查 createNotificationChannel() |

---

## 📚 相关文档

### 核心文档
- `README.md` - 项目介绍和快速开始
- `SERVICE_INTEGRATION.md` - Service 集成详解
- `UUID_ENCODING.md` - UUID 编码说明

### 参考文档
- `ARCHITECTURE.md` - 架构设计
- `BUILD_GUIDE.md` - 构建部署
- `PROJECT_GUIDE.md` - 完整项目指南

### 外部资源
- [Android BLE 文档](https://developer.android.com/guide/topics/connectivity/bluetooth/ble-overview)
- [Android Foreground Services](https://developer.android.com/guide/components/foreground-services)
- [Jetpack Compose 文档](https://developer.android.com/jetpack/compose)
- [nRF Connect 工具](https://www.nordicsemi.com/Products/Development-tools/nrf-connect-for-mobile)

---

**文档维护**: 请在重大更改后更新本文档  
**版本**: 2.0.0  
**最后更新**: 2026-05-29

---

*本文档由 AI Agent 生成和维护*
