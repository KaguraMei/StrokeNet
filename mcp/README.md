# StrokeNet MCP Server

通过 Model Context Protocol (MCP) 控制 StrokeNet 设备。

## 🚀 快速开始

### 推荐方式（最简单）

```bash
cd mcp
chmod +x start.sh stop.sh
./start.sh
```

选择模式 1 (HTTP 协议)，然后在 Claude Desktop 中配置 MCP 服务器。

---

## 📁 文件说明

### 核心文件
- **`daxiu_mcp_http.py`** - HTTP 协议版本（Claude Desktop 新版本）⭐ 推荐
- **`daxiu_mcp_auto.py`** - 自动检测版本（支持所有协议）
- **`start.sh`** - 交互式启动脚本
- **`stop.sh`** - 停止所有服务

### 快捷启动脚本
- **`start_http.sh`** - 直接启动 HTTP 模式
- **`start_sse.sh`** - 直接启动 SSE 模式（旧版本）

### 版本兼容文件（高级用户）
- `daxiu_mcp_v1.py` - FastMCP 1.x
- `daxiu_mcp_v2_http.py` - FastMCP 2.x HTTP
- `daxiu_mcp_v2_stdio.py` - FastMCP 2.x stdio
- `daxiu_mcp_v3.py` - FastMCP 3.x

### 文档
- **`CLAUDE_MCP_SETUP.md`** - Claude Desktop 配置详细指南
- **`MCP_VERSION_GUIDE.md`** - 版本兼容性指南

---

## 📋 使用步骤

### 1. 安装依赖
```bash
pip install fastmcp uvicorn
```

### 2. 启动服务
```bash
cd mcp
./start.sh
```

选择 `1` (HTTP 协议)

### 3. 获取 URL
启动后会显示 cloudflared URL，例如：
```
https://minimum-goals-reaction-establishment.trycloudflare.com
```

### 4. 配置 Claude Desktop
在 Claude Desktop 中添加 MCP 服务器：
- **名称**: StrokeNet
- **URL**: `https://你的域名.trycloudflare.com/mcp`
- **协议**: HTTP

⚠️ **注意**: URL 末尾必须加 `/mcp`

### 5. 测试
在 Claude 中输入：
```
请列出可用的工具
```

应该能看到 5 个工具：
- daxiu_start - 启动推拉
- daxiu_thrust - 调节推拉
- daxiu_strength - 设置强度
- daxiu_temp - 设置温度
- daxiu_stop - 停止

---

## 🛠️ 管理命令

### 启动服务
```bash
./start.sh          # 交互式选择
./start_http.sh     # 直接启动 HTTP 模式
./start_sse.sh      # 直接启动 SSE 模式
```

### 停止服务
```bash
./stop.sh
```

### 重启服务
```bash
./stop.sh && ./start_http.sh
```

---

## 🔧 高级用法

### 命令行指定模式
```bash
./start.sh http     # HTTP 协议
./start.sh sse      # SSE 协议
./start.sh stdio    # stdio 模式
./start.sh auto     # 自动检测
```

### 环境变量配置
```bash
export MCP_TRANSPORT=http
export MCP_HOST=0.0.0.0
export MCP_PORT=3459
python daxiu_mcp_auto.py
```

### 直接运行特定版本
```bash
python daxiu_mcp_http.py        # HTTP 协议
python daxiu_mcp_auto.py        # 自动检测
```

---

## 📖 详细文档

- **Claude Desktop 配置**: 查看 `CLAUDE_MCP_SETUP.md`
- **版本兼容性**: 查看 `MCP_VERSION_GUIDE.md`

---

## ❓ 常见问题

### Q: 如何停止服务？
```bash
./stop.sh
```

### Q: 如何切换协议？
```bash
./stop.sh
./start_http.sh  # 或 ./start_sse.sh
```

### Q: cloudflared URL 每次都变？
这是正常的。免费版每次启动都会生成新的临时域名。

### Q: 服务启动失败？
检查依赖：
```bash
pip install --upgrade fastmcp uvicorn
```

---

## 🔗 相关链接

- [FastMCP 文档](https://github.com/jlowin/fastmcp)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [Cloudflare Tunnel](https://developers.cloudflare.com/cloudflare-one/connections/connect-apps/)
