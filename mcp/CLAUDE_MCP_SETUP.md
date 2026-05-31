# Claude Desktop MCP 配置指南

## 🚀 快速启动

### 方式 1: 交互式启动（推荐）
```bash
./start.sh
```
然后根据提示选择模式（推荐选择 1 - HTTP 协议）

### 方式 2: 直接启动 HTTP 模式
```bash
./start_http.sh
```

### 方式 3: 命令行指定模式
```bash
./start.sh http    # HTTP 协议（新版本）
./start.sh sse     # SSE 协议（旧版本）
./start.sh stdio   # stdio 模式
./start.sh auto    # 自动检测
```

---

## 📋 Claude Desktop 配置

### 新版本 Claude Desktop（HTTP 协议）

1. **启动服务**
   ```bash
   ./start_http.sh
   ```

2. **获取 cloudflared URL**
   
   启动后会显示类似：
   ```
   https://minimum-goals-reaction-establishment.trycloudflare.com
   ```

3. **配置 Claude Desktop**
   
   在 Claude Desktop 中添加 MCP 服务器：
   
   - **名称**: StrokeNet
   - **URL**: `https://你的域名.trycloudflare.com/mcp`
   - **协议**: HTTP
   
   ⚠️ **注意**: URL 末尾必须加 `/mcp`

4. **测试连接**
   
   在 Claude 中输入：
   ```
   请列出可用的工具
   ```
   
   应该能看到：
   - daxiu_start
   - daxiu_thrust
   - daxiu_strength
   - daxiu_temp
   - daxiu_stop

---

### 旧版本 Claude Desktop（SSE 协议）

1. **启动服务**
   ```bash
   ./start_sse.sh
   ```

2. **获取 cloudflared URL**
   
   启动后会显示类似：
   ```
   https://minimum-goals-reaction-establishment.trycloudflare.com
   ```

3. **配置 Claude Desktop**
   
   - **名称**: StrokeNet
   - **URL**: `https://你的域名.trycloudflare.com/sse`
   - **协议**: SSE
   
   ⚠️ **注意**: URL 末尾是 `/sse`

---

## 🛠️ 管理命令

### 启动服务
```bash
# 交互式选择
./start.sh

# 直接启动 HTTP 模式
./start_http.sh

# 直接启动 SSE 模式
./start_sse.sh
```

### 停止服务
```bash
./stop.sh
```

### 重启服务
```bash
./stop.sh && ./start_http.sh
```

### 查看运行状态
```bash
ps aux | grep daxiu_mcp
ps aux | grep cloudflared
```

---

## 📝 可用工具

### 1. daxiu_start
启动推拉功能
```
参数:
- depth: 深度 (0-72, 默认 36)
- extend_speed: 伸出速度 (0-15, 默认 8)
- retract_speed: 缩回速度 (0-15, 默认 8)

示例:
启动设备，深度 40，伸出速度 10，缩回速度 8
```

### 2. daxiu_thrust
运行中调节推拉参数
```
参数: 同 daxiu_start

示例:
调节推拉参数，深度 50，伸出速度 12，缩回速度 10
```

### 3. daxiu_strength
设置震动强度
```
参数:
- value: 强度 (0-100, 默认 50)

示例:
设置强度为 70
```

### 4. daxiu_temp
设置加热温度
```
参数:
- value: 温度 (0-60, 默认 30)

示例:
设置温度为 35 度
```

### 5. daxiu_stop
停止所有功能
```
示例:
停止设备
```

---

## ❓ 常见问题

### Q1: Claude 连接不上 MCP 服务器
**A**: 检查以下几点：
1. 确认服务已启动：`ps aux | grep daxiu_mcp`
2. 确认 cloudflared 正在运行：`ps aux | grep cloudflared`
3. 确认 URL 末尾有 `/mcp` 或 `/sse`
4. 尝试重启服务：`./stop.sh && ./start_http.sh`

### Q2: cloudflared URL 每次都变化
**A**: 这是正常的。免费版 cloudflared 每次启动都会生成新的临时域名。如果需要固定域名，可以：
1. 注册 Cloudflare 账号
2. 创建固定隧道
3. 修改 `start.sh` 使用固定 token

### Q3: 如何切换协议？
**A**: 
```bash
# 停止当前服务
./stop.sh

# 启动新协议
./start_http.sh  # 或 ./start_sse.sh
```

### Q4: 服务启动失败
**A**: 检查依赖：
```bash
# 检查 Python 和 FastMCP
python -c "import fastmcp; print(fastmcp.__version__)"

# 检查 cloudflared
cloudflared --version

# 重新安装依赖
pip install --upgrade fastmcp uvicorn
```

### Q5: 如何查看日志？
**A**: 
```bash
# 查看 MCP 服务器输出
tail -f ~/daxiu_mcp.log  # 如果有日志文件

# 或者前台运行查看输出
python daxiu_mcp_http.py
```

---

## 🔧 高级配置

### 修改端口
编辑 `daxiu_mcp_http.py` 或 `daxiu_mcp_auto.py`：
```python
os.environ["FASTMCP_PORT"] = "3459"  # 改成你想要的端口
```

### 使用固定 cloudflared 隧道
编辑 `start.sh`，将：
```bash
cloudflared tunnel --url http://localhost:3459
```
改为：
```bash
cloudflared tunnel run --token YOUR_TOKEN
```

### 开机自启动
创建 systemd 服务（Linux）或使用 cron（macOS/Linux）：
```bash
# 添加到 crontab
@reboot /path/to/start_http.sh
```

---

## 📞 支持

如果遇到问题：
1. 查看本文档的"常见问题"部分
2. 检查 `MCP_VERSION_GUIDE.md` 了解版本兼容性
3. 运行 `./stop.sh` 清理所有进程后重试
