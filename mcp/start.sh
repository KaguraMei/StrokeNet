#!/bin/bash

# StrokeNet MCP Server 启动脚本
# 支持多种传输协议和交互式选择

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "  StrokeNet MCP Server 启动脚本"
echo "=========================================="
echo ""

# 检查是否有参数传入（非交互模式）
if [ $# -gt 0 ]; then
    MODE=$1
else
    # 交互模式：显示菜单
    echo "请选择启动模式:"
    echo ""
    echo "1. HTTP 协议 (推荐 - Claude Desktop 新版本)"
    echo "2. SSE 协议 (旧版本 Claude Desktop)"
    echo "3. stdio 模式 (作为子进程)"
    echo "4. 自动检测 (智能选择)"
    echo ""
    read -p "请输入选项 (1-4) [默认: 1]: " choice
    
    # 默认选择 1
    choice=${choice:-1}
    
    case $choice in
        1) MODE="http" ;;
        2) MODE="sse" ;;
        3) MODE="stdio" ;;
        4) MODE="auto" ;;
        *)
            echo "无效选项，使用默认模式 (HTTP)"
            MODE="http"
            ;;
    esac
fi

echo ""
echo "启动模式: $MODE"
echo ""

# 根据模式选择启动方式
case $MODE in
    http)
        echo "使用 HTTP 协议 (Claude Desktop 新版本)"
        echo "MCP 端点: http://localhost:3459/mcp"
        echo ""
        
        # 启动 MCP 服务器
        python daxiu_mcp_http.py &
        MCP_PID=$!
        
        sleep 2
        
        # 检查服务是否启动成功
        if ! ps -p $MCP_PID > /dev/null 2>&1; then
            echo "错误: MCP 服务器启动失败"
            exit 1
        fi
        
        # 启动 cloudflared
        echo "启动 cloudflared 隧道..."
        echo "请复制下面的 URL 并在 Claude Desktop 中配置"
        echo "URL 格式: https://xxx.trycloudflare.com/mcp"
        echo ""
        cloudflared tunnel --url http://localhost:3459
        ;;
        
    sse)
        echo "使用 SSE 协议 (Claude Desktop 旧版本)"
        echo "SSE 端点: http://localhost:3459/sse"
        echo ""
        
        # 使用自动检测脚本，指定 SSE 模式
        MCP_TRANSPORT=sse python daxiu_mcp_auto.py &
        MCP_PID=$!
        
        sleep 2
        
        # 检查服务是否启动成功
        if ! ps -p $MCP_PID > /dev/null 2>&1; then
            echo "错误: MCP 服务器启动失败"
            exit 1
        fi
        
        # 启动 cloudflared
        echo "启动 cloudflared 隧道..."
        echo "请复制下面的 URL 并在 Claude Desktop 中配置"
        echo "URL 格式: https://xxx.trycloudflare.com/sse"
        echo ""
        cloudflared tunnel --url http://localhost:3459
        ;;
        
    stdio)
        echo "使用 stdio 模式 (标准输入输出)"
        echo "注意: stdio 模式不需要 cloudflared"
        echo ""
        
        # 直接运行，不需要后台
        MCP_TRANSPORT=stdio python daxiu_mcp_auto.py
        ;;
        
    auto)
        echo "使用自动检测模式"
        echo "将根据 FastMCP 版本自动选择最佳协议"
        echo ""
        
        # 使用自动检测脚本（默认 HTTP）
        python daxiu_mcp_auto.py &
        MCP_PID=$!
        
        sleep 2
        
        # 检查服务是否启动成功
        if ! ps -p $MCP_PID > /dev/null 2>&1; then
            echo "错误: MCP 服务器启动失败"
            exit 1
        fi
        
        # 启动 cloudflared
        echo "启动 cloudflared 隧道..."
        echo "请复制下面的 URL 并在 Claude Desktop 中配置"
        echo "URL 格式: https://xxx.trycloudflare.com/mcp"
        echo ""
        cloudflared tunnel --url http://localhost:3459
        ;;
        
    *)
        echo "错误: 未知模式 '$MODE'"
        echo "支持的模式: http, sse, stdio, auto"
        exit 1
        ;;
esac

# 清理函数
cleanup() {
    echo ""
    echo "正在停止服务..."
    if [ ! -z "$MCP_PID" ]; then
        kill $MCP_PID 2>/dev/null
    fi
    pkill -f "daxiu_mcp" 2>/dev/null
    pkill -f "cloudflared" 2>/dev/null
    echo "服务已停止"
    exit 0
}

# 捕获 Ctrl+C
trap cleanup INT TERM

# 等待（如果是后台模式）
if [ "$MODE" != "stdio" ]; then
    wait
fi
