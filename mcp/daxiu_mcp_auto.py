"""
StrokeNet MCP Server - 自动检测版本（支持所有协议）
适用于: 任何 FastMCP 版本
安装: pip install fastmcp

特点: 自动检测 FastMCP 版本并支持多种传输协议
用途: 懒人专用，支持 HTTP/SSE/stdio 三种模式
"""

from fastmcp import FastMCP
import subprocess
import sys
import os

# 从命令行参数或环境变量获取传输协议
TRANSPORT = os.environ.get("MCP_TRANSPORT", "http")  # 默认使用 HTTP（新协议）
HOST = os.environ.get("MCP_HOST", "0.0.0.0")
PORT = int(os.environ.get("MCP_PORT", "3459"))

# 支持命令行参数覆盖
if len(sys.argv) > 1:
    TRANSPORT = sys.argv[1].lower()
if len(sys.argv) > 2:
    PORT = int(sys.argv[2])

# 检测 FastMCP 版本
try:
    import fastmcp
    version = fastmcp.__version__
    major_version = int(version.split('.')[0])
    print(f"检测到 FastMCP 版本: {version}")
except:
    print("警告: 无法检测 FastMCP 版本，假设为 2.x")
    major_version = 2

# 根据版本创建 MCP 实例
if major_version >= 3:
    print("使用 FastMCP 3.x API")
    mcp = FastMCP("daxiu")
else:
    print("使用 FastMCP 1.x/2.x API")
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
    print("StrokeNet MCP Server (自动适配版本)")
    print("=" * 60)
    print(f"传输协议: {TRANSPORT.upper()}")
    print(f"监听地址: {HOST}:{PORT}")
    
    if TRANSPORT == "http":
        print(f"MCP 端点: http://localhost:{PORT}/mcp")
        print("适用于: Claude Desktop 新版本")
    elif TRANSPORT == "sse":
        print(f"SSE 端点: http://localhost:{PORT}/sse")
        print("适用于: Claude Desktop 旧版本")
    else:
        print("模式: stdio (标准输入输出)")
        print("适用于: 作为子进程调用")
    
    print("=" * 60)
    print("按 Ctrl+C 停止服务")
    print("=" * 60)
    
    # 设置环境变量
    os.environ["FASTMCP_HOST"] = HOST
    os.environ["FASTMCP_PORT"] = str(PORT)
    
    # 根据传输协议和版本选择启动方式
    try:
        if TRANSPORT == "stdio":
            # stdio 模式
            mcp.run()
            
        elif TRANSPORT in ["http", "sse"]:
            # HTTP/SSE 模式
            if major_version >= 3:
                # FastMCP 3.x 必须显式传参
                mcp.run(transport=TRANSPORT, host=HOST, port=PORT)
                
            elif major_version == 1:
                # FastMCP 1.x
                try:
                    mcp.run(transport=TRANSPORT, host=HOST, port=PORT)
                except TypeError:
                    print("FastMCP 1.x API 调用失败，尝试使用 uvicorn...")
                    import uvicorn
                    uvicorn.run(mcp.get_asgi_app(), host=HOST, port=PORT)
                    
            else:
                # FastMCP 2.x
                try:
                    mcp.run(transport=TRANSPORT)
                except Exception as e:
                    print(f"mcp.run() 失败: {e}")
                    print("尝试使用 uvicorn...")
                    import uvicorn
                    uvicorn.run(mcp.get_asgi_app(), host=HOST, port=PORT, log_level="info")
        else:
            print(f"错误: 不支持的传输协议 '{TRANSPORT}'")
            print("支持的协议: http, sse, stdio")
            sys.exit(1)
            
    except KeyboardInterrupt:
        print("\n服务已停止")
    except Exception as e:
        print(f"错误: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
