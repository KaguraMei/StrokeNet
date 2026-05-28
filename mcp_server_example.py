#!/usr/bin/env python3
"""
StrokeNet MCP Server
在 Termux 中运行此服务，将设备控制封装为 AI 可调用的工具
"""

from mcp.server.fastmcp import FastMCP
import subprocess
import logging

# 配置日志
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

mcp = FastMCP("strokenet")

# 包名和 Activity
PACKAGE = "com.ec.strokenet"
ACTIVITY = ".MainActivity"


def am(action: str, **kwargs) -> dict:
    """
    发送 Intent 到 StrokeNet APP
    
    Args:
        action: 操作类型 (start/thrust/strength/temp/stop)
        **kwargs: 参数 (depth, extend, retract, value)
    
    Returns:
        dict: 执行结果
    """
    cmd = [
        "am", "start", "-n",
        f"{PACKAGE}/{ACTIVITY}",
        "--es", "action", action
    ]
    
    # 添加参数
    for key, value in kwargs.items():
        cmd += ["--ei", key, str(value)]
    
    try:
        result = subprocess.run(
            cmd,
            capture_output=True,
            text=True,
            timeout=5
        )
        
        if result.returncode == 0:
            logger.info(f"Command sent: {action} {kwargs}")
            return {
                "success": True,
                "action": action,
                "params": kwargs,
                "output": result.stdout
            }
        else:
            logger.error(f"Command failed: {result.stderr}")
            return {
                "success": False,
                "error": result.stderr
            }
    except subprocess.TimeoutExpired:
        logger.error("Command timeout")
        return {
            "success": False,
            "error": "Command timeout"
        }
    except Exception as e:
        logger.error(f"Exception: {str(e)}")
        return {
            "success": False,
            "error": str(e)
        }


@mcp.tool()
def strokenet_start(
    depth: int = 36,
    extend_speed: int = 8,
    retract_speed: int = 8
) -> str:
    """
    启动推拉功能
    
    Args:
        depth: 推拉深度 (0-72)，控制行程深浅
        extend_speed: 伸出速度 (0-15)，往外推的速度
        retract_speed: 缩回速度 (0-15)，往回拉的速度
    
    Returns:
        str: 执行结果描述
    
    Note:
        三个参数必须同时提供，设备才会执行动作
    """
    # 参数验证
    if not (0 <= depth <= 72):
        return f"错误: depth 必须在 0-72 范围内，当前值: {depth}"
    if not (0 <= extend_speed <= 15):
        return f"错误: extend_speed 必须在 0-15 范围内，当前值: {extend_speed}"
    if not (0 <= retract_speed <= 15):
        return f"错误: retract_speed 必须在 0-15 范围内，当前值: {retract_speed}"
    
    result = am(
        "start",
        depth=depth,
        extend=extend_speed,
        retract=retract_speed
    )
    
    if result["success"]:
        return f"✓ 推拉已启动: 深度={depth}/72, 伸出速度={extend_speed}/15, 缩回速度={retract_speed}/15"
    else:
        return f"✗ 启动失败: {result.get('error', 'Unknown error')}"


@mcp.tool()
def strokenet_thrust(
    depth: int = 36,
    extend_speed: int = 8,
    retract_speed: int = 8
) -> str:
    """
    运行中调节推拉参数
    
    Args:
        depth: 推拉深度 (0-72)
        extend_speed: 伸出速度 (0-15)
        retract_speed: 缩回速度 (0-15)
    
    Returns:
        str: 执行结果描述
    """
    # 参数验证
    if not (0 <= depth <= 72):
        return f"错误: depth 必须在 0-72 范围内"
    if not (0 <= extend_speed <= 15):
        return f"错误: extend_speed 必须在 0-15 范围内"
    if not (0 <= retract_speed <= 15):
        return f"错误: retract_speed 必须在 0-15 范围内"
    
    result = am(
        "thrust",
        depth=depth,
        extend=extend_speed,
        retract=retract_speed
    )
    
    if result["success"]:
        return f"✓ 推拉参数已调节: 深度={depth}/72, 伸出={extend_speed}/15, 缩回={retract_speed}/15"
    else:
        return f"✗ 调节失败: {result.get('error', 'Unknown error')}"


@mcp.tool()
def strokenet_strength(value: int = 50) -> str:
    """
    设置入体端强度
    
    Args:
        value: 强度值 (0-100)，控制震动强度
    
    Returns:
        str: 执行结果描述
    """
    if not (0 <= value <= 100):
        return f"错误: value 必须在 0-100 范围内，当前值: {value}"
    
    result = am("strength", value=value)
    
    if result["success"]:
        return f"✓ 入体端强度已设置: {value}/100"
    else:
        return f"✗ 设置失败: {result.get('error', 'Unknown error')}"


@mcp.tool()
def strokenet_temp(value: int = 30) -> str:
    """
    设置加热温度
    
    Args:
        value: 温度值 (0-60)，单位：摄氏度
    
    Returns:
        str: 执行结果描述
    """
    if not (0 <= value <= 60):
        return f"错误: value 必须在 0-60 范围内，当前值: {value}"
    
    result = am("temp", value=value)
    
    if result["success"]:
        return f"✓ 加热温度已设置: {value}°C"
    else:
        return f"✗ 设置失败: {result.get('error', 'Unknown error')}"


@mcp.tool()
def strokenet_stop() -> str:
    """
    停止所有功能
    
    Returns:
        str: 执行结果描述
    """
    result = am("stop")
    
    if result["success"]:
        return "✓ 所有功能已停止"
    else:
        return f"✗ 停止失败: {result.get('error', 'Unknown error')}"


@mcp.tool()
def strokenet_status() -> str:
    """
    获取 APP 状态信息
    
    Returns:
        str: APP 状态描述
    """
    try:
        # 检查 APP 是否安装
        result = subprocess.run(
            ["pm", "list", "packages", PACKAGE],
            capture_output=True,
            text=True,
            timeout=5
        )
        
        if PACKAGE in result.stdout:
            return f"✓ StrokeNet APP 已安装 ({PACKAGE})"
        else:
            return f"✗ StrokeNet APP 未安装"
    except Exception as e:
        return f"✗ 无法检查状态: {str(e)}"


if __name__ == "__main__":
    logger.info("Starting StrokeNet MCP Server...")
    logger.info(f"Package: {PACKAGE}")
    logger.info(f"Activity: {ACTIVITY}")
    
    # 启动服务
    # SSE 模式，监听所有接口，端口 3459
    mcp.run(transport="sse", host="0.0.0.0", port=3459)
