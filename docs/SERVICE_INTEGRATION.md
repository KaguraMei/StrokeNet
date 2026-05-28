# BLE Service 集成说明

## 概述

StrokeNet 使用前台服务来确保 BLE 指令可靠送达，即使应用在后台运行也能完成发送任务。

## 架构设计

### 三层架构

```
UI 层 (ControlScreen, PresetsScreen)
  - 用户交互、显示界面、启动 Service
    ↓ Intent
BleService (前台服务)
  - 生命周期管理、重试逻辑、通知栏显示、后台运行
    ↓ 调用
BleAdvertiser (蓝牙广播工具类)
  - BLE 广播功能、UUID 编码、权限检查
```

## 组件职责

### BleAdvertiser (工具类)
- 构造控制 UUID（编码参数到 UUID）
- 调用 Android BLE API 发送广播
- 检查蓝牙权限和状态
- 提供成功/失败回调
- 无状态、发送一次就结束

### BleService (前台服务)
- 前台服务（通知栏显示，系统不杀）
- 重试逻辑（失败时最多重试 2 次）
- 延迟管理（重试间隔 300ms）
- 通知更新（显示发送状态）
- 自动停止（成功后 2 秒，失败后 3 秒）

## 工作流程

### 用户操作流程
1. 用户点击按钮
2. UI 启动 BleService
3. Service 显示通知："发送中..."
4. Service 调用 BleAdvertiser
5. 成功 → 通知"已发送"，2秒后停止
6. 失败 → 重试最多2次，失败后3秒停止

### MCP 调用流程
```bash
python daxiu_mcp.py
daxiu_start(depth=50, extend_speed=10, retract_speed=10)
  ↓
adb shell am start -n aya.strokenet/.MainActivity
  ↓
MainActivity 启动 BleService
  ↓
Service 发送指令（带重试）
  ↓
通知栏显示状态
```

## 通知栏状态

- 发送中: 推拉: 深度=50 伸=10 缩=10
- 重试中 (1/2): 推拉: 深度=50 伸=10 缩=10
- ✓ 已发送: 推拉: 深度=50 伸=10 缩=10
- ✗ 发送失败: 推拉: 深度=50 (错误码: -4)

## 支持的操作

### 启动推拉 (start)
```kotlin
val intent = Intent(context, BleService::class.java).apply {
    putExtra("action", "start")
    putExtra(BleService.EXTRA_DEPTH, 50)
    putExtra(BleService.EXTRA_EXTEND, 10)
    putExtra(BleService.EXTRA_RETRACT, 10)
}
startForegroundService(intent)
```

### 停止设备 (stop)
```kotlin
val intent = Intent(context, BleService::class.java).apply {
    putExtra("action", "stop")
}
startForegroundService(intent)
```

## 未来扩展：自定义动作循环

### 概念
按照预设的动作序列自动变化参数，例如：
- 慢浅 (深度=20, 速度=5) → 持续 10 秒
- 慢深 (深度=60, 速度=5) → 持续 10 秒
- 快深 (深度=60, 速度=12) → 持续 5 秒
- 循环回到开始

### 数据模型（占位）
```kotlin
data class ActionStep(
    val name: String,
    val depth: Int,
    val extendSpeed: Int,
    val retractSpeed: Int,
    val durationSeconds: Int
)

data class ActionLoop(
    val id: String,
    val name: String,
    val steps: List<ActionStep>,
    val repeat: Boolean = true
)
```

### 实现优先级
- P0: 当前的单次发送+重试机制（已完成）
- P1: 自定义动作循环（未来功能）
- P2: 循环的保存和分享
