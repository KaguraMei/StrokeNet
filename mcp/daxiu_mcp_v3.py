"""
StrokeNet MCP Server - FastMCP 3.x 版本
适用于: FastMCP 3.0.0+
安装: pip install fastmcp>=3.0.0

特点: 新的 API 设计，host 和 port 在构造函数中指定
用途: 最新版本，API 更清晰
"""

from mcp.server.fastmcp import FastMCP
import subprocess

# FastMCP 3.x 语法: 在构造函数中指定 host 和 port
mcp = FastMCP("daxiu", host="0.0.0.0", port=3459)

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
    # FastMCP 3.x 语法: run() 只接受 transport 参数
    mcp.run(transport="sse")
