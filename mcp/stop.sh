#!/bin/bash

# StrokeNet MCP Server 停止脚本

echo "=========================================="
echo "  停止 StrokeNet MCP Server"
echo "=========================================="
echo ""

# 检查并停止 MCP 服务器进程
MCP_PIDS=$(pgrep -f "daxiu_mcp")
if [ -n "$MCP_PIDS" ]; then
    echo "正在停止 MCP 服务器 (PID: $MCP_PIDS)..."
    kill $MCP_PIDS 2>/dev/null
    sleep 1
    # 如果还在运行，强制杀死
    if pgrep -f "daxiu_mcp" > /dev/null; then
        echo "强制停止 MCP 服务器..."
        kill -9 $(pgrep -f "daxiu_mcp") 2>/dev/null
    fi
    echo "✓ MCP 服务器已停止"
else
    echo "✗ 未找到运行中的 MCP 服务器"
fi

# 检查并停止 cloudflared 进程
CF_PIDS=$(pgrep -f "cloudflared")
if [ -n "$CF_PIDS" ]; then
    echo "正在停止 cloudflared (PID: $CF_PIDS)..."
    kill $CF_PIDS 2>/dev/null
    sleep 1
    # 如果还在运行，强制杀死
    if pgrep -f "cloudflared" > /dev/null; then
        echo "强制停止 cloudflared..."
        kill -9 $(pgrep -f "cloudflared") 2>/dev/null
    fi
    echo "✓ cloudflared 已停止"
else
    echo "✗ 未找到运行中的 cloudflared"
fi

echo ""
echo "所有服务已停止"
echo ""

# 显示当前状态
echo "验证进程状态:"
if pgrep -f "daxiu_mcp" > /dev/null; then
    echo "⚠ MCP 服务器仍在运行"
else
    echo "✓ MCP 服务器已完全停止"
fi

if pgrep -f "cloudflared" > /dev/null; then
    echo "⚠ cloudflared 仍在运行"
else
    echo "✓ cloudflared 已完全停止"
fi
