# StrokeNet

> 基于 BLE 广播协议的 Android 控制应用 | iOS 玻璃拟态设计

[![Android](https://img.shields.io/badge/Android-12%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-Latest-orange.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## ✨ 特性

- 🎨 **iOS 玻璃拟态设计** - 半透明悬浮面板、纯白简洁、苹果美学
- 📡 **BLE 广播控制** - 无需配对，直接通过广播控制设备
- 🎚️ **5 种控制参数** - 推拉深度、伸出/缩回速度、强度、温度
- 📋 **预设模式** - 轻柔/标准/强力三种内置预设
- 🔌 **Intent 支持** - 支持外部调用，可与 MCP 服务集成
- 📱 **专注手机端** - 为手机屏幕优化的 UI 设计

## 🚀 快速开始

### 编译安装
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 授予权限
```bash
adb shell pm grant aya.strokenet android.permission.BLUETOOTH_ADVERTISE
adb shell pm grant aya.strokenet android.permission.BLUETOOTH_CONNECT
adb shell pm grant aya.strokenet android.permission.ACCESS_FINE_LOCATION
```

### 测试控制
```bash
# 启动推拉
adb shell am start -n aya.strokenet/.MainActivity \
  --es action start --ei depth 36 --ei extend 8 --ei retract 8

# 停止
adb shell am start -n aya.strokenet/.MainActivity --es action stop
```

## 📖 文档

### 🎯 快速导航

**👨‍💻 给 AI Agent**: [`docs/AI_AGENT_GUIDE.md`](docs/AI_AGENT_GUIDE.md) ⭐⭐⭐⭐⭐  
综合开发指南，整合了所有关键信息，专为 AI Agent 设计。

**👤 给人类开发者**: 从本文档开始，然后查看 [`docs/DOCS_INDEX.md`](docs/DOCS_INDEX.md) 获取完整文档导航。

### 📚 核心文档

- **[docs/AI_AGENT_GUIDE.md](docs/AI_AGENT_GUIDE.md)** - AI Agent 综合开发指南 ⭐ 推荐
- **[docs/DOCS_INDEX.md](docs/DOCS_INDEX.md)** - 文档索引和导航
- **[docs/DESIGN_IOS_GLASSMORPHISM.md](docs/DESIGN_IOS_GLASSMORPHISM.md)** - iOS 玻璃拟态设计系统
- **[docs/UUID_ENCODING.md](docs/UUID_ENCODING.md)** - UUID 编码配置 ⚠️ 重要
- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** - 架构设计说明
- **[docs/BUILD_GUIDE.md](docs/BUILD_GUIDE.md)** - 构建部署指南

### 📋 参考文档

- **[docs/PROJECT_GUIDE.md](docs/PROJECT_GUIDE.md)** - 完整项目指南
- **[docs/UI_REDESIGN_SUMMARY.md](docs/UI_REDESIGN_SUMMARY.md)** - UI 重构总结
- **[docs/DEBUG_TIPS.md](docs/DEBUG_TIPS.md)** - 调试技巧
- **[docs/QUICKSTART.md](docs/QUICKSTART.md)** - 快速上手

## 🎨 设计风格

采用 **iOS 玻璃拟态（Glassmorphism）** 设计语言：

- ✅ 半透明白色玻璃面板（75% 透明度）
- ✅ 大圆角设计（24dp）
- ✅ 极淡光晕阴影（3% 透明度）
- ✅ iOS 经典配色（#007AFF 蓝、#34C759 绿、#FF3B30 红）
- ✅ 浅灰背景（#F2F2F7）
- ✅ 悬浮 Dock 式导航栏
- ✅ 纯净简约的视觉降噪

详见 [DESIGN_IOS_GLASSMORPHISM.md](DESIGN_IOS_GLASSMORPHISM.md)

## 🏗️ 项目结构

```
app/src/main/java/com/ec/strokenet/
├── MainActivity.kt              # 主入口
├── BleAdvertiser.kt            # BLE 广播核心
├── data/model/                 # 数据模型
│   ├── ControlParams.kt
│   └── Preset.kt
└── ui/                         # UI 层
    ├── navigation/Screen.kt
    ├── screens/                # 页面
    │   ├── ControlScreen.kt
    │   ├── PresetsScreen.kt
    │   └── SettingsScreen.kt
    ├── components/             # 组件
    │   ├── ControlSlider.kt
    │   ├── PresetCard.kt
    │   └── StatusIndicator.kt
    └── theme/
        └── ConstructStyle.kt   # 设计系统
```

## 🔌 控制协议

### 参数范围
| 参数 | 范围 | 说明 |
|------|------|------|
| depth | 0-72 | 推拉深度 |
| extendSpeed | 0-15 | 伸出速度 |
| retractSpeed | 0-15 | 缩回速度 |
| strength | 0-100 | 入体端强度 |
| temp | 0-60 | 加热温度（℃）|

### Intent 调用
```bash
# 启动推拉
am start -n aya.strokenet/.MainActivity \
  --es action start --ei depth 36 --ei extend 8 --ei retract 8

# 设置强度
am start -n aya.strokenet/.MainActivity \
  --es action strength --ei value 80

# 设置温度
am start -n aya.strokenet/.MainActivity \
  --es action temp --ei value 40

# 停止
am start -n aya.strokenet/.MainActivity --es action stop
```

## ⚠️ 重要提示

### UUID 编码调整
当前的 UUID 编码是示例代码，需要根据实际设备调整：

1. 使用 nRF Connect Scanner 抓取官方 APP 的 UUID
2. 分析字节位变化规律
3. 修改 `BleAdvertiser.kt` 中的 `buildControlUuid()` 函数
4. 重新编译测试

详见 [UUID_ENCODING.md](UUID_ENCODING.md)

### 权限要求
Android 12+ 必须手动在设置中授予权限：
- 设置 → 应用 → StrokeNet → 权限
- 开启：蓝牙、位置信息（精确位置）

### 推拉参数规则
推拉的三个参数（depth / extendSpeed / retractSpeed）必须同时设置，设备才会执行动作。

## 🔧 开发

### 系统要求
- Android 12 (API 31) 或更高版本
- JDK 11+
- Android SDK
- 支持 BLE 的设备

### 调试
```bash
# 查看日志
adb logcat | grep BleAdvertiser

# 验证广播
# 使用 nRF Connect Scanner 抓取 APP 发出的广播
```

## 🤝 MCP 服务集成

在 Termux 中运行 MCP 服务：

```python
# mcp_server_example.py
from mcp.server.fastmcp import FastMCP
import subprocess

mcp = FastMCP("strokenet")

@mcp.tool()
def strokenet_start(depth: int = 36, extend_speed: int = 8, retract_speed: int = 8) -> str:
    """启动推拉"""
    subprocess.run([
        "am", "start", "-n", "aya.strokenet/.MainActivity",
        "--es", "action", "start",
        "--ei", "depth", str(depth),
        "--ei", "extend", str(extend_speed),
        "--ei", "retract", str(retract_speed)
    ])
    return f"推拉已启动: 深度={depth}, 伸出={extend_speed}, 缩回={retract_speed}"

if __name__ == "__main__":
    mcp.run(transport="sse", host="0.0.0.0", port=3459)
```

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE)

## 🙏 致谢

设计灵感来源：
- 俄罗斯构成主义艺术运动
- 工程蓝图设计
- Bauhaus 设计风格
- De Stijl 运动

---

**项目状态**: ✅ 核心功能完成，等待 UUID 编码调整  
**版本**: 1.0.0  
**最后更新**: 2026-05-28
