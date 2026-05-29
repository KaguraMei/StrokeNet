# StrokeNet

> 基于 BLE 广播协议的 Android 控制应用

[![Android](https://img.shields.io/badge/Android-12%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-Latest-orange.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## ✨ 核心功能

- 📡 **BLE 广播控制** - 无需配对，直接通过 BLE 广播控制设备
- 🔄 **前台服务** - 后台可靠发送，失败自动重试（最多2次）
- 🎚️ **完整参数控制** - 推拉深度、伸出/缩回速度、强度、温度
- 📋 **预设模式** - 内置多种预设，一键启动
- 🔌 **Intent 接口** - 支持外部调用，可与 MCP/Termux 集成
- 🔔 **通知反馈** - 实时显示发送状态和结果

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

- **[docs/RELEASE_BUILD_GUIDE.md](docs/RELEASE_BUILD_GUIDE.md)** - 正式版 APK 打包指南 📦 新增
- **[docs/SERVICE_INTEGRATION.md](docs/SERVICE_INTEGRATION.md)** - BLE Service 集成说明 ⭐
- **[docs/UUID_ENCODING.md](docs/UUID_ENCODING.md)** - UUID 编码配置 ⚠️ 重要
- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** - 架构设计说明
- **[docs/BUILD_GUIDE.md](docs/BUILD_GUIDE.md)** - 构建部署指南
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

### UUID 编码调整

**当前的 UUID 编码是示例代码**，需要根据实际设备调整：

1. 使用 nRF Connect Scanner 抓取官方 APP 的 UUID
2. 分析字节位变化规律
3. 修改 `BleAdvertiser.kt` 中的 `buildControlUuid()` 函数
4. 重新编译测试

详见 [UUID_ENCODING.md](docs/UUID_ENCODING.md)

### 权限要求

Android 12+ 必须手动在设置中授予权限：
- 设置 → 应用 → StrokeNet → 权限
- 开启：蓝牙、位置信息（精确位置）

### 推拉参数规则

推拉的三个参数（depth / extendSpeed / retractSpeed）必须同时设置，设备才会执行动作。

## 🤝 MCP 服务集成

在 Termux 中运行 MCP 服务，通过 AI 控制设备。

### 📱 环境准备

#### 1. 安装必要应用

- **Termux** - 终端模拟器
- **Material Files** - 文件管理器

#### 2. 配置 Termux 存储访问

由于 Termux 的工作目录位于 `data/data/com.termux/...`，Android 常规文件管理器无法直接访问该路径。需要通过 Material Files 配置存储空间：

1. 打开 **Material Files**
2. **左滑** 打开侧边菜单
3. 选择 **添加存储空间** → **外部存储空间**
4. 点击左上角 **三** 字图标（菜单）
5. 选择 **Termux**

现在可以在 Material Files 中访问 Termux 目录了，一般是home。

#### 3. 复制 MCP 文件到 Termux

将项目中的 `mcp/` 目录下的所有文件复制到 Termux 的主目录：

```
项目/mcp/* → Termux/home/
```

可以通过 Material Files 直接复制粘贴。

#### 4. 安装依赖环境

打开 **Termux**，依次执行以下命令：

```bash
# 更新包管理器
pkg update && pkg upgrade

# 安装 Python
pkg install python

# 安装 Rust（fastMCP 依赖）
pkg install rust

# 安装 fastMCP（⚠️ 这一步可能需要 10-30 分钟，耐心等待）
pip install fastmcp

# 等待直到看到 -$ 提示符即表示安装完成
```

**注意**：fastMCP 会自动安装 3.x 版本，安装过程中会编译 Rust 组件，时间较长属于正常现象。

#### 5. 安装 Cloudflared（可选，用于远程访问）

```bash
# 直接从 Termux 官方仓库安装
pkg install cloudflared
```

### 🚀 启动 MCP 服务

#### 方式一：使用启动脚本（推荐）

```bash
cd ~
bash start.sh
```

#### 方式二：直接运行 Python 脚本

```bash
cd ~
python daxiu_mcp_auto.py
# 或
python daxiu_mcp_http.py
```

服务启动后会监听在 `0.0.0.0:3459`，等待 AI 客户端连接。

**⚠️ 注意**：如果使用 `daxiu_mcp_http.py`，请确保脚本中已显式指定端口为 3459（已修复），否则 FastMCP 会使用默认端口 8000。

### 🌐 配置远程访问（可选）

如果需要从外网访问 MCP 服务，需要启动 Cloudflared 隧道。

**在 Termux 中新开一个会话**（下拉通知栏 → Termux → NEW SESSION）：

```bash
cloudflared tunnel --url http://localhost:3459
```

Cloudflared 会输出一个公网 URL，例如：
```
https://random-name-1234.trycloudflare.com
```

将这个 URL 配置到 AI 客户端的 MCP 服务器地址即可。

### 🔧 MCP 服务代码示例

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

### 💡 使用方式

```bash
# 1. 在 Termux 会话 1 中启动 MCP 服务
python daxiu_mcp.py

# 2. 在 Termux 会话 2 中启动 Cloudflared（可选）
cloudflared tunnel --url http://localhost:3459

# 3. 在 AI 客户端中配置 MCP 服务器
# 本地: http://localhost:3459
# 远程: https://your-cloudflare-url.trycloudflare.com

# 4. 通过 AI 对话控制设备
# "启动设备，深度50，速度10"
# → daxiu_start(depth=50, extend_speed=10, retract_speed=10)
```

### 📝 完整启动流程总结

1. ✅ 安装 Termux 和 Material Files
2. ✅ 配置 Material Files 访问 Termux 存储
3. ✅ 复制 `mcp/` 目录文件到 Termux
4. ✅ 安装 Python、Rust、fastMCP
5. ✅ 运行 `start.sh` 或 `python daxiu_mcp_auto.py`
6. ✅ （可选）新开会话运行 `cloudflared tunnel --url http://localhost:3459`
7. ✅ 配置 AI 客户端连接 MCP 服务器
8. ✅ 开始使用 AI 控制设备

## 🚀 未来功能（占位）

### 自定义动作循环

按照预设的动作序列自动变化参数：

```
慢浅 (深度=20, 速度=5) → 持续 10 秒
慢深 (深度=60, 速度=5) → 持续 10 秒
快深 (深度=60, 速度=12) → 持续 5 秒
循环回到开始
```

实现方案见 [SERVICE_INTEGRATION.md](docs/SERVICE_INTEGRATION.md#未来扩展自定义动作循环)

## 🔧 开发

### 系统要求
- Android 12 (API 31) 或更高版本
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
- 🚀 Termux + Cloudflared 实现远程访问

感谢作者分享的宝贵经验，为本项目提供了核心技术思路。

---

**项目状态**: ✅ 核心功能完成，等待 UUID 编码调整  
**版本**: 1.0.0  
**最后更新**: 2026-05-29
