"""
StrokeNet MCP Server - HTTP 协议版本（Claude 新协议）
适用于: FastMCP 2.14+ 配合 Claude Desktop 新版本
安装: pip install fastmcp

特点: 使用 HTTP transport（非 SSE），配合 cloudflared
用途: Claude Desktop 新协议要求，URL 格式: https://xxx.trycloudflare.com/mcp
"""

from fastmcp import FastMCP
import subprocess
import os

mcp = FastMCP("daxiu")

def am(action, **kwargs):
    """通过 Android Intent 发送控制指令"""
    cmd = ["am", "start", "-n",
           "aya.strokenet/.MainActivity",
           "--es", "action", action]
    for k, v in kwargs.items():
        cmd += ["--ei", k, str(v)]
    subprocess.run(cmd, capture_output=True, text=True, timeout=5)

@mcp.tool()
def daxiu_start(depth: int = 36,
                extend_speed: int = 8,
                retract_speed: int = 8) -> str:
    """启动推拉。depth 0-72, extend_speed 0-15, retract_speed 0-15"""
    am("start", depth=depth, extend=extend_speed, retract=retract_speed)
    return f"推拉启动: 深浅={depth}/72 伸出={extend_speed}/15 缩回={retract_speed}/15"

@mcp.tool()
def daxiu_thrust(depth: int = 36,
                 extend_speed: int = 8,
                 retract_speed: int = 8) -> str:
    """运行中调节推拉参数"""
    am("thrust", depth=depth, extend=extend_speed, retract=retract_speed)
    return f"推拉调节: 深浅={depth} 伸出={extend_speed} 缩回={retract_speed}"

@mcp.tool()
def daxiu_strength(value: int = 50) -> str:
    """设置入体端强度 0-100"""
    am("strength", value=value)
    return f"入体端强度: {value}/100"

@mcp.tool()
def daxiu_temp(value: int = 30) -> str:
    """设置加热温度 0-60"""
    am("temp", value=value)
    return f"温度: {value}/60"

@mcp.tool()
def daxiu_stop() -> str:
    """全部停止"""
    am("stop")
    return "已停止"

if __name__ == "__main__":
    print("=" * 60)
    print("StrokeNet MCP Server - HTTP 协议")
    print("=" * 60)
    print("协议: HTTP (Claude Desktop 新版本)")
    print("监听: 0.0.0.0:3459")
    print("MCP 端点: http://localhost:3459/mcp")
    print("Cloudflared URL: https://xxx.trycloudflare.com/mcp")
    print("=" * 60)
    print("按 Ctrl+C 停止服务")
    print("=" * 60)
    
    # 使用 HTTP transport，显式指定 host 和 port
    # FastMCP 默认端口是 8000，必须显式指定才能使用 3459
    mcp.run(transport="http", host="0.0.0.0", port=3459)
