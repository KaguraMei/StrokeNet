# StrokeNet 2.0

> 基于 BLE 广播协议的 Android 控制应用 | 内置官方 MCP SDK | AI 原生控制

[![Android](https://img.shields.io/badge/Android-7%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![MCP SDK](https://img.shields.io/badge/MCP_SDK-0.6.0-brightgreen.svg)](https://github.com/modelcontextprotocol/kotlin-sdk)
[![Compose](https://img.shields.io/badge/Compose-Latest-orange.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

**项目状态**: ✅ **2.0 正式版发布**  
**版本**: 2.0.0 | **MCP SDK**: kotlin-sdk-server 0.6.0 | **Android**: API 24+ (Android 7.0+)  
**最后更新**: 2026-06-03

## 🔗 快速链接

- 📱 [下载 APK](../../releases) - 最新发布版本
- 📖 [完整文档](docs/DOCS_INDEX.md) - 所有文档索引
- 🚀 [快速开始](docs/MCP_QUICKSTART.md) - 5 分钟上手
- 🎨 [预设指南](docs/MCP_PRESET_GUIDE.md) - 创建自定义预设
- 💻 [开发指南](docs/AI_AGENT_GUIDE.md) - AI Agent 综合指南
- ⚠️ [UUID 协议](docs/UUID_ENCODING.md) - 正确的编码实现

---

## ✨ 2.0 核心特性

### 🌐 内置 MCP Server（官方 SDK）
- ✅ 基于官方 Kotlin MCP SDK 0.6.0
- ✅ 无需 Termux，一键启动
- ✅ 20+ MCP 工具，完整控制能力
- ✅ 标准 HTTP Streamable 传输协议

### 🎨 自定义循环预设
- ✅ 可视化创建、编辑预设
- ✅ 多段动作自动循环执行
- ✅ JSON 格式导入导出分享
- ✅ 应用启动时自动加载所有预设

### 🔧 完整功能
- ✅ BLE 广播控制（无需配对）
- ✅ 前台服务保活 + 失败重试
- ✅ 完整参数控制（推拉、震动、温度）
- ✅ 加热定时器（1-10分钟自动关闭）
- ✅ MCP 管理界面（状态、工具、日志）
- ✅ Intent 接口（可选 Termux 集成）

## 🚀 快速开始

### 编译安装（开发版）
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 打包正式版 APK
详见 [正式版打包指南](docs/RELEASE_BUILD_GUIDE.md) 📦

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
综合开发指南，整合了所有关键信息。

**👤 给人类开发者**: 从本文档开始，然后查看 [`docs/DOCS_INDEX.md`](docs/DOCS_INDEX.md) 获取完整文档导航。

### 📚 核心文档

#### 🌐 MCP Server（2.0 内置）
- **[docs/MCP_QUICKSTART.md](docs/MCP_QUICKSTART.md)** - **5 分钟快速开始** ⚡⚡⚡
- **[docs/MCP_INTEGRATION_GUIDE.md](docs/MCP_INTEGRATION_GUIDE.md)** - 完整集成指南
- **[docs/MCP_PRESET_GUIDE.md](docs/MCP_PRESET_GUIDE.md)** - 预设管理和 MCP 工具详解

#### 📦 构建和发布
- **[docs/RELEASE_BUILD_GUIDE.md](docs/RELEASE_BUILD_GUIDE.md)** - 正式版 APK 打包指南

#### ⚙️ 技术细节
- **[docs/UUID_ENCODING.md](docs/UUID_ENCODING.md)** - UUID 编码协议详解 ⚠️ 必读
- **[docs/SERVICE_INTEGRATION.md](docs/SERVICE_INTEGRATION.md)** - BLE Service 集成说明
- **[docs/DOCS_INDEX.md](docs/DOCS_INDEX.md)** - 完整文档索引

## 🏗️ 项目结构

```
app/src/main/java/aya/strokenet/
├── MainActivity.kt              # 主入口，处理 Intent 调用
├── BleService.kt               # 前台服务，重试逻辑
├── BleAdvertiser.kt            # BLE 广播工具类
├── data/model/                 # 数据模型
│   ├── ControlParams.kt        # 控制参数
│   └── Preset.kt               # 预设模式
└── ui/                         # UI 层
    ├── screens/                # 页面
    │   ├── ControlScreen.kt    # 控制页面
    │   ├── PresetsScreen.kt    # 预设页面
    │   └── SettingsScreen.kt   # 设置页面
    └── components/             # 可复用组件
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

### Intent 调用接口

所有指令通过 Intent 发送到 MainActivity，自动启动前台服务处理：

```bash
# 启动推拉
am start -n aya.strokenet/.MainActivity \
  --es action start --ei depth 36 --ei extend 8 --ei retract 8

# 运行中调节参数
am start -n aya.strokenet/.MainActivity \
  --es action thrust --ei depth 50 --ei extend 10 --ei retract 10

# 设置强度
am start -n aya.strokenet/.MainActivity \
  --es action strength --ei value 80

# 设置温度
am start -n aya.strokenet/.MainActivity \
  --es action temp --ei value 40

# 停止
am start -n aya.strokenet/.MainActivity --es action stop
```

### 工作流程

```
Intent 调用
    ↓
MainActivity 接收
    ↓
启动 BleService (前台服务)
    ↓
BleService 调用 BleAdvertiser
    ↓
发送 BLE 广播
    ↓
失败？→ 重试（最多2次，间隔300ms）
    ↓
通知栏显示结果
    ↓
自动停止服务
```

## 🔔 前台服务特性

### 为什么使用前台服务？

官方 APP 的问题：切后台就停止广播，导致设备卡住。

StrokeNet 的解决方案：
- ✅ 前台服务保活，切后台继续运行
- ✅ 失败自动重试（最多2次）
- ✅ 通知栏实时反馈状态
- ✅ 任务完成自动停止

### 通知栏状态示例

```
发送中: 推拉: 深度=50 伸=10 缩=10
重试中 (1/2): 推拉: 深度=50 伸=10 缩=10
✓ 已发送: 推拉: 深度=50 伸=10 缩=10
✗ 发送失败: 推拉: 深度=50 (错误码: -4)
```

详见 [SERVICE_INTEGRATION.md](docs/SERVICE_INTEGRATION.md)

## ⚠️ 重要提示

### UUID 编码协议

本项目的 BLE 协议实现**完全基于官方 APP 的反编译结果**。

感谢 **[用 AI 远程控制你的 Cachito 大秀炮机](https://claude.ai/public/artifacts/921eda06-e567-4cde-85af-8cde831a608f)** 教程提供的逆向思路和 MCP 集成灵感，但需要指出：**该文章中给出的 UUID 编码逻辑并不正确**（参数位置、校验和计算等与官方实现不符）。

**本项目实现的正确编码协议请参考**：[docs/UUID_ENCODING.md](docs/UUID_ENCODING.md) 📖

主要差异：
- ✅ 完整的 UUID 格式：`710003XX-YYYY-ZZZZ-0000-WWWWWWWWWWCC`
- ✅ 正确的参数编码位置（最后12位）
- ✅ 校验和计算机制
- ✅ 设备ID持久化存储
- ✅ 准确的参数映射公式

详见 [UUID_ENCODING.md](docs/UUID_ENCODING.md)

### 权限要求

Android 12+ 必须手动在设置中授予权限：
- 设置 → 应用 → StrokeNet → 权限
- 开启：蓝牙、位置信息（精确位置）

### 推拉参数规则

推拉的三个参数（depth / extendSpeed / retractSpeed）必须同时设置，设备才会执行动作。

## 🤝 控制方式

### 方式一：内置 MCP Server（推荐）🌐

**StrokeNet 2.0** 内置标准 MCP Server，**无需 Termux**，一键启动即可使用。

#### 技术栈
- **Kotlin MCP SDK 0.6.0** - 官方标准实现
- **Ktor 3.0** - 高性能 HTTP 服务器
- **Streamable HTTP** - 标准 MCP 传输协议

#### 📱 本地模式
- 同一 WiFi 网络下访问
- 低延迟，稳定可靠
- URL: `http://192.168.x.x:8080/mcp`

#### ⚡ 快速开始

1. 在 App 中打开「MCP」页面
2. 点击「启动服务」按钮
3. 复制显示的服务地址（包含 `/mcp` 路径）
4. 在 AI 工具（Claude Desktop/Kiro）中配置 MCP Server
5. 通过自然语言控制设备

#### 配置示例

```json
{
  "mcpServers": {
    "strokenet": {
      "url": "http://192.168.1.5:8080/mcp"
    }
  }
}
```

#### 🎨 MCP 预设管理

StrokeNet 2.0 支持通过 MCP 完整管理预设：

- 📋 `list_presets` - 列出所有预设
- 📖 `get_official_presets` - 获取官方预设
- 📝 `get_custom_presets` - 获取自定义预设
- ➕ `create_custom_preset` - 创建自定义预设
- ✏️ `update_custom_preset` - 更新自定义预设
- ▶️ `run_preset` - 运行预设
- ⏹️ `stop_preset` - 停止预设
- 🗑️ `delete_custom_preset` - 删除自定义预设
- 📤 `export_custom_presets` - 导出预设
- 📥 `import_custom_presets` - 导入预设

详见 **[MCP_PRESET_GUIDE.md](docs/MCP_PRESET_GUIDE.md)** 和 **[MCP_INTEGRATION_GUIDE.md](docs/MCP_INTEGRATION_GUIDE.md)** 📖

### 方式二：Termux + Python MCP（可选）

> ⚠️ **注意**：StrokeNet 2.0 推荐使用内置 MCP Server。Termux 方式保留用于需要自定义 Python MCP 工具的高级用户。

如果你需要：
- 自定义 Python MCP 工具
- 使用 fastMCP 快速原型开发
- 1.0 版本的 Termux 工作流

请查看：**[Termux MCP 配置指南（1.0 版本留档）](docs/TERMUX_MCP_GUIDE.md)** 📖

---

## 🎯 2.0 版本新特性

### ✅ 已实现

- ✅ **内置 MCP Server** - 基于官方 Kotlin MCP SDK 0.6.0
- ✅ **自定义循环预设** - 可视化创建、编辑、运行
- ✅ **预设导入导出** - JSON 格式分享
- ✅ **加热定时器** - 温度 + 自动关闭时长
- ✅ **应用启动预加载** - 官方预设 + 自定义预设自动加载
- ✅ **完整 MCP 工具集** - 20+ 工具，涵盖所有功能
- ✅ **MCP 管理界面** - 服务状态、工具列表、请求日志

### 🔄 架构升级

**1.x → 2.0**:
- ❌ 需要 Termux 环境 → ✅ 内置 MCP Server
- ❌ 5 个基础工具 → ✅ 20+ 完整工具
- ❌ 无预设管理 → ✅ 完整预设系统
- ❌ 手动安装依赖 → ✅ 开箱即用

## 🔧 开发

### 系统要求
- Android 7 (API 24) 或更高版本
- JDK 11+
- Android SDK
- 支持 BLE 的设备

### 调试
```bash
# 查看日志
adb logcat | grep -E "BleAdvertiser|BleService"

# 查看 Service 状态
adb shell dumpsys activity services aya.strokenet

# 验证广播（使用 nRF Connect Scanner）
```

### 测试脚本
```bash
# 测试 MCP 服务调用
bash test_mcp_service.sh
```

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE)

## 🙏 致谢

本项目的 BLE 逆向工程和 MCP 集成方案受到以下教程的启发：

**[用 AI 远程控制你的 Cachito 大秀炮机：BLE 逆向 + MCP 全链路教程](https://claude.ai/public/artifacts/921eda06-e567-4cde-85af-8cde831a608f)**

该教程详细讲解了：
- 🔍 如何使用 nRF Connect Scanner 逆向 BLE 广播协议
- 📡 BLE Advertiser 模式与传统 GATT 连接的区别
- 🤖 通过 MCP 让 AI 控制智能设备的完整链路
- 🚀 Termux + MCP 实现远程 AI 控制

感谢作者分享的宝贵经验，为本项目提供了核心技术思路。

### ⚠️ 重要说明

**本项目的 BLE 协议实现完全基于官方 APP 的反编译结果**。该教程虽然提供了很好的思路，但**文章中的 UUID 编码逻辑并不正确**（参数位置、校验和计算等与官方实现不符）。

**本项目实现的正确编码协议请参考**：[UUID_ENCODING.md](docs/UUID_ENCODING.md) ⚠️

主要差异：
- ✅ 完整 UUID 格式：`710003XX-YYYY-ZZZZ-0000-WWWWWWWWWWCC`
- ✅ 正确的参数编码位置（最后12位）
- ✅ 校验和计算机制
- ✅ 设备 ID 持久化存储
- ✅ 准确的参数映射公式

如果你在开发类似项目，请以本项目的实现和文档为准。

---

**项目状态**: ✅ **2.0 正式版发布**  
**版本**: 2.0.0  
**MCP SDK**: kotlin-sdk-server 0.6.0  
**Android**: API 24+ (Android 7.0+)  
**最后更新**: 2026-06-03

## 🔗 快速链接

- 📱 [下载 APK](../../releases) - 最新发布版本
- 📖 [完整文档](docs/DOCS_INDEX.md) - 所有文档索引
- 🚀 [快速开始](docs/MCP_QUICKSTART.md) - 5 分钟上手
- 🎨 [预设指南](docs/MCP_PRESET_GUIDE.md) - 创建自定义预设
- 💻 [开发指南](docs/AI_AGENT_GUIDE.md) - AI Agent 综合指南
- ⚠️ [UUID 协议](docs/UUID_ENCODING.md) - 正确的编码实现
