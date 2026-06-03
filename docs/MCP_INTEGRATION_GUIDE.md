# MCP Server 集成指南

## 概述

StrokeNet 集成了 MCP (Model Context Protocol) Server，允许 AI 代理（如 Claude Desktop、Kiro）通过网络控制设备。

## 功能特性

### 运行模式

#### **本地模式**
- 同一 WiFi 网络下访问
- 低延迟，稳定可靠
- URL 格式：`http://192.168.x.x:8080/mcp`
- 适合：家庭、办公室使用

> ⚠️ **注意**: 2.0 版本仅支持本地模式，确保手机和 AI 工具在同一 WiFi 网络下

### 可用工具

| 工具名称 | 描述 | 参数 |
|---------|------|------|
| `thrust` | 控制推拉运动 | depth (1-100), extend (1-100), retract (1-100) |
| `strength` | 设置震动强度 | value (1-100) |
| `temperature` | 设置加热温度（立即生效） | value (0-60°C) |
| `start_heating` | 启动定时加热 | temperature (1-60°C), duration (1-10分钟) |
| `stop_heating` | 停止加热定时器 | 无 |
| `send_all` | 批量发送所有参数 | depth, extend, retract, strength, temperature (可选) |
| `stop_thrust` | 停止推拉 | 无 |
| `stop_strength` | 停止震动 | 无 |
| `stop_all` | 全部停止 | 无 |

**注意**：参数范围为 UI 实际使用的 1-100，温度为 0-60°C

## 使用步骤

### 1. 在 App 中启动服务器

1. 打开 StrokeNet App
2. 导航到「MCP Server」页面
3. 点击「启动服务」按钮
4. 复制显示的服务地址（完整 URL 包含 `/mcp` 路径）

**服务地址示例**: `http://192.168.1.5:8080/mcp`

> 💡 **提示**: IP 地址会根据您的网络自动检测

### 2. 配置 AI 工具

#### Claude Desktop 配置

**Windows**: `%APPDATA%\Claude\claude_desktop_config.json`  
**macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`  
**Linux**: `~/.config/Claude/claude_desktop_config.json`

```json
{
  "mcpServers": {
    "strokenet": {
      "url": "http://192.168.1.5:8080/mcp"
    }
  }
}
```

#### Kiro MCP 配置

创建或编辑 `.kiro/settings/mcp.json`：

```json
{
  "mcpServers": {
    "strokenet": {
      "url": "http://192.168.1.5:8080/mcp"
    }
  }
}
```

⚠️ **重要提示**:
- 替换 `192.168.1.5` 为您手机的实际 IP 地址
- URL 路径必须包含 `/mcp`（SDK 默认端点）
- 配置后需要重启 AI 工具

### 3. 测试连接

在 AI 工具中尝试：

```
"使用 StrokeNet 设置深度为 50，伸展速度 80，收缩速度 65"
```

AI 将调用 `thrust` 工具，发送指令到设备。

## 技术架构

### 核心组件

```
┌─────────────────────────────────────────┐
│         AI Agent (Claude/Kiro)          │
│   通过 MCP 协议调用工具                   │
└────────────────┬────────────────────────┘
                 │ HTTP/Streamable (官方 SDK)
                 ▼
┌─────────────────────────────────────────┐
│    StrokeNet MCP Server (官方 SDK)       │
│   - Server() 构造                        │
│   - addTool() 注册工具                   │
│   - mcpStreamableHttp() 挂载             │
└────────────────┬────────────────────────┘
                 │ BLE 命令
                 ▼
┌─────────────────────────────────────────┐
│         BleService (前台服务)            │
│   - 推拉控制                             │
│   - 震动控制                             │
│   - 温度控制                             │
└─────────────────────────────────────────┘
```

### 网络拓扑

```
AI Tool (WiFi) ──HTTP──> Android Device (WiFi)
                         ↓ BLE
                      Daxiu 设备
```

## MCP 协议实现

本项目使用**官方 Kotlin MCP SDK**，协议细节由 SDK 自动处理。

### 客户端连接示例

```json
{
  "mcpServers": {
    "strokenet": {
      "url": "http://192.168.1.5:8080/mcp"
    }
  }
}
```

**重要**：URL 路径必须包含 `/mcp`（SDK 默认端点）

## 安全注意事项

1. **本地模式**：仅限同一 WiFi，相对安全
2. **建议添加认证机制**：当前无认证，同网络内任何人都可调用

## 故障排查

### 服务器无法启动
- 检查端口 8080 是否被占用
- 查看 Logcat：`adb logcat -s McpServerService`
- 确认网络权限已授予
- 重启应用

### 连接超时
- ✅ 确认 AI 工具与手机在**同一 WiFi 网络**
- ✅ 检查防火墙设置（Windows Defender/杀毒软件）
- ✅ 确认 IP 地址正确（可能会变化）
- ✅ 尝试 ping 手机 IP：`ping 192.168.1.5`

### AI 无法调用工具
- 验证 MCP 配置 JSON 格式正确
- 确认 URL 包含 `/mcp` 路径
- 测试端点健康检查：
  ```bash
  curl http://192.168.1.5:8080/
  # 应返回：StrokeNet MCP Server Running
  ```
- 使用 MCP Inspector 调试：
  ```bash
  npx @modelcontextprotocol/inspector http://192.168.1.5:8080/mcp
  ```

### 找不到工具列表
- 确认服务器已启动（查看 App 状态指示）
- 重启 AI 客户端
- 检查网络连接稳定性

## 未来优化

- [ ] 添加认证机制（API Key / Token）
- [ ] WebSocket 传输支持
- [ ] 实时状态推送（SSE）
- [ ] 自定义端口配置
- [ ] 多设备协同支持

---

**版本**: 2.0  
**最后更新**: 2026-06-03