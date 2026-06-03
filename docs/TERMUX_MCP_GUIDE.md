# Termux + Python MCP 配置指南（1.0 版本留档）

> ⚠️ **注意**：本文档保留用于需要自定义 Python MCP 工具的高级用户。  
> **推荐方式**：使用 [StrokeNet 2.0 内置 MCP Server](MCP_QUICKSTART.md) 📱

## 概述

本指南介绍如何在 Android 设备的 Termux 环境中运行 Python MCP 服务。

### 适用场景

- 需要自定义 Python MCP 工具
- 使用 fastMCP 快速原型开发
- 熟悉 Python 生态的开发者
- 1.0 版本的工作流迁移

### 不适用场景

如果你只是想用 AI 控制设备，**强烈推荐**使用 [内置 MCP Server](MCP_QUICKSTART.md)，无需配置 Termux。

---

## 📱 环境准备

### 1. 安装必要应用

- **Termux** - 终端模拟器 ([Google Play](https://play.google.com/store/apps/details?id=com.termux) / [F-Droid](https://f-droid.org/packages/com.termux/))
- **Material Files** - 文件管理器 ([Google Play](https://play.google.com/store/apps/details?id=me.zhanghai.android.files))

### 2. 配置 Termux 存储访问

由于 Termux 的工作目录位于 `data/data/com.termux/...`，Android 常规文件管理器无法直接访问该路径。需要通过 Material Files 配置存储空间：

1. 打开 **Material Files**
2. **左滑** 打开侧边菜单
3. 选择 **添加存储空间** → **外部存储空间**
4. 点击左上角 **≡** 图标（菜单）
5. 选择 **Termux**

现在可以在 Material Files 中访问 Termux 目录了，一般是 `home/`。

### 3. 复制 MCP 文件到 Termux

将项目中的 `mcp/` 目录下的所有文件复制到 Termux 的主目录：

```
项目/mcp/* → Termux/home/
```

可以通过 Material Files 直接复制粘贴。

### 4. 安装依赖环境

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

# 等待直到看到 $ 提示符即表示安装完成
```

---

## 🔧 MCP 服务实现

### 基础示例

```python
# daxiu_mcp.py
from mcp.server.fastmcp import FastMCP
import subprocess

mcp = FastMCP("daxiu")

def am(action, **kwargs):
    """调用 Android Activity Manager"""
    cmd = ["am", "start", "-n", "aya.strokenet/.MainActivity",
           "--es", "action", action]
    for k, v in kwargs.items():
        cmd += ["--ei", k, str(v)]
    subprocess.run(cmd, capture_output=True, text=True, timeout=5)

@mcp.tool()
def daxiu_start(depth: int = 36, extend_speed: int = 8, retract_speed: int = 8) -> str:
    """启动推拉运动
    
    Args:
        depth: 推拉深度 (0-72)
        extend_speed: 伸出速度 (0-15)
        retract_speed: 缩回速度 (0-15)
    """
    am("start", depth=depth, extend=extend_speed, retract=retract_speed)
    return f"推拉启动: 深度={depth}/72 伸出={extend_speed}/15 缩回={retract_speed}/15"

@mcp.tool()
def daxiu_thrust(depth: int, extend_speed: int, retract_speed: int) -> str:
    """运行中调节推拉参数"""
    am("thrust", depth=depth, extend=extend_speed, retract=retract_speed)
    return f"参数已调节: 深度={depth} 伸出={extend_speed} 缩回={retract_speed}"

@mcp.tool()
def daxiu_strength(value: int) -> str:
    """设置震动强度
    
    Args:
        value: 强度值 (0-100)
    """
    am("strength", value=value)
    return f"强度已设置: {value}"

@mcp.tool()
def daxiu_temp(value: int) -> str:
    """设置加热温度
    
    Args:
        value: 温度值 (0-60°C)
    """
    am("temp", value=value)
    return f"温度已设置: {value}°C"

@mcp.tool()
def daxiu_stop() -> str:
    """停止所有运动"""
    am("stop")
    return "已停止"

if __name__ == "__main__":
    # SSE 传输模式
    mcp.run(transport="sse", host="0.0.0.0", port=3459)
```

### 启动脚本

项目提供了现成的启动脚本：

```bash
# 使用 daxiu_mcp_auto.py (推荐)
python daxiu_mcp_auto.py

# 或使用 shell 脚本
bash start.sh
```

---

## 💡 使用流程

### 1. 启动 MCP 服务

```bash
# 在 Termux 中运行
cd ~
python daxiu_mcp_auto.py

# 输出示例：
# FastMCP server running on http://0.0.0.0:3459
# Tools: daxiu_start, daxiu_thrust, daxiu_strength, daxiu_temp, daxiu_stop
```

### 2. 配置 AI 客户端

#### Claude Desktop

编辑 `~/AppData/Roaming/Claude/claude_desktop_config.json` (Windows):

```json
{
  "mcpServers": {
    "daxiu-termux": {
      "url": "http://localhost:3459",
      "transport": "sse"
    }
  }
}
```

#### Kiro

编辑 `.kiro/settings/mcp.json`:

```json
{
  "mcpServers": {
    "daxiu-termux": {
      "command": "node",
      "args": ["path/to/mcp-proxy.js", "http://localhost:3459"]
    }
  }
}
```

### 3. 使用 AI 控制

重启 AI 客户端后，即可通过自然语言控制设备：

```
用户: "启动设备，深度50，速度10"
AI: [调用 daxiu_start(depth=50, extend_speed=10, retract_speed=10)]
✓ 推拉启动: 深度=50/72 伸出=10/15 缩回=10/15

用户: "设置强度80"
AI: [调用 daxiu_strength(value=80)]
✓ 强度已设置: 80

用户: "停止"
AI: [调用 daxiu_stop()]
✓ 已停止
```

---

## 🆚 对比：Termux vs 内置 MCP

| 特性 | Termux MCP | 内置 MCP Server (2.0) |
|------|-----------|---------------------|
| 易用性 | ⭐⭐⭐ 需配置环境 | ⭐⭐⭐⭐⭐ 一键启动 |
| 安装时间 | ~30 分钟 | 0 分钟 |
| 依赖 | Python, Rust, fastMCP | 无 |
| 预设管理 | ❌ | ✅ 完整支持 |
| 加热定时 | ❌ | ✅ |
| 工具数量 | 5 个基础工具 | 20+ 完整工具 |
| 自定义工具 | ✅ Python 灵活扩展 | ❌ |
| 稳定性 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| 推荐度 | 可选（高级用户） | ✅ 推荐（所有用户） |

---

## 🔧 自定义工具示例

### 添加自定义工具

```python
@mcp.tool()
def daxiu_combo_gentle(duration: int = 60) -> str:
    """温柔组合模式
    
    Args:
        duration: 持续时间（秒）
    """
    # 启动温柔设置
    am("start", depth=30, extend=5, retract=5)
    am("strength", value=40)
    
    return f"温柔模式已启动，将持续 {duration} 秒"

@mcp.tool()
def daxiu_wave_pattern() -> str:
    """波浪节奏模式（需要配合定时器）"""
    import time
    
    # 轻 → 中 → 强 → 中 → 轻
    patterns = [
        {"depth": 30, "speed": 5, "strength": 30},
        {"depth": 45, "speed": 8, "strength": 50},
        {"depth": 60, "speed": 12, "strength": 70},
        {"depth": 45, "speed": 8, "strength": 50},
        {"depth": 30, "speed": 5, "strength": 30},
    ]
    
    for p in patterns:
        am("thrust", depth=p["depth"], extend=p["speed"], retract=p["speed"])
        am("strength", value=p["strength"])
        time.sleep(10)  # 每段持续10秒
    
    return "波浪模式完成"
```

### 工具组合

```python
@mcp.tool()
def daxiu_progressive_intensity(steps: int = 5) -> str:
    """渐进增强模式
    
    Args:
        steps: 渐进步骤数 (3-10)
    """
    import time
    
    start_intensity = 20
    end_intensity = 80
    step_size = (end_intensity - start_intensity) / steps
    
    for i in range(steps):
        intensity = int(start_intensity + step_size * i)
        am("strength", value=intensity)
        time.sleep(15)  # 每步15秒
    
    return f"渐进增强完成：{start_intensity} → {end_intensity} ({steps}步)"
```

---

## ⚠️ 注意事项

### Termux 环境

1. **保持 Termux 前台运行**  
   切后台可能导致 MCP 服务暂停，建议使用 Termux:Widget 或分屏模式

2. **网络权限**  
   确保 Termux 有网络权限（首次启动会自动请求）

3. **电池优化**  
   在系统设置中关闭 Termux 的电池优化，防止后台被杀

### 安全建议

1. **仅本地访问**  
   不要将 MCP 服务暴露到公网（使用 `127.0.0.1` 而非 `0.0.0.0`）

2. **防火墙规则**  
   如果需要局域网访问，确保路由器防火墙规则正确

---

## 🐛 常见问题

### Q: fastMCP 安装失败？

A: 可能是 Rust 未正确安装。尝试：
```bash
pkg reinstall rust
pip install --upgrade pip
pip install fastmcp --no-cache-dir
```

### Q: MCP 服务无法连接？

A: 检查：
1. Termux 是否在前台运行
2. 端口 3459 是否被占用：`netstat -tuln | grep 3459`
3. AI 客户端配置的 URL 是否正确

### Q: Intent 调用无响应？

A: 确认：
1. StrokeNet App 已安装并授予必要权限
2. 使用 `adb shell` 手动测试 Intent 调用是否正常

### Q: 为什么推荐用内置 MCP 而不是 Termux？

A: 
- **易用性**：内置 MCP 无需配置，一键启动
- **功能完整**：20+ 工具，支持预设管理、加热定时等
- **稳定性**：原生 Android 服务，不受 Termux 后台限制
- **维护性**：官方 SDK 实现，长期支持

**Termux 方式主要适合**：需要自定义 Python 工具或特殊集成需求的高级用户

---

## 📚 相关文档

- [MCP 快速开始](MCP_QUICKSTART.md) - 推荐的内置 MCP Server 方式
- [MCP 集成指南](MCP_INTEGRATION_GUIDE.md) - 完整的 MCP 功能文档
- [MCP 预设指南](MCP_PRESET_GUIDE.md) - 预设管理和工具详解
- [返回主文档](../README.md)

---

**版本**: 1.0 (留档)  
**最后更新**: 2026-06-03  
**状态**: 📝 归档（推荐使用 2.0 内置 MCP Server）
