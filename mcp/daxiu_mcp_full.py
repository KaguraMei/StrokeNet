"""
StrokeNet MCP Server - 完整功能版
包含基础控制 + 自定义循环预设管理

特点:
- 基础控制: 推拉、强度、温度、停止
- 预设管理: 创建、删除、导入、导出自定义循环预设
- 预设执行: 运行官方或自定义循环预设

安装: pip install fastmcp
用法: python daxiu_mcp_full.py
"""

from fastmcp import FastMCP
import subprocess
import json
import os
import uuid
from typing import List, Dict, Optional

mcp = FastMCP("daxiu")

# ============= 基础控制函数 =============

def am(action: str, **kwargs):
    """通过 Android Intent 发送控制指令"""
    cmd = ["am", "start", "-n",
           "aya.strokenet/.MainActivity",
           "--es", "action", action]
    for k, v in kwargs.items():
        if isinstance(v, bool):
            cmd += ["--ez", k, str(v).lower()]
        elif isinstance(v, int):
            cmd += ["--ei", k, str(v)]
        else:
            cmd += ["--es", k, str(v)]
    
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=5)
    return result.returncode == 0

# ============= 基础控制工具 =============

@mcp.tool()
def daxiu_start(depth: int = 36,
                extend_speed: int = 8,
                retract_speed: int = 8) -> str:
    """
    启动推拉运动
    
    参数:
    - depth: 深度 0-72 (默认36，约50%)
    - extend_speed: 伸出速度 0-15 (默认8，约50%)
    - retract_speed: 缩回速度 0-15 (默认8，约50%)
    """
    am("start", depth=depth, extend=extend_speed, retract=retract_speed)
    return f"✓ 推拉启动: 深度={depth}/72, 伸速={extend_speed}/15, 缩速={retract_speed}/15"

@mcp.tool()
def daxiu_thrust(depth: int = 36,
                 extend_speed: int = 8,
                 retract_speed: int = 8) -> str:
    """
    运行中调节推拉参数（不会停止）
    
    参数:
    - depth: 深度 0-72
    - extend_speed: 伸出速度 0-15
    - retract_speed: 缩回速度 0-15
    """
    am("thrust", depth=depth, extend=extend_speed, retract=retract_speed)
    return f"✓ 推拉调节: 深度={depth}, 伸速={extend_speed}, 缩速={retract_speed}"

@mcp.tool()
def daxiu_strength(value: int = 50) -> str:
    """
    设置入体端震动强度
    
    参数:
    - value: 强度 0-100 (默认50)
    """
    am("strength", value=value)
    return f"✓ 入体端强度: {value}/100"

@mcp.tool()
def daxiu_temp(value: int = 30) -> str:
    """
    设置加热温度
    
    参数:
    - value: 温度 0-60°C (默认30)
    """
    am("temp", value=value)
    return f"✓ 加热温度: {value}°C"

@mcp.tool()
def daxiu_stop() -> str:
    """停止所有运动"""
    am("stop")
    return "✓ 已停止所有运动"

# ============= 预设管理工具 =============

@mcp.tool()
def preset_list() -> str:
    """
    列出所有可用预设（官方+自定义）
    
    返回预设列表，包含ID、名称、描述等信息
    """
    try:
        am("get_presets")
        # 等待广播结果（简化版，实际应该监听广播）
        import time
        time.sleep(0.5)
        
        # 这里需要读取结果，实际实现中应该使用广播接收器
        # 暂时返回提示信息
        return """✓ 预设列表请求已发送

官方预设示例:
- 14459: 九浅一深
- 14461: 暗涌
- 14463: 旧梦
- 14465: 狂澜

自定义预设可以通过 preset_get_custom() 查看"""
        
    except Exception as e:
        return f"✗ 获取预设列表失败: {e}"

