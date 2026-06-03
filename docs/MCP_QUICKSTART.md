# MCP Server 快速开始

## 5 分钟上手指南 ⚡

### 第一步：启动 MCP Server

1. 打开 **StrokeNet App**
2. 点击底部导航栏的 **"MCP Server"** 图标
3. 点击「启动服务」按钮
4. 等待服务启动（通常 1-2 秒）
5. 复制显示的服务地址（点击复制按钮）

**服务地址格式**: `http://192.168.x.x:8080/mcp`

> 📱 **本地模式**: 仅支持同一 WiFi 网络访问，确保手机和 AI 工具在同一网络下

### 第二步：配置 AI 工具

#### Claude Desktop

**Windows**: 编辑 `%APPDATA%\Claude\claude_desktop_config.json`  
**macOS**: 编辑 `~/Library/Application Support/Claude/claude_desktop_config.json`  
**Linux**: 编辑 `~/.config/Claude/claude_desktop_config.json`

```json
{
  "mcpServers": {
    "strokenet": {
      "url": "http://192.168.1.5:8080/mcp"
    }
  }
}
```

⚠️ **重要**: 
- 替换 `192.168.1.5` 为你手机的实际 IP 地址
- 路径必须包含 `/mcp`
- 保存后重启 Claude Desktop

#### Kiro

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

⚠️ **重要**: 
- 替换 `192.168.1.5` 为你手机的实际 IP 地址
- 路径必须包含 `/mcp`

#### 其他 MCP 客户端

任何支持 MCP HTTP 传输的客户端都可以连接：

```json
{
  "url": "http://你的手机IP:8080/mcp",
  "transport": "http"
}
```

### 第三步：开始使用 🎉

在 AI 对话中说：

```
"帮我控制设备：深度 50，伸展速度 60，收缩速度 60"
```

AI 将自动调用 MCP 工具，发送指令到您的设备。

## 💬 可用命令示例

### 基础控制

```
"设置深度为 60，伸展速度 80，收缩速度 80，震动强度 70"
"批量发送：深度 50，伸展 60，收缩 60，震动 80"
"震动强度调到 80"
"停止所有运动"
```

### 加热控制

```
"启动加热 35 度，持续 5 分钟"
"停止加热定时器"
"设置温度 40 度"（立即生效，无定时）
```

### 预设管理

```
"列出所有可用的预设"
"运行轻柔模式预设"
"创建一个温柔渐进的预设"
"导出我的自定义预设"
"停止当前运行的预设"
```

详见 **[MCP 预设指南](MCP_PRESET_GUIDE.md)** 🎨

## 🔧 故障排查

### 无法启动服务器

**现象**: 点击「启动服务」无反应或立即停止

**解决方法**:
1. 检查网络权限是否授予
2. 查看应用日志：`adb logcat -s McpServerService`
3. 重启 App
4. 检查端口 8080 是否被其他应用占用

### AI 无法连接

**现象**: AI 工具报告连接超时或无法找到 MCP Server

**解决方法**:
1. ✅ 确认手机和 AI 工具在**同一 WiFi 网络**
2. ✅ 确认 URL 包含 `/mcp` 路径
3. ✅ 确认 IP 地址正确（手机 IP 可能变化）
4. ✅ 检查防火墙设置（Windows Defender/杀毒软件）
5. ✅ 尝试重启 AI 工具

### 如何找到手机 IP 地址

**方法 1**: 在 App 的 MCP Server 页面直接查看

**方法 2**: 手机设置
- 打开 WiFi 设置
- 点击已连接的网络
- 查看 IP 地址（通常是 192.168.x.x）

**方法 3**: 使用 adb
```bash
adb shell ip addr show wlan0
```

### 测试连接

#### 方法 1: 健康检查（推荐）

```bash
# 替换 IP 地址
curl http://192.168.1.5:8080/

# 应返回：StrokeNet MCP Server Running
```

#### 方法 2: MCP Inspector（推荐）

```bash
npx -y @modelcontextprotocol/inspector http://192.168.1.5:8080/mcp
```

在浏览器中打开显示的 URL，可以：
- 查看所有可用工具
- 测试工具调用
- 查看参数定义

### 工具调用失败

**现象**: AI 调用工具但返回错误

**解决方法**:
1. 检查参数范围是否正确
2. 查看应用通知栏的错误信息
3. 查看日志：`adb logcat -s McpServerFactory BleService`
4. 确认设备是否开机并处于可接收状态

## 🚀 高级功能

想要了解更多？查看：

- **[MCP 预设指南](MCP_PRESET_GUIDE.md)** - 创建和管理自定义预设
- **[MCP 集成指南](MCP_INTEGRATION_GUIDE.md)** - 完整的技术文档
- **[LLM 开发指南](LLM.md)** - 给 AI 的完整项目文档

---

**版本**: 2.0  
**最后更新**: 2026-06-03