@mcp.tool()
def preset_create(
    name: str,
    description: str,
    actions: List[Dict[str, int]]
) -> str:
    """
    创建自定义循环预设
    
    参数:
    - name: 预设名称
    - description: 预设描述
    - actions: 动作列表，每个动作包含以下字段:
      {
        "depth": 深度 1-100,
        "extend_speed": 伸出速度 1-100,
        "retract_speed": 缩回速度 1-100,
        "strength": 震动强度 1-100,
        "duration": 持续时间（毫秒）
      }
    
    示例:
    preset_create(
        name="温柔模式",
        description="轻柔舒适的节奏",
        actions=[
            {"depth": 30, "extend_speed": 40, "retract_speed": 40, "strength": 30, "duration": 2000},
            {"depth": 50, "extend_speed": 50, "retract_speed": 50, "strength": 40, "duration": 2000}
        ]
    )
    """
    try:
        # 构建命令列表
        commands = []
        for action in actions:
            depth = action.get("depth", 50)
            extend = action.get("extend_speed", 50)
            retract = action.get("retract_speed", 50)
            strength = action.get("strength", 50)
            duration = action.get("duration", 2000)
            
            # 构建命令字符串（格式: 710003**-8800-####-0000-DDEERRSS0000）
            depth_hex = f"{depth:02X}"
            extend_hex = f"{extend:02X}"
            retract_hex = f"{retract:02X}"
            strength_hex = f"{strength:02X}"
            
            command = f"710003**-8800-####-0000-{depth_hex}{extend_hex}{retract_hex}{strength_hex}0000"
            commands.append({"command": command, "time": duration})
        
        # 构建预设JSON
        preset_id = str(uuid.uuid4())
        preset = {
            "id": preset_id,
            "name": name,
            "description": description,
            "commands": commands,
            "isCustom": True
        }
        
        preset_json = json.dumps(preset)
        am("create_preset", json=preset_json)
        
        return f"✓ 预设已创建: {name}\nID: {preset_id}\n动作数: {len(commands)}"
        
    except Exception as e:
        return f"✗ 创建预设失败: {e}"

@mcp.tool()
def preset_run(preset_id: str) -> str:
    """
    运行指定的循环预设
    
    参数:
    - preset_id: 预设ID（从 preset_list 获取）
    
    预设会循环播放，直到调用 daxiu_stop() 停止
    """
    try:
        am("run_preset", preset_id=preset_id)
        return f"✓ 正在运行预设: {preset_id}\n提示: 使用 daxiu_stop() 停止"
    except Exception as e:
        return f"✗ 运行预设失败: {e}"

@mcp.tool()
def preset_delete(preset_id: str) -> str:
    """
    删除自定义预设（官方预设无法删除）
    
    参数:
    - preset_id: 预设ID
    """
    try:
        am("delete_preset", preset_id=preset_id)
        return f"✓ 预设已删除: {preset_id}"
    except Exception as e:
        return f"✗ 删除预设失败: {e}"

@mcp.tool()
def preset_export() -> str:
    """
    导出所有自定义预设为JSON格式
    
    返回JSON字符串，可用于分享或备份
    """
    try:
        am("export_presets")
        return """✓ 导出请求已发送

导出的JSON会保存到剪贴板或文件。
格式示例:
[
  {
    "id": "xxx",
    "name": "我的预设",
    "description": "自定义节奏",
    "commands": [...],
    "isCustom": true
  }
]"""
    except Exception as e:
        return f"✗ 导出预设失败: {e}"

@mcp.tool()
def preset_import(json_data: str, replace: bool = False) -> str:
    """
    从JSON导入自定义预设
    
    参数:
    - json_data: JSON字符串（从 preset_export 导出的格式）
    - replace: 是否替换现有预设（默认false，合并模式）
    """
    try:
        am("import_presets", json=json_data, replace=replace)
        mode = "替换" if replace else "合并"
        return f"✓ 预设导入请求已发送（{mode}模式）"
    except Exception as e:
        return f"✗ 导入预设失败: {e}"

# ============= 快速创建预设模板 =============

@mcp.tool()
def preset_quick_gentle() -> str:
    """快速创建「轻柔模式」预设"""
    return preset_create(
        name="轻柔模式",
        description="温和舒适，适合初次体验",
        actions=[
            {"depth": 25, "extend_speed": 30, "retract_speed": 30, "strength": 25, "duration": 3000},
            {"depth": 35, "extend_speed": 35, "retract_speed": 35, "strength": 30, "duration": 3000},
            {"depth": 30, "extend_speed": 32, "retract_speed": 32, "strength": 27, "duration": 2500}
        ]
    )

@mcp.tool()
def preset_quick_standard() -> str:
    """快速创建「标准模式」预设"""
    return preset_create(
        name="标准模式",
        description="平衡节奏，适合日常使用",
        actions=[
            {"depth": 45, "extend_speed": 50, "retract_speed": 50, "strength": 45, "duration": 2500},
            {"depth": 55, "extend_speed": 55, "retract_speed": 55, "strength": 50, "duration": 2500},
            {"depth": 50, "extend_speed": 52, "retract_speed": 52, "strength": 47, "duration": 2000}
        ]
    )

@mcp.tool()
def preset_quick_intense() -> str:
    """快速创建「强力模式」预设"""
    return preset_create(
        name="强力模式",
        description="强劲刺激，适合进阶体验",
        actions=[
            {"depth": 70, "extend_speed": 70, "retract_speed": 70, "strength": 65, "duration": 2000},
            {"depth": 80, "extend_speed": 75, "retract_speed": 75, "strength": 70, "duration": 2000},
            {"depth": 75, "extend_speed": 72, "retract_speed": 72, "strength": 67, "duration": 1500}
        ]
    )

@mcp.tool()
def preset_quick_wave() -> str:
    """快速创建「波浪模式」预设 - 渐强渐弱的节奏"""
    return preset_create(
        name="波浪模式",
        description="如波浪般起伏的渐变节奏",
        actions=[
            {"depth": 30, "extend_speed": 40, "retract_speed": 40, "strength": 30, "duration": 2000},
            {"depth": 50, "extend_speed": 55, "retract_speed": 55, "strength": 45, "duration": 2000},
            {"depth": 70, "extend_speed": 70, "retract_speed": 70, "strength": 60, "duration": 2000},
            {"depth": 50, "extend_speed": 55, "retract_speed": 55, "strength": 45, "duration": 2000},
            {"depth": 30, "extend_speed": 40, "retract_speed": 40, "strength": 30, "duration": 2000}
        ]
    )

# ============= 启动服务器 =============

if __name__ == "__main__":
    # 从命令行参数或环境变量获取传输协议
    TRANSPORT = os.environ.get("MCP_TRANSPORT", "http")
    HOST = os.environ.get("MCP_HOST", "0.0.0.0")
    PORT = int(os.environ.get("MCP_PORT", "3459"))
    
    print("=" * 70)
    print("StrokeNet MCP Server - 完整功能版")
    print("=" * 70)
    print("✓ 基础控制: 推拉、强度、温度、停止")
    print("✓ 预设管理: 创建、删除、导入、导出")
    print("✓ 快速预设: 轻柔、标准、强力、波浪模式")
    print("=" * 70)
    print(f"传输协议: {TRANSPORT.upper()}")
    print(f"监听地址: {HOST}:{PORT}")
    
    if TRANSPORT == "http":
        print(f"MCP 端点: http://localhost:{PORT}/mcp")
    
    print("=" * 70)
    print("按 Ctrl+C 停止服务")
    print("=" * 70)
    
    try:
        if TRANSPORT == "stdio":
            mcp.run()
        else:
            mcp.run(transport=TRANSPORT, host=HOST, port=PORT)
    except KeyboardInterrupt:
        print("\n✓ 服务已停止")
    except Exception as e:
        print(f"✗ 错误: {e}")
        import traceback
        traceback.print_exc()
